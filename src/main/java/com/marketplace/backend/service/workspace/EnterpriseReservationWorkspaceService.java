package com.marketplace.backend.service.workspace;

import com.marketplace.backend.dto.workspace.ReservationWorkspaceDtos.ActivityCenterItemDto;
import com.marketplace.backend.dto.workspace.ReservationWorkspaceDtos.InsightCardDto;
import com.marketplace.backend.dto.workspace.ReservationWorkspaceDtos.OrderLineItemDto;
import com.marketplace.backend.dto.workspace.ReservationWorkspaceDtos.ReservationConfirmationNoticeDto;
import com.marketplace.backend.dto.workspace.ReservationWorkspaceDtos.ReservationConflictDto;
import com.marketplace.backend.dto.workspace.ReservationWorkspaceDtos.ReservationNotificationEventDto;
import com.marketplace.backend.dto.workspace.ReservationWorkspaceDtos.TrackingStepDto;
import com.marketplace.backend.dto.workspace.ReservationWorkspaceDtos.WorkspaceBootstrapResponse;
import com.marketplace.backend.dto.workspace.ReservationWorkspaceDtos.WorkspaceOrderRequest;
import com.marketplace.backend.dto.workspace.ReservationWorkspaceDtos.WorkspaceOrderResponse;
import com.marketplace.backend.dto.workspace.ReservationWorkspaceDtos.WorkspaceReservationRequest;
import com.marketplace.backend.dto.workspace.ReservationWorkspaceDtos.WorkspaceReservationResponse;
import com.marketplace.backend.dto.workspace.ReservationWorkspaceDtos.WorkspaceSlotRequest;
import com.marketplace.backend.dto.workspace.ReservationWorkspaceDtos.WorkspaceSlotResponse;
import com.marketplace.backend.dto.workspace.ReservationWorkspaceDtos.WorkspaceSummaryDto;
import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.enums.Role;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.InsightTone;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.NotificationChannel;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.OrderStatus;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.PaymentStatus;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.ReservationCategory;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.ReservationRole;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.ReservationStatus;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.SlotPortfolio;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.SlotStatus;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.SlotType;
import com.marketplace.backend.entity.workspace.WorkspaceOrder;
import com.marketplace.backend.entity.workspace.WorkspacePayloads;
import com.marketplace.backend.entity.workspace.WorkspaceReservation;
import com.marketplace.backend.entity.workspace.WorkspaceReservationSlot;
import com.marketplace.backend.repository.EnterpriseRepository;
import com.marketplace.backend.repository.workspace.WorkspaceOrderRepository;
import com.marketplace.backend.repository.workspace.WorkspaceReservationRepository;
import com.marketplace.backend.repository.workspace.WorkspaceReservationSlotRepository;
import com.marketplace.backend.security.SecurityUserHelper;
import com.marketplace.backend.service.EmailService;
import com.marketplace.backend.service.RealtimeNotificationService;
import com.marketplace.backend.service.workspace.WorkspaceSmsService.SmsDispatchResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnterpriseReservationWorkspaceService {

  private static final List<String> HEATMAP_DAYS =
      List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");
  private static final List<String> HEATMAP_HOURS =
      List.of("07h", "09h", "11h", "13h", "15h", "17h", "19h");
  private static final DateTimeFormatter WINDOW_FORMAT =
      DateTimeFormatter.ofPattern("MMM d HH:mm", Locale.ENGLISH);

  private final WorkspaceReservationSlotRepository slotRepository;
  private final WorkspaceReservationRepository reservationRepository;
  private final WorkspaceOrderRepository orderRepository;
  private final EnterpriseRepository enterpriseRepository;
  private final SecurityUserHelper securityUserHelper;
  private final EmailService emailService;
  private final RealtimeNotificationService realtimeNotificationService;
  private final WorkspaceSmsService workspaceSmsService;
  private final WorkspaceMlService workspaceMlService;

  @Transactional(readOnly = true)
  public WorkspaceBootstrapResponse bootstrap(Authentication auth, Long enterpriseId) {
    Enterprise enterprise = resolveReadableEnterprise(auth, enterpriseId);
    WorkspaceContext context = buildContext(enterprise);
    return new WorkspaceBootstrapResponse(
        context.summary(),
        context.reservations(),
        context.slots(),
        context.orders(),
        context.conflicts(),
        context.reservationInsights(),
        context.slotInsights(),
        context.orderInsights(),
        context.reservationNotifications(),
        context.activityFeed());
  }

  @Transactional(readOnly = true)
  public WorkspaceSummaryDto summary(Authentication auth, Long enterpriseId) {
    return buildContext(resolveReadableEnterprise(auth, enterpriseId)).summary();
  }

  @Transactional(readOnly = true)
  public List<WorkspaceReservationResponse> listReservations(Authentication auth, Long enterpriseId) {
    return buildContext(resolveReadableEnterprise(auth, enterpriseId)).reservations();
  }

  @Transactional(readOnly = true)
  public WorkspaceReservationResponse getReservation(String id, Authentication auth) {
    WorkspaceReservation reservation =
        reservationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Workspace reservation not found"));
    assertCanAccess(securityUserHelper.requireUser(auth), reservation.getEnterprise());
    return buildContext(reservation.getEnterprise()).reservationById().get(id);
  }

  @Transactional
  public WorkspaceReservationResponse createReservation(
      Authentication auth, WorkspaceReservationRequest request) {
    Enterprise enterprise = resolveWritableEnterprise(auth, request.enterpriseId());
    WorkspaceReservationSlot slot = findSlotForEnterprise(enterprise.getId(), request.slotId());
    validateReservationWindow(request.start(), request.end());

    WorkspaceReservation reservation =
        WorkspaceReservation.builder()
            .id(
                nextIdentifier(
                    "RES",
                    reservationRepository.findByEnterpriseIdOrderByStartAtAsc(enterprise.getId()).stream()
                        .map(WorkspaceReservation::getId)
                        .toList()))
            .code(
                nextIdentifier(
                    "RSV",
                    reservationRepository.findByEnterpriseIdOrderByStartAtAsc(enterprise.getId()).stream()
                        .map(WorkspaceReservation::getCode)
                        .toList()))
            .enterprise(enterprise)
            .build();

    applyReservationMutation(reservation, request, enterprise, slot);
    reservationRepository.save(reservation);
    syncReservationConfirmation(reservation, true);
    reservationRepository.save(reservation);

    return buildContext(enterprise).reservationById().get(reservation.getId());
  }

  @Transactional
  public WorkspaceReservationResponse updateReservation(
      String id, Authentication auth, WorkspaceReservationRequest request) {
    WorkspaceReservation reservation =
        reservationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Workspace reservation not found"));

    User user = securityUserHelper.requireUser(auth);
    assertCanAccess(user, reservation.getEnterprise());

    Enterprise targetEnterprise =
        request.enterpriseId() != null
            ? resolveWritableEnterprise(auth, request.enterpriseId())
            : reservation.getEnterprise();
    WorkspaceReservationSlot slot = findSlotForEnterprise(targetEnterprise.getId(), request.slotId());
    validateReservationWindow(request.start(), request.end());

    ReservationStatus previousStatus = reservation.getStatus();
    String previousEmail = normalizeText(reservation.getContactEmail());
    String previousPhone = normalizeText(reservation.getContactPhone());
    NotificationChannel previousChannel = reservation.getNotificationChannel();

    applyReservationMutation(reservation, request, targetEnterprise, slot);
    reservationRepository.save(reservation);

    boolean destinationChanged =
        !Objects.equals(previousEmail, normalizeText(reservation.getContactEmail()))
            || !Objects.equals(previousPhone, normalizeText(reservation.getContactPhone()))
            || previousChannel != reservation.getNotificationChannel();
    boolean shouldDispatch =
        reservation.getStatus() == ReservationStatus.confirmed
            && (previousStatus != ReservationStatus.confirmed
                || destinationChanged
                || reservation.getConfirmationSentAt() == null);

    syncReservationConfirmation(reservation, shouldDispatch);
    reservationRepository.save(reservation);

    return buildContext(targetEnterprise).reservationById().get(reservation.getId());
  }

  @Transactional
  public WorkspaceReservationResponse sendReservationConfirmation(String id, Authentication auth) {
    WorkspaceReservation reservation =
        reservationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Workspace reservation not found"));
    assertCanAccess(securityUserHelper.requireUser(auth), reservation.getEnterprise());

    if (defaultReservationStatus(reservation.getStatus()) != ReservationStatus.confirmed) {
      throw new IllegalArgumentException("Reservation must be confirmed before sending a notice");
    }

    syncReservationConfirmation(reservation, true);
    reservationRepository.save(reservation);
    return buildContext(reservation.getEnterprise()).reservationById().get(id);
  }

  @Transactional
  public void deleteReservation(String id, Authentication auth) {
    WorkspaceReservation reservation =
        reservationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Workspace reservation not found"));
    assertCanAccess(securityUserHelper.requireUser(auth), reservation.getEnterprise());

    orderRepository.deleteAll(
        orderRepository.findByEnterpriseIdAndReservationId(
            reservation.getEnterprise().getId(), reservation.getId()));
    reservationRepository.delete(reservation);
  }

  @Transactional(readOnly = true)
  public List<ReservationConflictDto> reservationConflicts(Authentication auth, Long enterpriseId) {
    return buildContext(resolveReadableEnterprise(auth, enterpriseId)).conflicts();
  }

  @Transactional(readOnly = true)
  public List<InsightCardDto> reservationInsights(Authentication auth, Long enterpriseId) {
    return buildContext(resolveReadableEnterprise(auth, enterpriseId)).reservationInsights();
  }

  @Transactional(readOnly = true)
  public List<ReservationNotificationEventDto> reservationNotifications(
      Authentication auth, Long enterpriseId) {
    return buildContext(resolveReadableEnterprise(auth, enterpriseId)).reservationNotifications();
  }

  @Transactional(readOnly = true)
  public List<ActivityCenterItemDto> activityFeed(Authentication auth, Long enterpriseId) {
    return buildContext(resolveReadableEnterprise(auth, enterpriseId)).activityFeed();
  }

  @Transactional(readOnly = true)
  public List<WorkspaceSlotResponse> listSlots(Authentication auth, Long enterpriseId) {
    return buildContext(resolveReadableEnterprise(auth, enterpriseId)).slots();
  }

  @Transactional(readOnly = true)
  public WorkspaceSlotResponse getSlot(String id, Authentication auth) {
    WorkspaceReservationSlot slot =
        slotRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Workspace slot not found"));
    assertCanAccess(securityUserHelper.requireUser(auth), slot.getEnterprise());
    return buildContext(slot.getEnterprise()).slotById().get(id);
  }

  @Transactional
  public WorkspaceSlotResponse createSlot(Authentication auth, WorkspaceSlotRequest request) {
    Enterprise enterprise = resolveWritableEnterprise(auth, request.enterpriseId());
    validateCoordinates(request.coordinates());
    validateOccupiedCapacity(request.occupied(), request.capacity());

    WorkspaceReservationSlot slot =
        WorkspaceReservationSlot.builder()
            .id(
                nextIdentifier(
                    "SLT",
                    slotRepository.findByEnterpriseIdOrderByNameAsc(enterprise.getId()).stream()
                        .map(WorkspaceReservationSlot::getId)
                        .toList()))
            .enterprise(enterprise)
            .build();

    applySlotMutation(slot, request, enterprise);
    slotRepository.save(slot);
    return buildContext(enterprise).slotById().get(slot.getId());
  }

  @Transactional
  public WorkspaceSlotResponse updateSlot(String id, Authentication auth, WorkspaceSlotRequest request) {
    WorkspaceReservationSlot slot =
        slotRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Workspace slot not found"));

    User user = securityUserHelper.requireUser(auth);
    assertCanAccess(user, slot.getEnterprise());

    Enterprise targetEnterprise =
        request.enterpriseId() != null ? resolveWritableEnterprise(auth, request.enterpriseId()) : slot.getEnterprise();
    validateCoordinates(request.coordinates());
    validateOccupiedCapacity(request.occupied(), request.capacity());

    applySlotMutation(slot, request, targetEnterprise);
    slotRepository.save(slot);
    propagateSlotSnapshotName(targetEnterprise, slot);
    return buildContext(targetEnterprise).slotById().get(slot.getId());
  }

  @Transactional
  public void deleteSlot(String id, Authentication auth) {
    WorkspaceReservationSlot slot =
        slotRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Workspace slot not found"));
    assertCanAccess(securityUserHelper.requireUser(auth), slot.getEnterprise());
    slotRepository.delete(slot);
  }

  @Transactional(readOnly = true)
  public List<InsightCardDto> slotInsights(Authentication auth, Long enterpriseId) {
    return buildContext(resolveReadableEnterprise(auth, enterpriseId)).slotInsights();
  }

  @Transactional(readOnly = true)
  public List<WorkspaceOrderResponse> listOrders(Authentication auth, Long enterpriseId) {
    return buildContext(resolveReadableEnterprise(auth, enterpriseId)).orders();
  }

  @Transactional(readOnly = true)
  public WorkspaceOrderResponse getOrder(String id, Authentication auth) {
    WorkspaceOrder order =
        orderRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Workspace order not found"));
    assertCanAccess(securityUserHelper.requireUser(auth), order.getEnterprise());
    return buildContext(order.getEnterprise()).orderById().get(id);
  }

  @Transactional
  public WorkspaceOrderResponse createOrder(Authentication auth, WorkspaceOrderRequest request) {
    Enterprise enterprise = resolveWritableEnterprise(auth, request.enterpriseId());
    assertReservationExists(enterprise.getId(), request.reservationId());
    assertSlotExists(enterprise.getId(), request.slotId());
    validateOrderDates(request.createdAt(), request.dueDate());

    WorkspaceOrder order =
        WorkspaceOrder.builder()
            .id(
                nextIdentifier(
                    "ORD",
                    orderRepository.findByEnterpriseIdOrderByCreatedAtDesc(enterprise.getId()).stream()
                        .map(WorkspaceOrder::getId)
                        .toList()))
            .code(
                nextIdentifier(
                    "OR",
                    orderRepository.findByEnterpriseIdOrderByCreatedAtDesc(enterprise.getId()).stream()
                        .map(WorkspaceOrder::getCode)
                        .toList()))
            .invoiceNumber(
                nextIdentifier(
                    "INV",
                    orderRepository.findByEnterpriseIdOrderByCreatedAtDesc(enterprise.getId()).stream()
                        .map(WorkspaceOrder::getInvoiceNumber)
                        .toList()))
            .enterprise(enterprise)
            .build();

    applyOrderMutation(order, request, enterprise);
    orderRepository.save(order);
    return buildContext(enterprise).orderById().get(order.getId());
  }

  @Transactional
  public WorkspaceOrderResponse updateOrder(String id, Authentication auth, WorkspaceOrderRequest request) {
    WorkspaceOrder order =
        orderRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Workspace order not found"));

    User user = securityUserHelper.requireUser(auth);
    assertCanAccess(user, order.getEnterprise());

    Enterprise targetEnterprise =
        request.enterpriseId() != null ? resolveWritableEnterprise(auth, request.enterpriseId()) : order.getEnterprise();
    assertReservationExists(targetEnterprise.getId(), request.reservationId());
    assertSlotExists(targetEnterprise.getId(), request.slotId());
    validateOrderDates(request.createdAt(), request.dueDate());

    applyOrderMutation(order, request, targetEnterprise);
    orderRepository.save(order);
    return buildContext(targetEnterprise).orderById().get(order.getId());
  }

  @Transactional
  public WorkspaceOrderResponse advanceOrderStatus(String id, Authentication auth) {
    WorkspaceOrder order =
        orderRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Workspace order not found"));
    assertCanAccess(securityUserHelper.requireUser(auth), order.getEnterprise());

    List<OrderStatus> flow =
        List.of(OrderStatus.draft, OrderStatus.processing, OrderStatus.invoiced, OrderStatus.fulfilled);
    OrderStatus currentStatus = defaultOrderStatus(order.getStatus());

    if (currentStatus != OrderStatus.flagged && currentStatus != OrderStatus.fulfilled) {
      int currentIndex = flow.indexOf(currentStatus);
      OrderStatus nextStatus = flow.get(Math.min(currentIndex + 1, flow.size() - 1));
      order.setStatus(nextStatus);
      if (nextStatus == OrderStatus.fulfilled) {
        order.setPaymentStatus(PaymentStatus.paid);
      }
      orderRepository.save(order);
    }

    return buildContext(order.getEnterprise()).orderById().get(order.getId());
  }

  @Transactional
  public void deleteOrder(String id, Authentication auth) {
    WorkspaceOrder order =
        orderRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Workspace order not found"));
    assertCanAccess(securityUserHelper.requireUser(auth), order.getEnterprise());
    orderRepository.delete(order);
  }

  @Transactional(readOnly = true)
  public List<InsightCardDto> orderInsights(Authentication auth, Long enterpriseId) {
    return buildContext(resolveReadableEnterprise(auth, enterpriseId)).orderInsights();
  }

  private Enterprise resolveReadableEnterprise(Authentication auth, Long enterpriseId) {
    User user = securityUserHelper.requireUser(auth);
    if (user.getRole() == Role.ROLE_ADMIN) {
      if (enterpriseId != null) {
        return enterpriseRepository
            .findById(enterpriseId)
            .orElseThrow(() -> new IllegalArgumentException("Enterprise not found"));
      }
      return enterpriseRepository
          .findAll()
          .stream()
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("No enterprise available"));
    }

    Enterprise enterprise = securityUserHelper.requireEnterpriseStrict(auth);
    if (enterpriseId != null && !Objects.equals(enterprise.getId(), enterpriseId)) {
      throw new IllegalArgumentException("Forbidden");
    }
    return enterprise;
  }

  private Enterprise resolveWritableEnterprise(Authentication auth, Long enterpriseId) {
    User user = securityUserHelper.requireUser(auth);
    if (user.getRole() == Role.ROLE_ADMIN) {
      if (enterpriseId == null) {
        throw new IllegalArgumentException("enterpriseId is required for admin requests");
      }
      return enterpriseRepository
          .findById(enterpriseId)
          .orElseThrow(() -> new IllegalArgumentException("Enterprise not found"));
    }

    Enterprise enterprise = securityUserHelper.requireEnterpriseStrict(auth);
    if (enterpriseId != null && !Objects.equals(enterprise.getId(), enterpriseId)) {
      throw new IllegalArgumentException("Forbidden");
    }
    return enterprise;
  }

  private void assertCanAccess(User user, Enterprise enterprise) {
    if (user.getRole() == Role.ROLE_ADMIN) {
      return;
    }
    if (user.getEnterprise() == null || !Objects.equals(user.getEnterprise().getId(), enterprise.getId())) {
      throw new IllegalArgumentException("Forbidden");
    }
  }

  private WorkspaceReservationSlot findSlotForEnterprise(Long enterpriseId, String slotId) {
    WorkspaceReservationSlot slot =
        slotRepository.findById(slotId)
            .orElseThrow(() -> new IllegalArgumentException("Workspace slot not found"));
    if (!Objects.equals(slot.getEnterprise().getId(), enterpriseId)) {
      throw new IllegalArgumentException("Slot does not belong to the selected enterprise");
    }
    return slot;
  }

  private void assertSlotExists(Long enterpriseId, String slotId) {
    findSlotForEnterprise(enterpriseId, slotId);
  }

  private void assertReservationExists(Long enterpriseId, String reservationId) {
    WorkspaceReservation reservation =
        reservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("Workspace reservation not found"));
    if (!Objects.equals(reservation.getEnterprise().getId(), enterpriseId)) {
      throw new IllegalArgumentException("Reservation does not belong to the selected enterprise");
    }
  }

  private void validateCoordinates(List<Double> coordinates) {
    if (coordinates == null || coordinates.size() != 2) {
      throw new IllegalArgumentException("Coordinates must contain latitude and longitude");
    }
  }

  private void validateOccupiedCapacity(Integer occupied, Integer capacity) {
    if (occupied == null || capacity == null || occupied < 0 || capacity < 1 || occupied > capacity) {
      throw new IllegalArgumentException("Occupied capacity must stay between 0 and total capacity");
    }
  }

  private void validateReservationWindow(LocalDateTime start, LocalDateTime end) {
    if (start == null || end == null || !end.isAfter(start)) {
      throw new IllegalArgumentException("Reservation end time must be after start time");
    }
  }

  private void validateOrderDates(LocalDate createdAt, LocalDate dueDate) {
    if (createdAt == null || dueDate == null || dueDate.isBefore(createdAt)) {
      throw new IllegalArgumentException("Order due date must be on or after the created date");
    }
  }

  private void validateNotificationDestination(WorkspaceReservation reservation) {
    NotificationChannel channel = resolveEffectiveChannel(reservation);
    boolean hasEmail = !isBlank(reservation.getContactEmail());
    boolean hasPhone = !isBlank(reservation.getContactPhone());
    if (channel == NotificationChannel.email && !hasEmail) {
      throw new IllegalArgumentException("A confirmed reservation requires an email destination");
    }
    if (channel == NotificationChannel.sms && !hasPhone) {
      throw new IllegalArgumentException("A confirmed reservation requires a phone destination");
    }
    if (channel == NotificationChannel.both && (!hasEmail || !hasPhone)) {
      throw new IllegalArgumentException(
          "A confirmed reservation using both channels requires both email and phone");
    }
  }

  private void applyReservationMutation(
      WorkspaceReservation reservation,
      WorkspaceReservationRequest request,
      Enterprise enterprise,
      WorkspaceReservationSlot slot) {
    reservation.setEnterprise(enterprise);
    reservation.setTitle(normalizeText(request.title()));
    reservation.setCustomer(normalizeText(request.customer()));
    reservation.setResource(normalizeText(request.resource()));
    reservation.setSlotId(slot.getId());
    reservation.setSlotName(slot.getName());
    reservation.setRole(request.role());
    reservation.setCity(firstNonBlank(request.city(), slot.getCity(), enterpriseCity(enterprise), "Tunis"));
    reservation.setCategory(request.category());
    reservation.setStartAt(request.start());
    reservation.setEndAt(request.end());
    reservation.setHeadcount(request.headcount());
    reservation.setAmount(scale(request.amount()));
    reservation.setStatus(request.status());
    reservation.setNotes(normalizeText(request.notes()));
    reservation.setTags(safeStringList(request.tags()));
    reservation.setContactName(normalizeText(request.contactName()));
    reservation.setContactEmail(normalizeText(request.contactEmail()));
    reservation.setContactPhone(normalizeText(request.contactPhone()));
    reservation.setNotificationChannel(defaultNotificationChannel(request.notificationChannel()));
  }

  private void applySlotMutation(
      WorkspaceReservationSlot slot, WorkspaceSlotRequest request, Enterprise enterprise) {
    slot.setEnterprise(enterprise);
    slot.setName(normalizeText(request.name()));
    slot.setZone(normalizeText(request.zone()));
    slot.setCity(firstNonBlank(request.city(), enterpriseCity(enterprise), "Tunis"));
    slot.setOwnerCompany(firstNonBlank(request.ownerCompany(), enterprise.getCompanyName()));
    slot.setPortfolio(defaultSlotPortfolio(request.portfolio()));
    slot.setType(request.type());
    slot.setCoordinates(safeDoubleList(request.coordinates()));
    slot.setCapacity(request.capacity());
    slot.setOccupied(request.occupied());
    slot.setStatus(defaultSlotStatus(request.status()));
    slot.setEquipment(safeStringList(request.equipment()));
    slot.setHeatmap(createHeatmapFromStatus(defaultSlotStatus(request.status()), slot.getHeatmap()));
    slot.setForecast(createForecastFromStatus(defaultSlotStatus(request.status()), slot.getForecast()));
  }

  private void applyOrderMutation(
      WorkspaceOrder order, WorkspaceOrderRequest request, Enterprise enterprise) {
    List<WorkspacePayloads.OrderLineItem> items = mapItems(request.items());
    BigDecimal subtotal = computeItemsSubtotal(items);
    BigDecimal resolvedAmount = subtotal.signum() > 0 ? subtotal : scale(request.amount());
    BigDecimal tax = scale(request.tax());

    order.setEnterprise(enterprise);
    order.setCustomer(normalizeText(request.customer()));
    order.setReservationId(request.reservationId());
    order.setSlotId(request.slotId());
    order.setRole(defaultReservationRole(request.role()));
    order.setCity(firstNonBlank(request.city(), enterpriseCity(enterprise), "Tunis"));
    order.setAmount(resolvedAmount);
    order.setTax(tax);
    order.setTotal(scale(resolvedAmount.add(tax)));
    order.setCreatedAt(request.createdAt());
    order.setDueDate(request.dueDate());
    order.setStatus(defaultOrderStatus(request.status()));
    order.setPaymentStatus(defaultPaymentStatus(request.paymentStatus()));
    order.setItems(items);
  }

  private void syncReservationConfirmation(WorkspaceReservation reservation, boolean dispatchNow) {
    if (defaultReservationStatus(reservation.getStatus()) != ReservationStatus.confirmed) {
      clearConfirmationNotice(reservation);
      return;
    }

    validateNotificationDestination(reservation);
    if (!dispatchNow && reservation.getConfirmationSentAt() != null) {
      return;
    }

    NotificationChannel resolvedChannel = resolveEffectiveChannel(reservation);
    String destination = resolveDestination(reservation, resolvedChannel);
    LocalDateTime sentAt = LocalDateTime.now();
    NotificationDispatchOutcome dispatchOutcome =
        dispatchReservationConfirmation(reservation, resolvedChannel, destination);
    String summary = buildConfirmationSummary(resolvedChannel, destination, dispatchOutcome);

    reservation.setConfirmationChannel(resolvedChannel);
    reservation.setConfirmationDestination(destination);
    reservation.setConfirmationSentAt(sentAt);
    reservation.setConfirmationSummary(summary);
  }

  private void clearConfirmationNotice(WorkspaceReservation reservation) {
    reservation.setConfirmationChannel(null);
    reservation.setConfirmationDestination(null);
    reservation.setConfirmationSentAt(null);
    reservation.setConfirmationSummary(null);
  }

  private NotificationDispatchOutcome dispatchReservationConfirmation(
      WorkspaceReservation reservation, NotificationChannel resolvedChannel, String destination) {
    Enterprise enterprise = reservation.getEnterprise();
    WorkspaceReservationSlot slot =
        slotRepository.findById(reservation.getSlotId()).orElse(null);
    String smsMessage =
        "Reservation "
            + reservation.getCode()
            + " is confirmed for "
            + formatReservationWindow(reservation.getStartAt(), reservation.getEndAt())
            + ".";

    boolean emailDelivered = false;
    SmsDispatchResult smsDispatch = null;

    if ((resolvedChannel == NotificationChannel.email || resolvedChannel == NotificationChannel.both)
        && !isBlank(reservation.getContactEmail())) {
      emailDelivered =
          emailService.sendHtmlEmail(
          reservation.getContactEmail(),
          "Reservation confirmed - " + reservation.getCode(),
          buildConfirmationEmailBody(reservation, enterprise, slot));
    }

    if ((resolvedChannel == NotificationChannel.sms || resolvedChannel == NotificationChannel.both)
        && !isBlank(reservation.getContactPhone())) {
      smsDispatch =
          workspaceSmsService.sendReservationConfirmation(
              reservation.getContactPhone(), smsMessage);
    }

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("reservationId", reservation.getId());
    payload.put("reservationCode", reservation.getCode());
    payload.put("customer", reservation.getCustomer());
    payload.put("channel", resolvedChannel.name());
    payload.put("destination", destination);
    payload.put("emailDelivered", emailDelivered);
    payload.put("smsDelivered", smsDispatch != null && smsDispatch.success());
    payload.put("smsProvider", smsDispatch != null ? smsDispatch.provider() : "");
    payload.put("smsDetail", smsDispatch != null ? smsDispatch.detail() : "");

    User enterpriseUser = enterprise.getUser();
    if (enterpriseUser != null && enterpriseUser.getId() != null) {
      realtimeNotificationService.notifyUser(
          enterpriseUser.getId(),
          "WORKSPACE_RESERVATION_CONFIRMED",
          "Reservation " + reservation.getCode() + " has been confirmed.",
          payload);
    }
    realtimeNotificationService.notifyAdmin(
        "WORKSPACE_RESERVATION_CONFIRMED",
        enterprise.getCompanyName() + ": " + reservation.getCode() + " confirmed",
        payload);

    return new NotificationDispatchOutcome(emailDelivered, smsDispatch);
  }

  private String buildConfirmationEmailBody(
      WorkspaceReservation reservation, Enterprise enterprise, WorkspaceReservationSlot slot) {
    String slotName = slot != null ? slot.getName() : reservation.getSlotName();
    return """
        <html>
          <body style="font-family: Arial, sans-serif; color: #1f2937;">
            <h2 style="margin-bottom: 12px;">Reservation confirmed</h2>
            <p>Your reservation <strong>%s</strong> is now confirmed.</p>
            <table style="border-collapse: collapse; margin: 16px 0;">
              <tr><td style="padding: 6px 12px 6px 0;"><strong>Reference</strong></td><td>%s</td></tr>
              <tr><td style="padding: 6px 12px 6px 0;"><strong>Company</strong></td><td>%s</td></tr>
              <tr><td style="padding: 6px 12px 6px 0;"><strong>Site</strong></td><td>%s</td></tr>
              <tr><td style="padding: 6px 12px 6px 0;"><strong>Window</strong></td><td>%s</td></tr>
            </table>
            <p>Please keep this message as your service confirmation.</p>
          </body>
        </html>
        """
        .formatted(
            reservation.getTitle(),
            reservation.getCode(),
            enterprise.getCompanyName(),
            slotName,
            formatReservationWindow(reservation.getStartAt(), reservation.getEndAt()));
  }

  private NotificationChannel resolveEffectiveChannel(WorkspaceReservation reservation) {
    NotificationChannel requested = defaultNotificationChannel(reservation.getNotificationChannel());
    boolean hasEmail = !isBlank(reservation.getContactEmail());
    boolean hasPhone = !isBlank(reservation.getContactPhone());

    if (requested == NotificationChannel.both && (!hasEmail || !hasPhone)) {
      if (hasEmail) {
        return NotificationChannel.email;
      }
      if (hasPhone) {
        return NotificationChannel.sms;
      }
    }
    return requested;
  }

  private String resolveDestination(WorkspaceReservation reservation, NotificationChannel channel) {
    return switch (channel) {
      case email -> normalizeText(reservation.getContactEmail());
      case sms -> normalizeText(reservation.getContactPhone());
      case both ->
          normalizeText(reservation.getContactEmail())
              + " and "
              + normalizeText(reservation.getContactPhone());
    };
  }

  private String buildConfirmationSummary(
      NotificationChannel channel, String destination, NotificationDispatchOutcome dispatchOutcome) {
    boolean emailDelivered = dispatchOutcome.emailDelivered();
    boolean smsDelivered =
        dispatchOutcome.smsDispatch() != null && dispatchOutcome.smsDispatch().success();

    return switch (channel) {
      case email ->
          emailDelivered
              ? "Confirmation sent by email to " + destination + "."
              : "Email confirmation was prepared for " + destination + ", but delivery still needs SMTP access.";
      case sms ->
          smsDelivered
              ? "Confirmation sent by SMS to " + destination + "."
              : "SMS confirmation was prepared for "
                  + destination
                  + ", but delivery could not be confirmed yet.";
      case both ->
          "Email "
              + (emailDelivered ? "sent" : "prepared")
              + " and SMS "
              + (smsDelivered ? "sent" : "prepared")
              + " for "
              + destination
              + ".";
    };
  }

  private void propagateSlotSnapshotName(Enterprise enterprise, WorkspaceReservationSlot slot) {
    List<WorkspaceReservation> impacted =
        reservationRepository.findByEnterpriseIdAndSlotId(enterprise.getId(), slot.getId());
    impacted.forEach((reservation) -> reservation.setSlotName(slot.getName()));
    reservationRepository.saveAll(impacted);
  }

  private WorkspaceContext buildContext(Enterprise enterprise) {
    List<WorkspaceReservationSlot> slotEntities =
        slotRepository.findByEnterpriseIdOrderByNameAsc(enterprise.getId());
    List<WorkspaceReservation> reservationEntities =
        reservationRepository.findByEnterpriseIdOrderByStartAtAsc(enterprise.getId());
    List<WorkspaceOrder> orderEntities =
        orderRepository.findByEnterpriseIdOrderByCreatedAtDesc(enterprise.getId());
    WorkspaceMlService.WorkspaceMlAnalysis mlAnalysis =
        workspaceMlService.analyze(slotEntities, reservationEntities, orderEntities);

    Map<String, WorkspaceSlotResponse> slotById = new LinkedHashMap<>();
    for (WorkspaceReservationSlot slotEntity : slotEntities) {
      WorkspaceSlotResponse slotResponse =
          toSlotResponse(slotEntity, mlAnalysis.slotProfiles().get(slotEntity.getId()));
      slotById.put(slotResponse.id(), slotResponse);
    }

    Map<String, WorkspaceReservationResponse> reservationById = new LinkedHashMap<>();
    for (WorkspaceReservation reservationEntity : reservationEntities) {
      WorkspaceReservationResponse reservationResponse =
          toReservationResponse(
              reservationEntity,
              slotById,
              mlAnalysis.reservationProfiles().get(reservationEntity.getId()));
      reservationById.put(reservationResponse.id(), reservationResponse);
    }

    Map<String, WorkspaceOrderResponse> orderById = new LinkedHashMap<>();
    for (WorkspaceOrder orderEntity : orderEntities) {
      WorkspaceOrderResponse orderResponse =
          toOrderResponse(
              orderEntity,
              reservationById,
              mlAnalysis.orderProfiles().get(orderEntity.getId()));
      orderById.put(orderResponse.id(), orderResponse);
    }

    List<WorkspaceSlotResponse> slots = new ArrayList<>(slotById.values());
    List<WorkspaceReservationResponse> reservations = new ArrayList<>(reservationById.values());
    List<WorkspaceOrderResponse> orders = new ArrayList<>(orderById.values());
    List<ReservationConflictDto> conflicts = buildConflicts(reservations, slots);
    List<ReservationNotificationEventDto> notifications = buildReservationNotifications(reservations);
    WorkspaceSummaryDto summary = buildSummary(reservations, slots, orders, conflicts, enterprise.getCompanyName());
    List<InsightCardDto> reservationInsights =
        buildReservationInsights(reservations, slots, conflicts);
    List<InsightCardDto> slotInsights = buildSlotInsights(slots);
    List<InsightCardDto> orderInsights = buildOrderInsights(orders, enterprise.getCompanyName());
    List<ActivityCenterItemDto> activityFeed =
        buildActivityFeed(reservations, slots, orders, conflicts, notifications);

    return new WorkspaceContext(
        slotById,
        reservationById,
        orderById,
        slots,
        reservations,
        orders,
        conflicts,
        summary,
        reservationInsights,
        slotInsights,
        orderInsights,
        notifications,
        activityFeed);
  }

  private WorkspaceSlotResponse toSlotResponse(
      WorkspaceReservationSlot slot, WorkspaceMlService.SlotMlProfile mlProfile) {
    SlotStatus status = defaultSlotStatus(slot.getStatus());
    SlotPortfolio portfolio = defaultSlotPortfolio(slot.getPortfolio());
    SlotType type = slot.getType() == null ? SlotType.Storage : slot.getType();
    List<List<Integer>> heatmap = createHeatmapFromStatus(status, slot.getHeatmap());
    List<Integer> forecast = createForecastFromStatus(status, slot.getForecast());

    int utilizationRate =
        (int) Math.round(slot.getOccupied() * 100.0 / Math.max(slot.getCapacity(), 1));
    int predictedAvailability =
        mlProfile != null
            ? mlProfile.predictedAvailability()
            : Math.max(6, 100 - (int) Math.round(forecast.stream().mapToInt(Integer::intValue).average().orElse(0.0)));
    int underusedScore =
        mlProfile != null
            ? mlProfile.underusedScore()
            : Math.max(5, 100 - utilizationRate - (int) Math.round(predictedAvailability * 0.15));
    String demandTrend = mlProfile != null ? mlProfile.demandTrend() : "Demand steady";
    String segmentLabel = mlProfile != null ? mlProfile.segmentLabel() : "Balanced network node";

    String recoveryAction = "Keep the current pace and watch Wed around midday.";
    if ("Recovery pocket".equals(segmentLabel)) {
      recoveryAction =
          "Lift "
              + slot.getName()
              + " in discovery this week and use "
              + demandTrend.toLowerCase(Locale.ROOT)
              + " hours as the pitch window.";
    } else if ("High-pressure node".equals(segmentLabel)) {
      recoveryAction =
          "Protect "
              + slot.getName()
              + " from low-value traffic and keep overflow options ready while "
              + demandTrend.toLowerCase(Locale.ROOT)
              + ".";
    } else if (portfolio == SlotPortfolio.partner) {
      recoveryAction =
          "Keep "
              + slot.getName()
              + " positioned as a trusted outside option with "
              + demandTrend.toLowerCase(Locale.ROOT)
              + " across the next cycle.";
    }

    String quietWindow = describeBestWindow(heatmap, slot.getName());
    String bestUseMode =
        switch (type) {
          case Dock -> "Best for short loading windows, export dispatch, and overflow truck flow.";
          case Storage -> "Best for buffer stock, transit pallets, and quick release inventory.";
          case Meeting ->
              "Best for partner reviews, commercial alignment, and light coordination sessions.";
          case Production ->
              "Best for overflow production runs, pilot batches, and urgent finishing work.";
        };
    String spotlightMessage =
        "Recovery pocket".equals(segmentLabel)
            ? "Open "
                + slot.getName()
                + " as a fast response option for "
                + firstNonBlank(slot.getCity(), "Tunis")
                + " demand while "
                + demandTrend.toLowerCase(Locale.ROOT)
                + "."
            : "High-pressure node".equals(segmentLabel)
                ? "Frame "
                    + slot.getName()
                    + " as a premium controlled window and keep it away from low-margin use."
                : portfolio == SlotPortfolio.partner
                    ? "Keep "
                        + slot.getName()
                        + " visible as a trusted outside option when your own network tightens."
                    : "Present "
                        + slot.getName()
                        + " as a stable "
                        + segmentLabel.toLowerCase(Locale.ROOT)
                        + " with refreshed equipment details.";

    List<String> activationChecklist =
        List.of(
            utilizationRate < 50
                ? "Publish a short availability highlight for the week."
                : "Reserve the strongest-fit hours for the highest-value requests.",
            status == SlotStatus.maintenance
                ? "Block low-priority demand until maintenance is cleared."
                : "Check staffing around " + quietWindow + " while " + demandTrend.toLowerCase(Locale.ROOT) + ".",
            portfolio == SlotPortfolio.partner
                ? "Refresh partner contact details and access instructions."
                : "Refresh site photos, equipment notes, and gate guidance.");

    String ecoFitNote =
        "Filling this site closer to "
            + firstNonBlank(slot.getCity(), "Tunis")
            + " demand can reduce empty transfers and make better use of existing shared capacity.";

    return new WorkspaceSlotResponse(
        slot.getId(),
        slot.getName(),
        slot.getZone(),
        firstNonBlank(slot.getCity(), enterpriseCity(slot.getEnterprise()), "Tunis"),
        firstNonBlank(slot.getOwnerCompany(), slot.getEnterprise().getCompanyName()),
        portfolio,
        type,
        safeDoubleList(slot.getCoordinates()),
        slot.getCapacity(),
        slot.getOccupied(),
        status,
        safeStringList(slot.getEquipment()),
        heatmap,
        forecast,
        utilizationRate,
        predictedAvailability,
        underusedScore,
        recoveryAction,
        quietWindow,
        bestUseMode,
        spotlightMessage,
        activationChecklist,
        ecoFitNote);
  }

  private WorkspaceReservationResponse toReservationResponse(
      WorkspaceReservation reservation,
      Map<String, WorkspaceSlotResponse> slotsById,
      WorkspaceMlService.ReservationMlProfile mlProfile) {
    WorkspaceSlotResponse slot = slotsById.get(reservation.getSlotId());
    ReservationRole role = defaultReservationRole(reservation.getRole());
    ReservationStatus status = defaultReservationStatus(reservation.getStatus());
    ReservationCategory category = defaultReservationCategory(reservation.getCategory(), slot);
    String enterpriseName = reservation.getEnterprise().getCompanyName();
    String city =
        firstNonBlank(
            reservation.getCity(),
            slot != null ? slot.city() : null,
            enterpriseCity(reservation.getEnterprise()),
            "Tunis");
    String providerCompany = role == ReservationRole.provider ? enterpriseName : reservation.getCustomer();
    String consumerCompany = role == ReservationRole.consumer ? enterpriseName : reservation.getCustomer();

    long durationHours =
        Math.max(
            1L,
            Math.round(
                Duration.between(reservation.getStartAt(), reservation.getEndAt()).toMinutes() / 60.0));
    long leadHours = Math.max(0L, Duration.between(LocalDateTime.now(), reservation.getStartAt()).toHours());
    WorkspaceMlService.ReservationMlProfile resolvedProfile =
        mlProfile != null
            ? mlProfile
            : new WorkspaceMlService.ReservationMlProfile(
                42, 64, 0.42, List.of("Heavy slot pressure", "Tight lead time"));
    int cancellationRisk = resolvedProfile.cancellationRisk();
    int readinessScore = resolvedProfile.readinessScore();
    List<String> topDrivers =
        resolvedProfile.topDrivers().isEmpty()
            ? List.of("Heavy slot pressure")
            : resolvedProfile.topDrivers();
    String primaryDriver = topDrivers.get(0).toLowerCase(Locale.ROOT);
    String secondaryDriver =
        topDrivers.size() > 1 ? topDrivers.get(1).toLowerCase(Locale.ROOT) : primaryDriver;

    String recommendedTime = getBestWindowForSlot(reservation.getSlotId(), slotsById);
    String color =
        switch (status) {
          case confirmed -> "#0284c7";
          case pending -> "#38bdf8";
          case risk -> "#f97316";
          case cancelled -> "#94a3b8";
        };

    String bookingSuggestion =
        cancellationRisk >= 65
            ? "Move this booking to "
                + recommendedTime
                + " to reduce "
                + primaryDriver
                + " around "
                + (slot != null ? slot.name() : "the site")
                + "."
            : role == ReservationRole.provider
                ? "This window looks steady. Keep the arrival coordinated and stay alert to "
                    + primaryDriver
                    + " while confirming with "
                    + reservation.getCustomer()
                    + "."
                : "This window stays comfortable. Confirm attendance and keep a fallback around "
                    + secondaryDriver
                    + ".";

    String serviceBrief =
        role == ReservationRole.provider
            ? "Host "
                + reservation.getCustomer()
                + " at "
                + (slot != null ? slot.name() : "the site")
                + " for a "
                + durationHours
                + "-hour "
                + category.name().toLowerCase(Locale.ROOT)
                + " service window with the main watch on "
                + primaryDriver
                + "."
            : "Use "
                + (slot != null ? slot.name() : "the selected site")
                + " from "
                + providerCompany
                + " for a "
                + durationHours
                + "-hour "
                + category.name().toLowerCase(Locale.ROOT)
                + " mission while keeping "
                + primaryDriver
                + " under control.";

    List<String> coordinationChecklist =
        List.of(
                status == ReservationStatus.confirmed
                    ? "Share the confirmed service window with the operating team."
                    : "Confirm the service window with the counterparty before the day starts.",
                category == ReservationCategory.Machine || category == ReservationCategory.Production
                    ? "Check technical readiness and handover conditions before arrival."
                    : category == ReservationCategory.Storage
                        ? "Prepare intake labels and release flow for stored goods."
                        : "Prepare reception flow, badges, and access guidance for the visit.",
                "Watch " + primaryDriver + " and " + secondaryDriver + " during the final confirmation.",
                reservation.getHeadcount() >= 8
                    ? "Assign one arrival lead for the larger team and keep the contact person reachable."
                    : "Keep the named contact reachable during the arrival window.",
                leadHours < 48
                    ? "Keep one fallback time window ready in case the schedule shifts."
                    : "Send a reminder on the day before the service.")
            .subList(0, 3);

    String messageDraft =
        "Hello "
            + firstNonBlank(reservation.getContactName(), reservation.getCustomer())
            + ", your "
            + reservation.getTitle().toLowerCase(Locale.ROOT)
            + " is planned for "
            + formatReservationWindow(reservation.getStartAt(), reservation.getEndAt())
            + " at "
            + (slot != null ? slot.name() : city)
            + ". "
            + (role == ReservationRole.provider
                ? "Our team will prepare the site and access flow. "
                : "Our team will arrive with the agreed service needs. ")
            + "Please let us know if timing, headcount, or access conditions change, especially around "
            + primaryDriver
            + ".";

    String ecoNote =
        "Keeping this "
            + category.name().toLowerCase(Locale.ROOT)
            + " service in "
            + city
            + " helps make better use of shared capacity and limits one-off movement across the network.";

    return new WorkspaceReservationResponse(
        reservation.getId(),
        reservation.getCode(),
        reservation.getTitle(),
        reservation.getCustomer(),
        reservation.getResource(),
        reservation.getSlotId(),
        slot != null ? slot.name() : reservation.getSlotName(),
        role,
        city,
        category,
        providerCompany,
        consumerCompany,
        reservation.getStartAt(),
        reservation.getEndAt(),
        reservation.getHeadcount(),
        scale(reservation.getAmount()),
        status,
        normalizeText(reservation.getNotes()),
        safeStringList(reservation.getTags()),
        normalizeText(reservation.getContactName()),
        normalizeText(reservation.getContactEmail()),
        normalizeText(reservation.getContactPhone()),
        defaultNotificationChannel(reservation.getNotificationChannel()),
        toConfirmationNotice(reservation),
        color,
        cancellationRisk,
        recommendedTime,
        bookingSuggestion,
        readinessScore,
        serviceBrief,
        coordinationChecklist,
        messageDraft,
        ecoNote);
  }

  private WorkspaceOrderResponse toOrderResponse(
      WorkspaceOrder order,
      Map<String, WorkspaceReservationResponse> reservationsById,
      WorkspaceMlService.OrderMlProfile mlProfile) {
    WorkspaceReservationResponse linkedReservation = reservationsById.get(order.getReservationId());
    ReservationRole role =
        order.getRole() != null
            ? defaultReservationRole(order.getRole())
            : linkedReservation != null ? linkedReservation.role() : ReservationRole.provider;
    String enterpriseName = order.getEnterprise().getCompanyName();
    String buyerCompany = role == ReservationRole.consumer ? enterpriseName : order.getCustomer();
    String sellerCompany = role == ReservationRole.provider ? enterpriseName : order.getCustomer();

    List<OrderLineItemDto> items =
        order.getItems().stream()
            .map(item -> new OrderLineItemDto(item.label(), item.quantity(), scale(item.unitPrice())))
            .toList();

    BigDecimal subtotal = computeItemsSubtotal(order.getItems());
    BigDecimal resolvedAmount = subtotal.signum() > 0 ? subtotal : scale(order.getAmount());
    BigDecimal total = scale(resolvedAmount.add(scale(order.getTax())));
    PaymentStatus paymentStatus = defaultPaymentStatus(order.getPaymentStatus());
    OrderStatus status = defaultOrderStatus(order.getStatus());
    WorkspaceMlService.OrderMlProfile resolvedProfile =
        mlProfile != null
            ? mlProfile
            : new WorkspaceMlService.OrderMlProfile(
                38, "Balanced commercial flow", 0.38, List.of("Large settlement amount"));
    int fraudRisk = resolvedProfile.fraudRisk();
    String spendingCluster = firstNonBlank(resolvedProfile.spendingCluster(), "Balanced commercial flow");
    List<String> topDrivers =
        resolvedProfile.topDrivers().isEmpty()
            ? List.of("Large settlement amount")
            : resolvedProfile.topDrivers();
    String primaryDriver = topDrivers.get(0).toLowerCase(Locale.ROOT);
    String secondaryDriver =
        topDrivers.size() > 1 ? topDrivers.get(1).toLowerCase(Locale.ROOT) : primaryDriver;
    long daysToDue =
        order.getDueDate() != null ? ChronoUnit.DAYS.between(LocalDate.now(), order.getDueDate()) : 7L;

    String paymentInsight =
        fraudRisk >= 70
            ? "Keep manual validation before final release and clear " + primaryDriver + " with the counterparty."
            : fraudRisk >= 45
                ? "Watch " + primaryDriver + " and keep a checkpoint before the next step."
                : "Regular profile. The file can move forward with a light review while tracking "
                    + secondaryDriver
                    + ".";
    String settlementPriority =
        fraudRisk >= 65 || paymentStatus == PaymentStatus.review
            ? "High priority"
            : paymentStatus == PaymentStatus.pending || daysToDue <= 5 ? "Balanced follow-up" : "Routine";
    String nextBestAction =
        paymentStatus == PaymentStatus.review
            ? "Call the finance contact and confirm the supporting documents tied to " + primaryDriver + "."
            : status == OrderStatus.draft
                ? "Finalize the line items and release the invoice for validation."
                : paymentStatus == PaymentStatus.pending && role == ReservationRole.provider
                    ? "Send a payment reminder for "
                        + order.getInvoiceNumber()
                        + " and attach the invoice copy with a note on "
                        + secondaryDriver
                        + "."
                    : paymentStatus == PaymentStatus.pending
                        ? "Prepare the payment release pack and confirm the due date while checking "
                            + primaryDriver
                            + "."
                        : "Archive the closed file and keep it as a reference for repeat work.";
    String followUpDraft =
        role == ReservationRole.provider
            ? "Hello "
                + order.getCustomer()
                + ", this is a follow-up on invoice "
                + order.getInvoiceNumber()
                + " for "
                + total.setScale(2, RoundingMode.HALF_UP).toPlainString()
                + " TND due on "
                + order.getDueDate()
                + ". Please let us know if you need any supporting document to complete payment."
            : "Hello "
                + order.getCustomer()
                + ", we are preparing payment for invoice "
                + order.getInvoiceNumber()
                + " due on "
                + order.getDueDate()
                + ". Please confirm that the final document set is complete so the release can move forward.";

    List<String> financeChecklist =
        List.of(
                "Match the order total with the linked booking and service scope.",
                "Review " + primaryDriver + " before the file moves to the next finance stage.",
                scale(order.getTax()).compareTo(BigDecimal.ZERO) > 0
                    ? "Check the tax line and confirm the final total before release."
                    : "Confirm that the file does not require an additional tax adjustment.",
                "Attach the QR code reference to the finance file.",
                paymentStatus == PaymentStatus.review
                    ? "Keep written approval before moving the file."
                    : "Check the due date and reminder plan.")
            .subList(0, 3);

    String relationshipTone =
        fraudRisk >= 65
            ? "Keep the tone careful, precise, and document-led."
            : role == ReservationRole.provider
                ? "Keep the tone warm and commercially steady while acknowledging " + secondaryDriver + "."
                : "Keep the tone cooperative and timeline-focused.";

    return new WorkspaceOrderResponse(
        order.getId(),
        order.getCode(),
        order.getInvoiceNumber(),
        order.getCustomer(),
        order.getReservationId(),
        order.getSlotId(),
        role,
        firstNonBlank(order.getCity(), linkedReservation != null ? linkedReservation.city() : null, "Tunis"),
        buyerCompany,
        sellerCompany,
        resolvedAmount,
        scale(order.getTax()),
        total,
        order.getCreatedAt(),
        order.getDueDate(),
        status,
        paymentStatus,
        items,
        "eco://orders/" + order.getId() + "/invoice/" + order.getInvoiceNumber(),
        buildTracking(status, order.getCreatedAt()),
        fraudRisk,
        spendingCluster,
        paymentInsight,
        settlementPriority,
        nextBestAction,
        followUpDraft,
        financeChecklist,
        relationshipTone);
  }

  private ReservationConfirmationNoticeDto toConfirmationNotice(WorkspaceReservation reservation) {
    if (reservation.getConfirmationSentAt() == null
        || reservation.getConfirmationChannel() == null
        || isBlank(reservation.getConfirmationDestination())) {
      return null;
    }

    return new ReservationConfirmationNoticeDto(
        reservation.getConfirmationChannel(),
        normalizeText(reservation.getConfirmationDestination()),
        reservation.getConfirmationSentAt(),
        normalizeText(reservation.getConfirmationSummary()));
  }

  private List<TrackingStepDto> buildTracking(OrderStatus status, LocalDate createdAt) {
    List<String> steps = List.of("Order created", "Internal review", "Invoice ready", "File closed");
    Map<OrderStatus, Integer> completionMap =
        Map.of(
            OrderStatus.draft, 1,
            OrderStatus.processing, 2,
            OrderStatus.invoiced, 3,
            OrderStatus.fulfilled, 4,
            OrderStatus.flagged, 2);

    int completed = completionMap.getOrDefault(status, 1);
    List<String> timestamps = List.of("08:30", "10:15", "13:40", "17:10");

    List<TrackingStepDto> tracking = new ArrayList<>();
    for (int index = 0; index < steps.size(); index += 1) {
      boolean done = index < completed;
      tracking.add(
          new TrackingStepDto(
              steps.get(index),
              done ? createdAt + " " + timestamps.get(index) : "Waiting",
              done));
    }
    return tracking;
  }

  private List<ReservationConflictDto> buildConflicts(
      List<WorkspaceReservationResponse> reservations, List<WorkspaceSlotResponse> slots) {
    Map<String, WorkspaceSlotResponse> slotById =
        slots.stream().collect(Collectors.toMap(WorkspaceSlotResponse::id, slot -> slot));
    List<ReservationConflictDto> conflicts = new ArrayList<>();

    for (int index = 0; index < reservations.size(); index += 1) {
      for (int compareIndex = index + 1; compareIndex < reservations.size(); compareIndex += 1) {
        WorkspaceReservationResponse left = reservations.get(index);
        WorkspaceReservationResponse right = reservations.get(compareIndex);

        boolean overlaps =
            Objects.equals(left.slotId(), right.slotId())
                && left.status() != ReservationStatus.cancelled
                && right.status() != ReservationStatus.cancelled
                && left.start().isBefore(right.end())
                && right.start().isBefore(left.end());

        if (!overlaps) {
          continue;
        }

        WorkspaceSlotResponse slot = slotById.get(left.slotId());
        String severity =
            Math.abs(Duration.between(left.start(), right.start()).toHours()) < 2 ? "high" : "medium";

        conflicts.add(
            new ReservationConflictDto(
                List.of(left.id(), right.id()),
                left.slotId(),
                slot != null ? slot.name() : left.slotName(),
                left.code() + " overlaps " + right.code(),
                severity));
      }
    }

    return conflicts;
  }

  private List<ReservationNotificationEventDto> buildReservationNotifications(
      List<WorkspaceReservationResponse> reservations) {
    return reservations.stream()
        .filter(reservation -> reservation.confirmationNotice() != null)
        .sorted(
            Comparator.comparing(
                    (WorkspaceReservationResponse reservation) -> reservation.confirmationNotice().sentAt())
                .reversed())
        .limit(8)
        .map(
            reservation ->
                new ReservationNotificationEventDto(
                    reservation.id(),
                    reservation.code(),
                    reservation.customer(),
                    reservation.confirmationNotice().channel(),
                    reservation.confirmationNotice().destination(),
                    reservation.confirmationNotice().sentAt(),
                    reservation.confirmationNotice().summary()))
        .toList();
  }

  private WorkspaceSummaryDto buildSummary(
      List<WorkspaceReservationResponse> reservations,
      List<WorkspaceSlotResponse> slots,
      List<WorkspaceOrderResponse> orders,
      List<ReservationConflictDto> conflicts,
      String enterpriseName) {
    int hostedReservations =
        (int)
            reservations.stream()
                .filter(
                    reservation ->
                        reservation.role() == ReservationRole.provider
                            && reservation.status() != ReservationStatus.cancelled)
                .count();
    int requestedReservations =
        (int)
            reservations.stream()
                .filter(
                    reservation ->
                        reservation.role() == ReservationRole.consumer
                            && reservation.status() != ReservationStatus.cancelled)
                .count();
    BigDecimal receivableTotal =
        orders.stream()
            .filter(
                order ->
                    Objects.equals(order.sellerCompany(), enterpriseName)
                        && order.paymentStatus() != PaymentStatus.paid)
            .map(WorkspaceOrderResponse::total)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal payableTotal =
        orders.stream()
            .filter(
                order ->
                    Objects.equals(order.buyerCompany(), enterpriseName)
                        && order.paymentStatus() != PaymentStatus.paid)
            .map(WorkspaceOrderResponse::total)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    int occupancySignal =
        slots.isEmpty()
            ? 0
            : (int) Math.round(slots.stream().mapToInt(WorkspaceSlotResponse::utilizationRate).average().orElse(0.0));
    int flaggedOrders =
        (int)
            orders.stream()
                .filter(order -> order.fraudRisk() >= 65 || order.paymentStatus() == PaymentStatus.review)
                .count();
    WorkspaceSlotResponse bestSlot =
        slots.stream()
            .sorted(
                Comparator.comparing(
                        (WorkspaceSlotResponse slot) -> slot.portfolio() == SlotPortfolio.owned ? 0 : 1)
                    .thenComparing(Comparator.comparingInt(WorkspaceSlotResponse::underusedScore).reversed()))
            .findFirst()
            .orElse(null);

    return new WorkspaceSummaryDto(
        hostedReservations,
        requestedReservations,
        scale(receivableTotal),
        scale(payableTotal),
        conflicts.size(),
        occupancySignal,
        flaggedOrders,
        bestSlot != null
            ? getBestWindowForSlot(
                bestSlot.id(),
                slots.stream().collect(Collectors.toMap(WorkspaceSlotResponse::id, slot -> slot)))
            : "No suggestion available");
  }

  private List<ActivityCenterItemDto> buildActivityFeed(
      List<WorkspaceReservationResponse> reservations,
      List<WorkspaceSlotResponse> slots,
      List<WorkspaceOrderResponse> orders,
      List<ReservationConflictDto> conflicts,
      List<ReservationNotificationEventDto> notifications) {
    List<ActivityCenterItemDto> items = new ArrayList<>();

    notifications.stream().limit(3).forEach(
        notification ->
            items.add(
                new ActivityCenterItemDto(
                    "notice-" + notification.reservationId(),
                    "both",
                    "reservation",
                    InsightTone.success,
                    "Confirmation",
                    notification.reservationCode() + " is confirmed",
                    notification.summary(),
                    notification.sentAt(),
                    "/enterprise/my-reservations",
                    "Open bookings")));

    WorkspaceReservationResponse pendingReservation =
        reservations.stream()
            .filter(reservation -> reservation.status() == ReservationStatus.pending)
            .min(Comparator.comparing(WorkspaceReservationResponse::start))
            .orElse(null);
    if (pendingReservation != null) {
      items.add(
          new ActivityCenterItemDto(
              "pending-" + pendingReservation.id(),
              "both",
              "reservation",
              InsightTone.info,
              pendingReservation.role() == ReservationRole.provider ? "Host side" : "Request side",
              pendingReservation.code() + " is waiting for approval",
              firstNonBlank(
                  pendingReservation.serviceBrief(),
                  "Review the service window for " + pendingReservation.customer() + " and decide whether to confirm it now."),
              normalizeOccurredAt(pendingReservation.start()),
              "/enterprise/my-reservations",
              "Review booking"));
    }

    WorkspaceReservationResponse highRiskReservation =
        reservations.stream()
            .filter(reservation -> reservation.cancellationRisk() >= 60)
            .max(Comparator.comparingInt(WorkspaceReservationResponse::cancellationRisk))
            .orElse(null);
    if (highRiskReservation != null) {
      items.add(
          new ActivityCenterItemDto(
              "risk-" + highRiskReservation.id(),
              "both",
              "reservation",
              highRiskReservation.cancellationRisk() >= 75 ? InsightTone.danger : InsightTone.warning,
              highRiskReservation.cancellationRisk() + "% watch",
              highRiskReservation.code() + " needs coordination",
              firstNonBlank(
                  firstListItem(highRiskReservation.coordinationChecklist()),
                  highRiskReservation.bookingSuggestion()),
              normalizeOccurredAt(highRiskReservation.start()),
              "/enterprise/my-reservations",
              "Open booking"));
    }

    for (int index = 0; index < Math.min(conflicts.size(), 2); index += 1) {
      ReservationConflictDto conflict = conflicts.get(index);
      WorkspaceReservationResponse relatedReservation =
          reservations.stream()
              .filter(reservation -> reservation.id().equals(conflict.reservationIds().get(0)))
              .findFirst()
              .orElse(null);

      items.add(
          new ActivityCenterItemDto(
              "conflict-" + index + "-" + conflict.slotId(),
              "both",
              "system",
              "high".equals(conflict.severity()) ? InsightTone.danger : InsightTone.warning,
              "Timing alert",
              conflict.overlapLabel(),
              conflict.slotName() + " needs a timing shift or a fallback route before the service day starts.",
              normalizeOccurredAt(relatedReservation != null ? relatedReservation.start() : null),
              "/enterprise/my-reservations",
              "Check planning"));
    }

    WorkspaceSlotResponse underusedSlot =
        slots.stream()
            .filter(slot -> slot.underusedScore() >= 55)
            .max(Comparator.comparingInt(WorkspaceSlotResponse::underusedScore))
            .orElse(null);
    if (underusedSlot != null) {
      items.add(
          new ActivityCenterItemDto(
              "slot-opportunity-" + underusedSlot.id(),
              "both",
              "slot",
              InsightTone.success,
              "Opportunity",
              underusedSlot.name() + " can take more activity",
              firstNonBlank(
                  underusedSlot.spotlightMessage(),
                  underusedSlot.name() + " is ready to absorb more requests this week."),
              nowAtHour(9),
              "/enterprise/my-reservations/slots",
              "Open spaces"));
    }

    WorkspaceSlotResponse busySlot =
        slots.stream()
            .filter(slot -> slot.utilizationRate() >= 75)
            .max(Comparator.comparingInt(WorkspaceSlotResponse::utilizationRate))
            .orElse(null);
    if (busySlot != null) {
      items.add(
          new ActivityCenterItemDto(
              "slot-protect-" + busySlot.id(),
              "both",
              "slot",
              busySlot.utilizationRate() >= 86 ? InsightTone.danger : InsightTone.warning,
              busySlot.utilizationRate() + "% occupied",
              busySlot.name() + " should be protected",
              firstNonBlank(
                  firstListItem(busySlot.activationChecklist()),
                  "Keep the strongest-fit hours on " + busySlot.name() + " for the most valuable requests."),
              nowAtHour(8),
              "/enterprise/my-reservations/slots",
              "Review space"));
    }

    WorkspaceOrderResponse flaggedOrder =
        orders.stream()
            .filter(order -> order.fraudRisk() >= 65 || order.paymentStatus() == PaymentStatus.review)
            .max(Comparator.comparingInt(WorkspaceOrderResponse::fraudRisk))
            .orElse(null);
    if (flaggedOrder != null) {
      items.add(
          new ActivityCenterItemDto(
              "order-review-" + flaggedOrder.id(),
              "both",
              "order",
              flaggedOrder.fraudRisk() >= 75 ? InsightTone.danger : InsightTone.warning,
              flaggedOrder.fraudRisk() + "% review",
              flaggedOrder.invoiceNumber() + " needs a finance check",
              firstNonBlank(
                  flaggedOrder.nextBestAction(),
                  "Confirm the supporting documents before the file moves to the next step."),
              normalizeOccurredAt(flaggedOrder.createdAt()),
              "/enterprise/my-reservations/orders",
              "Open orders"));
    }

    WorkspaceOrderResponse receivableOrder =
        orders.stream()
            .filter(
                order ->
                    order.role() == ReservationRole.provider
                        && order.paymentStatus() == PaymentStatus.pending)
            .max(Comparator.comparing(WorkspaceOrderResponse::total))
            .orElse(null);
    if (receivableOrder != null) {
      items.add(
          new ActivityCenterItemDto(
              "order-collect-" + receivableOrder.id(),
              "both",
              "order",
              InsightTone.info,
              "Collection",
              receivableOrder.invoiceNumber() + " is ready for follow-up",
              firstNonBlank(
                  receivableOrder.followUpDraft(),
                  "Follow up with " + receivableOrder.customer() + " on the pending receivable."),
              normalizeOccurredAt(receivableOrder.dueDate()),
              "/enterprise/my-reservations/orders",
              "Collect payment"));
    }

    WorkspaceOrderResponse payableOrder =
        orders.stream()
            .filter(
                order ->
                    order.role() == ReservationRole.consumer
                        && order.paymentStatus() == PaymentStatus.pending)
            .max(Comparator.comparing(WorkspaceOrderResponse::total))
            .orElse(null);
    if (payableOrder != null) {
      items.add(
          new ActivityCenterItemDto(
              "order-pay-" + payableOrder.id(),
              "admin",
              "order",
              InsightTone.info,
              "Payable",
              payableOrder.invoiceNumber() + " is nearing release",
              firstNonBlank(
                  firstListItem(payableOrder.financeChecklist()),
                  "Prepare the payment release pack and confirm the finance timeline."),
              normalizeOccurredAt(payableOrder.dueDate()),
              "/enterprise/my-reservations/orders",
              "Open orders"));
    }

    if (items.isEmpty()) {
      items.add(
          new ActivityCenterItemDto(
              "system-steady",
              "both",
              "system",
              InsightTone.success,
              "Stable",
              "The operational flow is calm",
              "No urgent booking, space, or order issue is currently asking for intervention.",
              nowAtHour(10),
              null,
              null));
    }

    return items.stream().limit(9).toList();
  }

  private List<InsightCardDto> buildReservationInsights(
      List<WorkspaceReservationResponse> reservations,
      List<WorkspaceSlotResponse> slots,
      List<ReservationConflictDto> conflicts) {
    WorkspaceReservationResponse highRisk =
        reservations.stream()
            .filter(reservation -> reservation.cancellationRisk() >= 60)
            .max(Comparator.comparingInt(WorkspaceReservationResponse::cancellationRisk))
            .orElse(null);
    int incoming =
        (int)
            reservations.stream()
                .filter(reservation -> reservation.role() == ReservationRole.provider)
                .count();
    int outgoing =
        (int)
            reservations.stream()
                .filter(reservation -> reservation.role() == ReservationRole.consumer)
                .count();
    WorkspaceSlotResponse reliefSlot =
        slots.stream()
            .filter(slot -> slot.underusedScore() >= 55)
            .max(Comparator.comparingInt(WorkspaceSlotResponse::underusedScore))
            .orElse(null);

    return List.of(
        new InsightCardDto(
            highRisk != null ? highRisk.code() + " needs a quick callback" : "The schedule stays under control",
            highRisk != null
                ? highRisk.customer() + " shows the highest watch level. " + highRisk.bookingSuggestion()
                : "No reservation crosses the main watch threshold.",
            highRisk != null ? highRisk.cancellationRisk() + "% watch" : "Stable",
            highRisk != null ? InsightTone.danger : InsightTone.success),
        new InsightCardDto(
            "Your activity is split between "
                + incoming
                + " hosted booking"
                + (incoming > 1 ? "s" : "")
                + " and "
                + outgoing
                + " requested booking"
                + (outgoing > 1 ? "s" : ""),
            incoming >= outgoing
                ? "The host role leads this week. Keep the focus on welcome quality and billing."
                : "The buyer role is rising. Secure outside slots before the end of the week.",
            incoming >= outgoing ? "Host side" : "Request side",
            incoming >= outgoing ? InsightTone.info : InsightTone.warning),
        new InsightCardDto(
            reliefSlot != null ? reliefSlot.name() + " remains your best relief site" : "Every site is already well balanced",
            reliefSlot != null
                ? reliefSlot.recoveryAction() + " Expected availability stays at " + reliefSlot.predictedAvailability() + "%."
                : "No site clearly stands out as a relief zone.",
            reliefSlot != null ? reliefSlot.city() : "Balanced",
            reliefSlot != null ? InsightTone.success : InsightTone.info),
        new InsightCardDto(
            conflicts.isEmpty()
                ? "No overlaps to report"
                : conflicts.size() + " overlap" + (conflicts.size() > 1 ? "s" : "") + " to handle",
            conflicts.isEmpty()
                ? "The calendar matches the published slots."
                : "Most urgent: " + conflicts.get(0).overlapLabel() + " on " + conflicts.get(0).slotName() + ".",
            conflicts.isEmpty() ? "Net" : "Planning",
            conflicts.isEmpty() ? InsightTone.info : InsightTone.warning));
  }

  private List<InsightCardDto> buildSlotInsights(List<WorkspaceSlotResponse> slots) {
    WorkspaceSlotResponse underused =
        slots.stream().max(Comparator.comparingInt(WorkspaceSlotResponse::underusedScore)).orElse(null);
    WorkspaceSlotResponse peak =
        slots.stream().max(Comparator.comparingInt(WorkspaceSlotResponse::utilizationRate)).orElse(null);

    return List.of(
        new InsightCardDto(
            underused != null ? underused.name() + " can absorb more volume" : "Load is well distributed",
            underused != null
                ? underused.recoveryAction() + " Estimated free window remains " + underused.predictedAvailability() + "%."
                : "No site needs special action today.",
            underused != null
                ? underused.portfolio() == SlotPortfolio.owned ? "Owned" : "Partner"
                : "Stable",
            underused != null ? InsightTone.success : InsightTone.info),
        new InsightCardDto(
            peak != null ? peak.name() + " is nearing its limit" : "No strong tension",
            peak != null
                ? peak.utilizationRate() + "% occupied now with " + peak.predictedAvailability() + "% estimated availability."
                : "All sites remain under the alert threshold.",
            peak != null ? peak.utilizationRate() + "%" : "Calm",
            peak != null && peak.utilizationRate() > 75 ? InsightTone.warning : InsightTone.info));
  }

  private List<InsightCardDto> buildOrderInsights(
      List<WorkspaceOrderResponse> orders, String enterpriseName) {
    WorkspaceOrderResponse flagged =
        orders.stream()
            .filter(order -> order.fraudRisk() >= 65 || order.paymentStatus() == PaymentStatus.review)
            .max(Comparator.comparingInt(WorkspaceOrderResponse::fraudRisk))
            .orElse(null);
    BigDecimal toCollect =
        orders.stream()
            .filter(
                order ->
                    Objects.equals(order.sellerCompany(), enterpriseName)
                        && order.paymentStatus() != PaymentStatus.paid)
            .map(WorkspaceOrderResponse::total)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal toPay =
        orders.stream()
            .filter(
                order ->
                    Objects.equals(order.buyerCompany(), enterpriseName)
                        && order.paymentStatus() != PaymentStatus.paid)
            .map(WorkspaceOrderResponse::total)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    return List.of(
        new InsightCardDto(
            flagged != null ? flagged.invoiceNumber() + " needs review" : "Orders are moving at a healthy pace",
            flagged != null
                ? flagged.customer() + " remains the most sensitive case. " + flagged.paymentInsight()
                : "No order crosses the reinforced manual review threshold.",
            flagged != null ? flagged.fraudRisk() + "% watch" : "Smooth",
            flagged != null ? InsightTone.danger : InsightTone.success),
        new InsightCardDto(
            "The balance between collections and payments stays clear",
            scale(toCollect).toPlainString()
                + " TND still need to be collected while "
                + scale(toPay).toPlainString()
                + " TND still need to be paid on the buying side.",
            toCollect.compareTo(toPay) >= 0 ? "Collections ahead" : "Purchases ahead",
            toCollect.compareTo(toPay) >= 0 ? InsightTone.success : InsightTone.warning));
  }

  private String getBestWindowForSlot(String slotId, Map<String, WorkspaceSlotResponse> slotsById) {
    WorkspaceSlotResponse slot = slotsById.get(slotId);
    if (slot == null || slot.heatmap().isEmpty()) {
      return "Check weekly availability";
    }
    return describeBestWindow(slot.heatmap(), slot.name());
  }

  private String describeBestWindow(List<List<Integer>> heatmap, String slotName) {
    int bestValue = Integer.MAX_VALUE;
    int bestDay = 0;
    int bestHour = 0;

    for (int dayIndex = 0; dayIndex < heatmap.size(); dayIndex += 1) {
      List<Integer> row = heatmap.get(dayIndex);
      for (int hourIndex = 0; hourIndex < row.size(); hourIndex += 1) {
        int value = row.get(hourIndex);
        if (value < bestValue) {
          bestValue = value;
          bestDay = dayIndex;
          bestHour = hourIndex;
        }
      }
    }

    return HEATMAP_DAYS.get(Math.min(bestDay, HEATMAP_DAYS.size() - 1))
        + " "
        + HEATMAP_HOURS.get(Math.min(bestHour, HEATMAP_HOURS.size() - 1))
        + " - "
        + slotName;
  }

  private String formatReservationWindow(LocalDateTime start, LocalDateTime end) {
    return start.format(WINDOW_FORMAT) + " to " + end.format(WINDOW_FORMAT);
  }

  private LocalDateTime normalizeOccurredAt(LocalDateTime value) {
    return value == null ? nowAtHour(9) : value;
  }

  private LocalDateTime normalizeOccurredAt(LocalDate value) {
    return value == null ? nowAtHour(9) : value.atTime(9, 0);
  }

  private LocalDateTime nowAtHour(int hour) {
    return LocalDate.now().atTime(hour, 0);
  }

  private List<List<Integer>> createHeatmapFromStatus(
      SlotStatus status, List<List<Integer>> existingSeed) {
    if (existingSeed != null && !existingSeed.isEmpty()) {
      return safeMatrix(existingSeed);
    }

    int base =
        switch (status) {
          case peak -> 72;
          case maintenance -> 78;
          case balanced -> 48;
          case available -> 22;
        };

    List<List<Integer>> heatmap = new ArrayList<>();
    for (int dayIndex = 0; dayIndex < HEATMAP_DAYS.size(); dayIndex += 1) {
      List<Integer> row = new ArrayList<>();
      for (int hourIndex = 0; hourIndex < HEATMAP_HOURS.size(); hourIndex += 1) {
        int modifier = dayIndex * 3 + hourIndex * 4;
        row.add(Math.min(96, Math.max(12, base + modifier - (dayIndex > 4 ? 18 : 0))));
      }
      heatmap.add(row);
    }
    return heatmap;
  }

  private List<Integer> createForecastFromStatus(SlotStatus status, List<Integer> existingSeed) {
    if (existingSeed != null && !existingSeed.isEmpty()) {
      return safeIntegerList(existingSeed);
    }

    int base =
        switch (status) {
          case peak -> 82;
          case maintenance -> 88;
          case balanced -> 58;
          case available -> 38;
        };

    List<Integer> forecast = new ArrayList<>();
    for (int index = 0; index < 7; index += 1) {
      forecast.add(Math.min(96, base + (index % 3) * 4 - (index > 4 ? 6 : 0)));
    }
    return forecast;
  }

  private List<WorkspacePayloads.OrderLineItem> mapItems(List<OrderLineItemDto> items) {
    return items.stream()
        .map(item -> new WorkspacePayloads.OrderLineItem(item.label(), item.quantity(), scale(item.unitPrice())))
        .toList();
  }

  private BigDecimal computeItemsSubtotal(List<WorkspacePayloads.OrderLineItem> items) {
    return items.stream()
        .map(
            item ->
                scale(item.unitPrice())
                    .multiply(BigDecimal.valueOf(item.quantity() == null ? 0 : item.quantity())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private String nextIdentifier(String prefix, List<String> existingValues) {
    int next =
        existingValues.stream()
            .filter(Objects::nonNull)
            .map(value -> value.replace(prefix + "-", ""))
            .map(value -> value.replaceAll("[^0-9]", ""))
            .filter(value -> !value.isBlank())
            .mapToInt(Integer::parseInt)
            .max()
            .orElse(0);
    return prefix + "-" + String.format(Locale.ROOT, "%03d", next + 1);
  }

  private String firstListItem(List<String> values) {
    return values == null || values.isEmpty() ? "" : normalizeText(values.get(0));
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (!isBlank(value)) {
        return value.trim();
      }
    }
    return "";
  }

  private String enterpriseCity(Enterprise enterprise) {
    return enterprise != null && enterprise.getUser() != null ? normalizeText(enterprise.getUser().getCity()) : "";
  }

  private boolean hasNotificationDestination(WorkspaceReservation reservation) {
    if (defaultNotificationChannel(reservation.getNotificationChannel()) == NotificationChannel.email) {
      return !isBlank(reservation.getContactEmail());
    }
    if (defaultNotificationChannel(reservation.getNotificationChannel()) == NotificationChannel.sms) {
      return !isBlank(reservation.getContactPhone());
    }
    return !isBlank(reservation.getContactEmail()) || !isBlank(reservation.getContactPhone());
  }

  private ReservationRole defaultReservationRole(ReservationRole role) {
    return role == null ? ReservationRole.provider : role;
  }

  private ReservationCategory defaultReservationCategory(
      ReservationCategory category, WorkspaceSlotResponse slot) {
    if (category != null) {
      return category;
    }
    if (slot == null) {
      return ReservationCategory.Space;
    }
    return switch (slot.type()) {
      case Storage -> ReservationCategory.Storage;
      case Production -> ReservationCategory.Production;
      case Dock, Meeting -> ReservationCategory.Space;
    };
  }

  private ReservationStatus defaultReservationStatus(ReservationStatus status) {
    return status == null ? ReservationStatus.pending : status;
  }

  private SlotPortfolio defaultSlotPortfolio(SlotPortfolio portfolio) {
    return portfolio == null ? SlotPortfolio.owned : portfolio;
  }

  private SlotStatus defaultSlotStatus(SlotStatus status) {
    return status == null ? SlotStatus.available : status;
  }

  private OrderStatus defaultOrderStatus(OrderStatus status) {
    return status == null ? OrderStatus.draft : status;
  }

  private PaymentStatus defaultPaymentStatus(PaymentStatus status) {
    return status == null ? PaymentStatus.pending : status;
  }

  private NotificationChannel defaultNotificationChannel(NotificationChannel channel) {
    return channel == null ? NotificationChannel.email : channel;
  }

  private List<String> safeStringList(List<String> values) {
    if (values == null) {
      return new ArrayList<>();
    }
    return values.stream()
        .filter(Objects::nonNull)
        .map(String::trim)
        .filter(value -> !value.isBlank())
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private List<Double> safeDoubleList(List<Double> values) {
    if (values == null) {
      return new ArrayList<>();
    }
    return values.stream().filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));
  }

  private List<Integer> safeIntegerList(List<Integer> values) {
    if (values == null) {
      return new ArrayList<>();
    }
    return values.stream().filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));
  }

  private List<List<Integer>> safeMatrix(List<List<Integer>> matrix) {
    if (matrix == null) {
      return new ArrayList<>();
    }
    return matrix.stream()
        .map(this::safeIntegerList)
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private BigDecimal scale(BigDecimal value) {
    if (value == null) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    return value.setScale(2, RoundingMode.HALF_UP);
  }

  private String normalizeText(String value) {
    return value == null ? "" : value.trim();
  }

  private boolean isBlank(String value) {
    return value == null || value.isBlank();
  }

  private record WorkspaceContext(
      Map<String, WorkspaceSlotResponse> slotById,
      Map<String, WorkspaceReservationResponse> reservationById,
      Map<String, WorkspaceOrderResponse> orderById,
      List<WorkspaceSlotResponse> slots,
      List<WorkspaceReservationResponse> reservations,
      List<WorkspaceOrderResponse> orders,
      List<ReservationConflictDto> conflicts,
      WorkspaceSummaryDto summary,
      List<InsightCardDto> reservationInsights,
      List<InsightCardDto> slotInsights,
      List<InsightCardDto> orderInsights,
      List<ReservationNotificationEventDto> reservationNotifications,
      List<ActivityCenterItemDto> activityFeed) {}

  private record NotificationDispatchOutcome(
      boolean emailDelivered, SmsDispatchResult smsDispatch) {}
}
