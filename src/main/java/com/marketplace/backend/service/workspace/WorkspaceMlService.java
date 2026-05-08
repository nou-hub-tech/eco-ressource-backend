package com.marketplace.backend.service.workspace;

import com.marketplace.backend.entity.workspace.WorkspaceEnums.OrderStatus;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.PaymentStatus;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.ReservationRole;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.ReservationStatus;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.SlotPortfolio;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.SlotStatus;
import com.marketplace.backend.entity.workspace.WorkspaceOrder;
import com.marketplace.backend.entity.workspace.WorkspacePayloads;
import com.marketplace.backend.entity.workspace.WorkspaceReservation;
import com.marketplace.backend.entity.workspace.WorkspaceReservationSlot;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceMlService {

  public WorkspaceMlAnalysis analyze(
      List<WorkspaceReservationSlot> slots,
      List<WorkspaceReservation> reservations,
      List<WorkspaceOrder> orders) {
    Map<String, SlotSnapshot> slotSnapshots = buildSlotSnapshots(slots);
    Map<String, ReservationMlProfile> reservationProfiles =
        buildReservationProfiles(reservations, slotSnapshots);
    Map<String, OrderMlProfile> orderProfiles =
        buildOrderProfiles(orders, reservations, reservationProfiles);

    Map<String, SlotMlProfile> slotProfiles =
        slotSnapshots.values().stream()
            .collect(
                Collectors.toMap(
                    SlotSnapshot::id,
                    snapshot ->
                        new SlotMlProfile(
                            snapshot.predictedAvailability(),
                            snapshot.underusedScore(),
                            snapshot.demandTrendLabel(),
                            snapshot.segmentLabel()),
                    (left, right) -> left,
                    LinkedHashMap::new));

    return new WorkspaceMlAnalysis(slotProfiles, reservationProfiles, orderProfiles);
  }

  private Map<String, SlotSnapshot> buildSlotSnapshots(List<WorkspaceReservationSlot> slots) {
    Map<String, BaseSlotVector> baseVectors = new LinkedHashMap<>();
    for (WorkspaceReservationSlot slot : slots) {
      int capacity = Math.max(defaultInteger(slot.getCapacity(), 1), 1);
      int occupied = clamp(defaultInteger(slot.getOccupied(), 0), 0, capacity);
      double utilizationRate = occupied * 100.0 / capacity;
      List<Integer> forecastSeries = resolveForecastSeries(slot, utilizationRate);

      TimeSeriesProjection projection = fitAvailabilityProjection(utilizationRate, forecastSeries);
      BaseSlotVector baseVector =
          new BaseSlotVector(
              slot.getId(),
              utilizationRate,
              projection.predictedAvailability(),
              projection.trendSlope(),
              forecastSeries.stream().mapToInt(Integer::intValue).average().orElse(utilizationRate),
              safeSize(slot.getEquipment()),
              defaultSlotStatus(slot.getStatus()),
              defaultSlotPortfolio(slot.getPortfolio()),
              capacity);
      baseVectors.put(slot.getId(), baseVector);
    }
    List<BaseSlotVector> orderedVectors = new ArrayList<>(baseVectors.values());

    List<double[]> clusteringVectors =
        orderedVectors.stream()
            .map(
                vector ->
                    new double[] {
                      vector.utilizationRate() / 100.0,
                      vector.predictedAvailability() / 100.0,
                      vector.forecastAverage() / 100.0,
                      (vector.trendSlope() + 10.0) / 20.0,
                      vector.equipmentCount() / 10.0,
                      vector.portfolio() == SlotPortfolio.partner ? 1.0 : 0.0
                    })
            .toList();

    KMeansResult slotClusters = runKMeans(clusteringVectors, Math.min(3, Math.max(clusteringVectors.size(), 1)));

    Map<Integer, Double> clusterDemand = new LinkedHashMap<>();
    Map<Integer, Double> clusterAvailability = new LinkedHashMap<>();
    Map<Integer, Integer> clusterCounts = new LinkedHashMap<>();
    for (int index = 0; index < clusteringVectors.size(); index += 1) {
      int cluster = slotClusters.assignments()[index];
      BaseSlotVector vector = orderedVectors.get(index);
      clusterDemand.merge(
          cluster, vector.utilizationRate() * 0.65 + vector.forecastAverage() * 0.35, Double::sum);
      clusterAvailability.merge(cluster, vector.predictedAvailability(), Double::sum);
      clusterCounts.merge(cluster, 1, Integer::sum);
    }
    for (Integer cluster : new ArrayList<>(clusterDemand.keySet())) {
      int count = Math.max(clusterCounts.getOrDefault(cluster, 1), 1);
      clusterDemand.put(cluster, clusterDemand.get(cluster) / count);
      clusterAvailability.put(cluster, clusterAvailability.getOrDefault(cluster, 0.0) / count);
    }

    Map<Integer, String> clusterLabels = rankSlotClusters(slotClusters, clusterDemand, clusterAvailability);
    int underusedCluster =
        clusterDemand.entrySet().stream()
            .min(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(0);
    int pressureCluster =
        clusterDemand.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(underusedCluster);

    Map<String, SlotSnapshot> snapshots = new LinkedHashMap<>();
    int index = 0;
    for (BaseSlotVector vector : baseVectors.values()) {
      int cluster = slotClusters.assignments()[Math.min(index, slotClusters.assignments().length - 1)];
      int clusterBonus = cluster == underusedCluster ? 22 : cluster == pressureCluster ? -18 : 4;
      int statusPenalty =
          switch (vector.status()) {
            case maintenance -> 8;
            case peak -> 12;
            default -> 0;
          };
      int underusedScore =
          clamp(
              (int)
                  Math.round(
                      (100.0 - vector.utilizationRate()) * 0.54
                          + vector.predictedAvailability() * 0.28
                          + Math.max(0.0, -vector.trendSlope()) * 4.0
                          + clusterBonus
                          - statusPenalty),
              6,
              96);

      snapshots.put(
          vector.id(),
          new SlotSnapshot(
              vector.id(),
              clamp((int) Math.round(vector.predictedAvailability()), 6, 96),
              underusedScore,
              describeDemandTrend(vector.trendSlope()),
              clusterLabels.getOrDefault(cluster, "Balanced node"),
              vector.utilizationRate(),
              vector.predictedAvailability() / 100.0,
              vector.status() == SlotStatus.maintenance,
              vector.capacity()));
      index += 1;
    }
    return snapshots;
  }

  private Map<String, ReservationMlProfile> buildReservationProfiles(
      List<WorkspaceReservation> reservations, Map<String, SlotSnapshot> slotSnapshots) {
    List<FeatureSample> training = new ArrayList<>();
    for (WorkspaceReservation reservation : reservations) {
      ReservationStatus status = defaultReservationStatus(reservation.getStatus());
      if (status == ReservationStatus.pending) {
        continue;
      }
      training.add(
          new FeatureSample(
              reservationFeatures(reservation, slotSnapshots),
              status == ReservationStatus.risk || status == ReservationStatus.cancelled ? 1 : 0));
    }

    ensureReservationTrainingCoverage(training);

    ExplainedLogisticModel reservationModel =
        trainExplainedLogisticModel(
            training,
            List.of(
                "high site utilization",
                "heavy slot pressure",
                "tight lead time",
                "long service window",
                "dense attendance plan",
                "light booking notes",
                "missing confirmation route",
                "maintenance exposure",
                "consumer-side dependency",
                "high booking amount"));

    Map<String, ReservationMlProfile> profiles = new LinkedHashMap<>();
    for (WorkspaceReservation reservation : reservations) {
      double[] features = reservationFeatures(reservation, slotSnapshots);
      double probability = reservationModel.predictProbability(features);
      ReservationStatus status = defaultReservationStatus(reservation.getStatus());

      if (status == ReservationStatus.cancelled) {
        probability = Math.max(probability, 0.92);
      } else if (status == ReservationStatus.risk) {
        probability = Math.max(probability, 0.76);
      }

      int cancellationRisk = clamp((int) Math.round(probability * 100.0), 9, 97);
      int readinessScore =
          clamp(
              (int)
                  Math.round(
                      100.0
                          - cancellationRisk * 0.58
                          + (hasContactDestination(reservation) ? 10 : -6)
                          + (isBlank(reservation.getNotes()) ? 0 : 8)
                          + (status == ReservationStatus.confirmed ? 8 : 0)
                          + (Duration.between(LocalDateTime.now(), reservation.getStartAt()).toHours() > 72
                              ? 5
                              : -3)),
              28,
              98);

      profiles.put(
          reservation.getId(),
          new ReservationMlProfile(
              cancellationRisk,
              readinessScore,
              probability,
              reservationModel.topDrivers(features, 2)));
    }
    return profiles;
  }

  private Map<String, OrderMlProfile> buildOrderProfiles(
      List<WorkspaceOrder> orders,
      List<WorkspaceReservation> reservations,
      Map<String, ReservationMlProfile> reservationProfiles) {
    Map<String, WorkspaceReservation> reservationById =
        reservations.stream()
            .collect(
                Collectors.toMap(
                    WorkspaceReservation::getId,
                    reservation -> reservation,
                    (left, right) -> left,
                    LinkedHashMap::new));

    List<FeatureSample> training = new ArrayList<>();
    for (WorkspaceOrder order : orders) {
      training.add(
          new FeatureSample(
              orderFeatures(order, reservationById, reservationProfiles),
              isFlaggedOrder(order) ? 1 : 0));
    }
    ensureOrderTrainingCoverage(training);

    ExplainedLogisticModel orderModel =
        trainExplainedLogisticModel(
            training,
            List.of(
                "large settlement amount",
                "tax-heavy file",
                "short due horizon",
                "multi-line order",
                "dense value per line",
                "linked booking instability",
                "consumer-side exposure",
                "pending payment state",
                "compressed order cycle"));

    List<double[]> spendingVectors = new ArrayList<>();
    for (WorkspaceOrder order : orders) {
      spendingVectors.add(spendingClusterVector(order, reservationById, reservationProfiles));
    }
    KMeansResult spendingClusters = runKMeans(spendingVectors, Math.min(3, Math.max(spendingVectors.size(), 1)));
    Map<Integer, String> clusterLabels = rankOrderClusters(spendingClusters, orders, reservationById, reservationProfiles);

    Map<String, OrderMlProfile> profiles = new LinkedHashMap<>();
    for (int index = 0; index < orders.size(); index += 1) {
      WorkspaceOrder order = orders.get(index);
      double[] features = orderFeatures(order, reservationById, reservationProfiles);
      double probability = orderModel.predictProbability(features);
      if (isFlaggedOrder(order)) {
        probability = Math.max(probability, 0.73);
      }

      int fraudRisk = clamp((int) Math.round(probability * 100.0), 8, 97);
      int cluster = spendingClusters.assignments()[Math.min(index, spendingClusters.assignments().length - 1)];
      profiles.put(
          order.getId(),
          new OrderMlProfile(
              fraudRisk,
              clusterLabels.getOrDefault(cluster, "Balanced commercial flow"),
              probability,
              orderModel.topDrivers(features, 2)));
    }
    return profiles;
  }

  private List<Integer> resolveForecastSeries(WorkspaceReservationSlot slot, double utilizationRate) {
    if (slot.getForecast() != null && !slot.getForecast().isEmpty()) {
      return slot.getForecast().stream().map(value -> clamp(defaultInteger(value, 0), 5, 95)).toList();
    }

    int base =
        switch (defaultSlotStatus(slot.getStatus())) {
          case peak -> 82;
          case maintenance -> 86;
          case balanced -> 54;
          case available -> 34;
        };
    List<Integer> fallback = new ArrayList<>();
    for (int index = 0; index < 7; index += 1) {
      int drift = index * (defaultSlotStatus(slot.getStatus()) == SlotStatus.available ? -2 : 3);
      fallback.add(clamp((int) Math.round((base + utilizationRate) / 2.0) + drift, 8, 94));
    }
    return fallback;
  }

  private TimeSeriesProjection fitAvailabilityProjection(
      double currentUtilization, List<Integer> forecastSeries) {
    List<Double> occupancySeries = new ArrayList<>();
    occupancySeries.add(currentUtilization);
    occupancySeries.addAll(forecastSeries.stream().map(Integer::doubleValue).toList());

    int n = occupancySeries.size();
    if (n < 2) {
      double availability = 100.0 - currentUtilization;
      return new TimeSeriesProjection(clamp(availability, 6.0, 96.0), 0.0);
    }

    double sumX = 0.0;
    double sumY = 0.0;
    double sumXY = 0.0;
    double sumXX = 0.0;
    for (int index = 0; index < n; index += 1) {
      double x = index;
      double y = occupancySeries.get(index);
      sumX += x;
      sumY += y;
      sumXY += x * y;
      sumXX += x * x;
    }

    double denominator = n * sumXX - sumX * sumX;
    double slope = Math.abs(denominator) < 1e-9 ? 0.0 : (n * sumXY - sumX * sumY) / denominator;
    double intercept = (sumY - slope * sumX) / n;
    double nextOne = intercept + slope * n;
    double nextTwo = intercept + slope * (n + 1.0);
    double projectedOccupancy = clamp((nextOne + nextTwo) / 2.0, 4.0, 97.0);
    return new TimeSeriesProjection(100.0 - projectedOccupancy, slope);
  }

  private double[] reservationFeatures(
      WorkspaceReservation reservation, Map<String, SlotSnapshot> slotSnapshots) {
    SlotSnapshot slotSnapshot = slotSnapshots.get(reservation.getSlotId());
    double utilization = slotSnapshot != null ? slotSnapshot.utilizationRate() / 100.0 : 0.5;
    double pressure = slotSnapshot != null ? 1.0 - slotSnapshot.predictedAvailabilityRatio() : 0.5;
    double leadHours =
        Math.max(0.0, Duration.between(LocalDateTime.now(), reservation.getStartAt()).toHours());
    double durationHours =
        Math.max(
            1.0,
            Duration.between(reservation.getStartAt(), reservation.getEndAt()).toMinutes() / 60.0);
    int slotCapacity = slotSnapshot != null ? Math.max(slotSnapshot.capacity(), 1) : 1;

    return new double[] {
      clamp(utilization, 0.0, 1.2),
      clamp(pressure, 0.0, 1.2),
      clamp(leadHours / 168.0, 0.0, 1.5),
      clamp(durationHours / 10.0, 0.05, 1.8),
      clamp(defaultInteger(reservation.getHeadcount(), 1) / (double) Math.max(slotCapacity, 1), 0.0, 1.6),
      isBlank(reservation.getNotes()) ? 1.0 : 0.0,
      hasContactDestination(reservation) ? 0.0 : 1.0,
      slotSnapshot != null && slotSnapshot.maintenanceExposure() ? 1.0 : 0.0,
      defaultReservationRole(reservation.getRole()) == ReservationRole.consumer ? 1.0 : 0.0,
      clamp(scaleDouble(reservation.getAmount()) / 6000.0, 0.0, 2.0)
    };
  }

  private double[] orderFeatures(
      WorkspaceOrder order,
      Map<String, WorkspaceReservation> reservationById,
      Map<String, ReservationMlProfile> reservationProfiles) {
    WorkspaceReservation reservation = reservationById.get(order.getReservationId());
    ReservationMlProfile reservationProfile =
        reservation != null ? reservationProfiles.get(reservation.getId()) : null;
    double total = scaleDouble(resolveOrderTotal(order));
    double amount = scaleDouble(defaultBigDecimal(order.getAmount()));
    double tax = scaleDouble(defaultBigDecimal(order.getTax()));
    double itemCount = Math.max(safeSize(order.getItems()), 1);
    long orderCycleDays =
        order.getCreatedAt() != null && order.getDueDate() != null
            ? Math.max(1L, ChronoUnit.DAYS.between(order.getCreatedAt(), order.getDueDate()))
            : 7L;
    long dueDays =
        order.getDueDate() != null ? ChronoUnit.DAYS.between(LocalDate.now(), order.getDueDate()) : 7L;

    return new double[] {
      clamp(total / 7000.0, 0.0, 2.2),
      clamp(tax / Math.max(total, 1.0), 0.0, 0.5),
      clamp(1.0 - Math.min(Math.max(dueDays, -14L), 30L) / 30.0, 0.0, 1.5),
      clamp(itemCount / 6.0, 0.15, 2.0),
      clamp(amount / Math.max(itemCount, 1.0) / 1500.0, 0.0, 2.0),
      reservationProfile != null ? reservationProfile.probability() : 0.28,
      defaultReservationRole(order.getRole()) == ReservationRole.consumer ? 1.0 : 0.0,
      defaultPaymentStatus(order.getPaymentStatus()) == PaymentStatus.pending ? 1.0 : 0.0,
      clamp(orderCycleDays / 21.0, 0.1, 2.0)
    };
  }

  private double[] spendingClusterVector(
      WorkspaceOrder order,
      Map<String, WorkspaceReservation> reservationById,
      Map<String, ReservationMlProfile> reservationProfiles) {
    WorkspaceReservation reservation = reservationById.get(order.getReservationId());
    ReservationMlProfile reservationProfile =
        reservation != null ? reservationProfiles.get(reservation.getId()) : null;
    double total = scaleDouble(resolveOrderTotal(order));
    double tax = scaleDouble(defaultBigDecimal(order.getTax()));
    long dueDays =
        order.getDueDate() != null ? ChronoUnit.DAYS.between(LocalDate.now(), order.getDueDate()) : 7L;
    return new double[] {
      clamp(total / 7000.0, 0.0, 2.5),
      clamp(tax / Math.max(total, 1.0), 0.0, 0.5),
      clamp(Math.max(safeSize(order.getItems()), 1) / 6.0, 0.15, 2.0),
      clamp(1.0 - Math.min(Math.max(dueDays, -14L), 30L) / 30.0, 0.0, 1.5),
      reservationProfile != null ? reservationProfile.probability() : 0.25
    };
  }

  private void ensureReservationTrainingCoverage(List<FeatureSample> training) {
    boolean hasPositive = training.stream().anyMatch(sample -> sample.label() == 1);
    boolean hasNegative = training.stream().anyMatch(sample -> sample.label() == 0);

    if (!hasNegative || training.size() < 6) {
      training.add(new FeatureSample(new double[] {0.24, 0.26, 0.92, 0.24, 0.38, 0.0, 0.0, 0.0, 0.0, 0.32}, 0));
      training.add(new FeatureSample(new double[] {0.36, 0.34, 0.85, 0.30, 0.42, 0.0, 0.0, 0.0, 1.0, 0.44}, 0));
      training.add(new FeatureSample(new double[] {0.41, 0.40, 1.10, 0.38, 0.48, 0.0, 0.0, 0.0, 0.0, 0.58}, 0));
    }
    if (!hasPositive || training.size() < 8) {
      training.add(new FeatureSample(new double[] {0.88, 0.84, 0.08, 0.92, 1.25, 1.0, 1.0, 1.0, 1.0, 1.18}, 1));
      training.add(new FeatureSample(new double[] {0.76, 0.78, 0.16, 0.80, 1.05, 1.0, 1.0, 0.0, 0.0, 0.94}, 1));
      training.add(new FeatureSample(new double[] {0.69, 0.72, 0.12, 0.68, 0.92, 0.0, 1.0, 1.0, 1.0, 0.86}, 1));
    }
  }

  private void ensureOrderTrainingCoverage(List<FeatureSample> training) {
    boolean hasPositive = training.stream().anyMatch(sample -> sample.label() == 1);
    boolean hasNegative = training.stream().anyMatch(sample -> sample.label() == 0);

    if (!hasNegative || training.size() < 6) {
      training.add(new FeatureSample(new double[] {0.28, 0.08, 0.25, 0.35, 0.28, 0.20, 0.0, 0.0, 0.40}, 0));
      training.add(new FeatureSample(new double[] {0.48, 0.12, 0.36, 0.48, 0.44, 0.28, 1.0, 1.0, 0.60}, 0));
      training.add(new FeatureSample(new double[] {0.62, 0.09, 0.18, 0.55, 0.58, 0.18, 0.0, 0.0, 0.78}, 0));
    }
    if (!hasPositive || training.size() < 8) {
      training.add(new FeatureSample(new double[] {1.42, 0.22, 0.92, 1.10, 1.16, 0.78, 1.0, 1.0, 0.36}, 1));
      training.add(new FeatureSample(new double[] {1.18, 0.18, 0.88, 0.92, 0.96, 0.66, 0.0, 1.0, 0.44}, 1));
      training.add(new FeatureSample(new double[] {1.56, 0.26, 1.02, 1.24, 1.18, 0.84, 1.0, 1.0, 0.28}, 1));
    }
  }

  private ExplainedLogisticModel trainExplainedLogisticModel(
      List<FeatureSample> trainingSamples, List<String> driverLabels) {
    int featureCount = driverLabels.size();
    double[] means = new double[featureCount];
    double[] scales = new double[featureCount];

    for (int feature = 0; feature < featureCount; feature += 1) {
      double sum = 0.0;
      for (FeatureSample sample : trainingSamples) {
        sum += sample.features()[feature];
      }
      means[feature] = sum / trainingSamples.size();

      double variance = 0.0;
      for (FeatureSample sample : trainingSamples) {
        double centered = sample.features()[feature] - means[feature];
        variance += centered * centered;
      }
      scales[feature] = Math.sqrt(variance / trainingSamples.size());
      if (scales[feature] < 1e-6) {
        scales[feature] = 1.0;
      }
    }

    double[] weights = new double[featureCount + 1];
    Arrays.fill(weights, 0.0);
    double learningRate = 0.18;
    double regularization = 0.015;

    for (int iteration = 0; iteration < 520; iteration += 1) {
      double[] gradients = new double[weights.length];
      for (FeatureSample sample : trainingSamples) {
        double[] standardized = standardize(sample.features(), means, scales);
        double linear = weights[0];
        for (int feature = 0; feature < standardized.length; feature += 1) {
          linear += weights[feature + 1] * standardized[feature];
        }
        double predicted = sigmoid(linear);
        double error = predicted - sample.label();
        gradients[0] += error;
        for (int feature = 0; feature < standardized.length; feature += 1) {
          gradients[feature + 1] += error * standardized[feature];
        }
      }

      double scale = 1.0 / trainingSamples.size();
      weights[0] -= learningRate * gradients[0] * scale;
      for (int feature = 0; feature < featureCount; feature += 1) {
        weights[feature + 1] -=
            learningRate * (gradients[feature + 1] * scale + regularization * weights[feature + 1]);
      }
    }

    return new ExplainedLogisticModel(driverLabels, means, scales, weights);
  }

  private Map<Integer, String> rankSlotClusters(
      KMeansResult clusters, Map<Integer, Double> clusterDemand, Map<Integer, Double> clusterAvailability) {
    List<Integer> orderedClusters =
        clusterDemand.keySet().stream()
            .sorted(Comparator.comparingDouble(clusterDemand::get))
            .toList();

    Map<Integer, String> labels = new LinkedHashMap<>();
    for (int rank = 0; rank < orderedClusters.size(); rank += 1) {
      int cluster = orderedClusters.get(rank);
      String label =
          rank == 0
              ? "Recovery pocket"
              : rank == orderedClusters.size() - 1
                  ? "High-pressure node"
                  : clusterAvailability.getOrDefault(cluster, 0.0) > 45.0
                      ? "Balanced network node"
                      : "Tightening node";
      labels.put(cluster, label);
    }
    return labels;
  }

  private Map<Integer, String> rankOrderClusters(
      KMeansResult clusters,
      List<WorkspaceOrder> orders,
      Map<String, WorkspaceReservation> reservationById,
      Map<String, ReservationMlProfile> reservationProfiles) {
    Map<Integer, List<WorkspaceOrder>> grouped = new LinkedHashMap<>();
    for (int index = 0; index < orders.size(); index += 1) {
      grouped.computeIfAbsent(clusters.assignments()[index], ignored -> new ArrayList<>()).add(orders.get(index));
    }

    List<Map.Entry<Integer, List<WorkspaceOrder>>> ordered =
        grouped.entrySet().stream()
            .sorted(
                Comparator.comparingDouble(
                    entry ->
                        entry.getValue().stream()
                            .map(this::resolveOrderTotal)
                            .mapToDouble(this::scaleDouble)
                            .average()
                            .orElse(0.0)))
            .toList();

    Map<Integer, String> labels = new LinkedHashMap<>();
    for (int rank = 0; rank < ordered.size(); rank += 1) {
      Map.Entry<Integer, List<WorkspaceOrder>> entry = ordered.get(rank);
      double avgRisk =
          entry.getValue().stream()
              .map(order -> reservationById.get(order.getReservationId()))
              .filter(reservation -> reservation != null)
              .map(reservation -> reservationProfiles.get(reservation.getId()))
              .filter(profile -> profile != null)
              .mapToDouble(ReservationMlProfile::probability)
              .average()
              .orElse(0.25);

      String label =
          rank == ordered.size() - 1
              ? avgRisk > 0.55 ? "Strategic account under watch" : "Strategic revenue stream"
              : rank == 0
                  ? avgRisk > 0.45 ? "Sensitive lightweight file" : "Light recurring flow"
                  : avgRisk > 0.5 ? "Priority supplier mission" : "Balanced commercial flow";
      labels.put(entry.getKey(), label);
    }
    return labels;
  }

  private KMeansResult runKMeans(List<double[]> vectors, int requestedClusters) {
    if (vectors.isEmpty()) {
      return new KMeansResult(new int[0], List.of());
    }

    int clusterCount = Math.max(1, Math.min(requestedClusters, vectors.size()));
    List<double[]> centroids = initializeCentroids(vectors, clusterCount);
    int[] assignments = new int[vectors.size()];
    Arrays.fill(assignments, 0);

    for (int iteration = 0; iteration < 14; iteration += 1) {
      boolean changed = false;
      for (int index = 0; index < vectors.size(); index += 1) {
        int nearest = nearestCentroid(vectors.get(index), centroids);
        if (assignments[index] != nearest) {
          assignments[index] = nearest;
          changed = true;
        }
      }

      centroids = recomputeCentroids(vectors, assignments, centroids, clusterCount);
      if (!changed && iteration > 2) {
        break;
      }
    }
    return new KMeansResult(assignments, centroids);
  }

  private List<double[]> initializeCentroids(List<double[]> vectors, int clusterCount) {
    if (clusterCount == 1) {
      return List.of(copyVector(vectors.get(0)));
    }

    List<double[]> centroids = new ArrayList<>();
    centroids.add(copyVector(vectors.get(0)));
    if (clusterCount > 1) {
      centroids.add(copyVector(vectors.get(vectors.size() - 1)));
    }
    if (clusterCount > 2) {
      centroids.add(copyVector(vectors.get(vectors.size() / 2)));
    }
    while (centroids.size() < clusterCount) {
      centroids.add(copyVector(vectors.get(centroids.size() % vectors.size())));
    }
    return centroids;
  }

  private List<double[]> recomputeCentroids(
      List<double[]> vectors, int[] assignments, List<double[]> previous, int clusterCount) {
    int dimensions = vectors.get(0).length;
    List<double[]> sums = new ArrayList<>();
    List<Integer> counts = new ArrayList<>();
    for (int cluster = 0; cluster < clusterCount; cluster += 1) {
      sums.add(new double[dimensions]);
      counts.add(0);
    }

    for (int index = 0; index < vectors.size(); index += 1) {
      int cluster = assignments[index];
      double[] vector = vectors.get(index);
      double[] sum = sums.get(cluster);
      for (int dimension = 0; dimension < dimensions; dimension += 1) {
        sum[dimension] += vector[dimension];
      }
      counts.set(cluster, counts.get(cluster) + 1);
    }

    List<double[]> centroids = new ArrayList<>();
    for (int cluster = 0; cluster < clusterCount; cluster += 1) {
      if (counts.get(cluster) == 0) {
        centroids.add(copyVector(previous.get(cluster)));
        continue;
      }
      double[] centroid = new double[dimensions];
      for (int dimension = 0; dimension < dimensions; dimension += 1) {
        centroid[dimension] = sums.get(cluster)[dimension] / counts.get(cluster);
      }
      centroids.add(centroid);
    }
    return centroids;
  }

  private int nearestCentroid(double[] vector, List<double[]> centroids) {
    int bestIndex = 0;
    double bestDistance = Double.MAX_VALUE;
    for (int index = 0; index < centroids.size(); index += 1) {
      double distance = squaredDistance(vector, centroids.get(index));
      if (distance < bestDistance) {
        bestDistance = distance;
        bestIndex = index;
      }
    }
    return bestIndex;
  }

  private double[] standardize(double[] raw, double[] means, double[] scales) {
    double[] standardized = new double[raw.length];
    for (int index = 0; index < raw.length; index += 1) {
      standardized[index] = (raw[index] - means[index]) / scales[index];
    }
    return standardized;
  }

  private double squaredDistance(double[] left, double[] right) {
    double distance = 0.0;
    for (int index = 0; index < left.length; index += 1) {
      double delta = left[index] - right[index];
      distance += delta * delta;
    }
    return distance;
  }

  private String describeDemandTrend(double slope) {
    if (slope <= -1.8) {
      return "Demand easing";
    }
    if (slope >= 1.8) {
      return "Demand rising";
    }
    return "Demand steady";
  }

  private BigDecimal resolveOrderTotal(WorkspaceOrder order) {
    if (order.getTotal() != null && order.getTotal().signum() > 0) {
      return order.getTotal();
    }
    BigDecimal itemsTotal = BigDecimal.ZERO;
    if (order.getItems() != null) {
      for (WorkspacePayloads.OrderLineItem item : order.getItems()) {
        BigDecimal unitPrice = defaultBigDecimal(item.unitPrice());
        int quantity = Math.max(defaultInteger(item.quantity(), 1), 1);
        itemsTotal = itemsTotal.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
      }
    }
    if (itemsTotal.signum() == 0) {
      itemsTotal = defaultBigDecimal(order.getAmount());
    }
    return itemsTotal.add(defaultBigDecimal(order.getTax()));
  }

  private boolean isFlaggedOrder(WorkspaceOrder order) {
    return defaultOrderStatus(order.getStatus()) == OrderStatus.flagged
        || defaultPaymentStatus(order.getPaymentStatus()) == PaymentStatus.review;
  }

  private boolean hasContactDestination(WorkspaceReservation reservation) {
    return !isBlank(reservation.getContactEmail()) || !isBlank(reservation.getContactPhone());
  }

  private double scaleDouble(BigDecimal value) {
    return value == null ? 0.0 : value.doubleValue();
  }

  private BigDecimal defaultBigDecimal(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private int defaultInteger(Integer value, int fallback) {
    return value == null ? fallback : value;
  }

  private int safeSize(List<?> values) {
    return values == null ? 0 : values.size();
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }

  private double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }

  private double sigmoid(double value) {
    if (value >= 0.0) {
      double exp = Math.exp(-value);
      return 1.0 / (1.0 + exp);
    }
    double exp = Math.exp(value);
    return exp / (1.0 + exp);
  }

  private double[] copyVector(double[] values) {
    return Arrays.copyOf(values, values.length);
  }

  private ReservationRole defaultReservationRole(ReservationRole role) {
    return role == null ? ReservationRole.provider : role;
  }

  private ReservationStatus defaultReservationStatus(ReservationStatus status) {
    return status == null ? ReservationStatus.pending : status;
  }

  private OrderStatus defaultOrderStatus(OrderStatus status) {
    return status == null ? OrderStatus.draft : status;
  }

  private PaymentStatus defaultPaymentStatus(PaymentStatus status) {
    return status == null ? PaymentStatus.pending : status;
  }

  private SlotStatus defaultSlotStatus(SlotStatus status) {
    return status == null ? SlotStatus.available : status;
  }

  private SlotPortfolio defaultSlotPortfolio(SlotPortfolio portfolio) {
    return portfolio == null ? SlotPortfolio.owned : portfolio;
  }

  public record WorkspaceMlAnalysis(
      Map<String, SlotMlProfile> slotProfiles,
      Map<String, ReservationMlProfile> reservationProfiles,
      Map<String, OrderMlProfile> orderProfiles) {}

  public record SlotMlProfile(
      int predictedAvailability,
      int underusedScore,
      String demandTrend,
      String segmentLabel) {}

  public record ReservationMlProfile(
      int cancellationRisk,
      int readinessScore,
      double probability,
      List<String> topDrivers) {}

  public record OrderMlProfile(
      int fraudRisk, String spendingCluster, double probability, List<String> topDrivers) {}

  private record SlotSnapshot(
      String id,
      int predictedAvailability,
      int underusedScore,
      String demandTrendLabel,
      String segmentLabel,
      double utilizationRate,
      double predictedAvailabilityRatio,
      boolean maintenanceExposure,
      int capacity) {}

  private record BaseSlotVector(
      String id,
      double utilizationRate,
      double predictedAvailability,
      double trendSlope,
      double forecastAverage,
      int equipmentCount,
      SlotStatus status,
      SlotPortfolio portfolio,
      int capacity) {}

  private record TimeSeriesProjection(double predictedAvailability, double trendSlope) {}

  private record FeatureSample(double[] features, int label) {}

  private record KMeansResult(int[] assignments, List<double[]> centroids) {}

  private record ExplainedLogisticModel(
      List<String> driverLabels, double[] means, double[] scales, double[] weights) {

    double predictProbability(double[] rawFeatures) {
      double[] standardized = standardize(rawFeatures);
      double linear = weights[0];
      for (int index = 0; index < standardized.length; index += 1) {
        linear += weights[index + 1] * standardized[index];
      }
      if (linear >= 0.0) {
        double exp = Math.exp(-linear);
        return 1.0 / (1.0 + exp);
      }
      double exp = Math.exp(linear);
      return exp / (1.0 + exp);
    }

    List<String> topDrivers(double[] rawFeatures, int limit) {
      double[] standardized = standardize(rawFeatures);
      List<FeatureContribution> contributions = new ArrayList<>();
      for (int index = 0; index < standardized.length; index += 1) {
        double contribution = standardized[index] * weights[index + 1];
        if (contribution <= 0.01) {
          continue;
        }
        contributions.add(new FeatureContribution(driverLabels.get(index), contribution));
      }

      if (contributions.isEmpty()) {
        contributions.add(new FeatureContribution("overall portfolio drift", 0.1));
      }

      return contributions.stream()
          .sorted(Comparator.comparingDouble(FeatureContribution::weight).reversed())
          .limit(limit)
          .map(FeatureContribution::label)
          .map(label -> label.substring(0, 1).toUpperCase(Locale.ROOT) + label.substring(1))
          .toList();
    }

    private double[] standardize(double[] rawFeatures) {
      double[] standardized = new double[rawFeatures.length];
      for (int index = 0; index < rawFeatures.length; index += 1) {
        standardized[index] = (rawFeatures[index] - means[index]) / scales[index];
      }
      return standardized;
    }
  }

  private record FeatureContribution(String label, double weight) {}
}
