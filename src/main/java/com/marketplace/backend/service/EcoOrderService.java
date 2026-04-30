package com.marketplace.backend.service;

import com.marketplace.backend.dto.EcoOrderRequest;
import com.marketplace.backend.entity.EcoOrder;
import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.enums.EcoGrade;
import com.marketplace.backend.entity.enums.OrderStatus;
import com.marketplace.backend.entity.enums.Role;
import com.marketplace.backend.repository.EcoOrderRepository;
import com.marketplace.backend.repository.EnterpriseRepository;
import com.marketplace.backend.security.SecurityUserHelper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EcoOrderService {

  private final EcoOrderRepository orderRepository;
  private final EnterpriseRepository enterpriseRepository;
  private final SecurityUserHelper securityUserHelper;
  private final AiService aiService;

  /** DRAFT → CONFIRMED → SHIPPED → DELIVERED. */
  private static final List<OrderStatus> WORKFLOW =
      List.of(OrderStatus.draft, OrderStatus.confirmed, OrderStatus.shipped, OrderStatus.delivered);

  // ====================================================================
  //  Read
  // ====================================================================
  @Transactional(readOnly = true)
  public List<EcoOrder> findAll(Authentication auth, boolean includeDeleted) {
    User u = securityUserHelper.requireUser(auth);
    if (u.getRole() == Role.ROLE_ADMIN) {
      return includeDeleted
          ? orderRepository.findAll()
          : orderRepository.findAllActive();
    }
    if (u.getEnterprise() == null) {
      return List.of();
    }
    Long eid = u.getEnterprise().getId();
    return includeDeleted
        ? orderRepository.findByEnterpriseId(eid)
        : orderRepository.findActiveByEnterpriseId(eid);
  }

  @Transactional(readOnly = true)
  public EcoOrder getById(Long id, Authentication auth) {
    EcoOrder o =
        orderRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    assertCanRead(securityUserHelper.requireUser(auth), o);
    return o;
  }

  // ====================================================================
  //  Create / Update
  // ====================================================================
  @Transactional
  public EcoOrder create(Authentication auth, EcoOrderRequest req) {
    Enterprise e = resolveEnterprise(auth, req.getEnterpriseId());

    String ref = (req.getRef() == null || req.getRef().isBlank()) ? generateRef() : req.getRef();
    OrderStatus status =
        req.getStatus() == null || req.getStatus().isBlank()
            ? OrderStatus.draft
            : parseStatus(req.getStatus());
    EcoGrade grade =
        req.getGrade() == null || req.getGrade().isBlank()
            ? aiGrade(req.getCo2Saved(), null, null)
            : parseGrade(req.getGrade());
    BigDecimal co2Saved =
        req.getCo2Saved() != null ? req.getCo2Saved() : estimateCo2Saved(req.getQtyKg(), grade);

    EcoOrder o =
        EcoOrder.builder()
            .ref(ref)
            .companyName(req.getCompanyName())
            .material(req.getMaterial())
            .qtyKg(req.getQtyKg())
            .supplier(req.getSupplier())
            .distanceKm(req.getDistanceKm())
            .orderDate(req.getOrderDate() == null ? LocalDate.now() : req.getOrderDate())
            .status(status)
            .grade(grade)
            .co2Saved(co2Saved)
            .waterSaved(req.getWaterSaved())
            .wasteAvoided(req.getWasteAvoided())
            .enterprise(e)
            .deleted(false)
            .build();
    return orderRepository.save(o);
  }

  @Transactional
  public EcoOrder update(Long id, Authentication auth, EcoOrderRequest req) {
    EcoOrder o =
        orderRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    User u = securityUserHelper.requireUser(auth);
    assertCanRead(u, o);
    if (Boolean.TRUE.equals(o.getDeleted())) {
      throw new IllegalArgumentException("Cannot edit a cancelled order");
    }

    if (u.getRole() == Role.ROLE_ADMIN && req.getEnterpriseId() != null) {
      Enterprise e =
          enterpriseRepository
              .findById(req.getEnterpriseId())
              .orElseThrow(() -> new IllegalArgumentException("Enterprise not found"));
      o.setEnterprise(e);
    } else if (u.getRole() != Role.ROLE_ADMIN) {
      if (u.getEnterprise() == null) {
        throw new IllegalArgumentException("Forbidden");
      }
      o.setEnterprise(u.getEnterprise());
    }

    o.setCompanyName(req.getCompanyName());
    o.setMaterial(req.getMaterial());
    o.setQtyKg(req.getQtyKg());
    o.setSupplier(req.getSupplier());
    o.setDistanceKm(req.getDistanceKm());
    if (req.getOrderDate() != null) o.setOrderDate(req.getOrderDate());
    if (req.getStatus() != null) {
      OrderStatus nextStatus = parseStatus(req.getStatus());
      validateStatusTransition(o.getStatus(), nextStatus);
      o.setStatus(nextStatus);
    }
    if (req.getGrade() != null) {
      o.setGrade(parseGrade(req.getGrade()));
    }
    if (req.getCo2Saved() != null) o.setCo2Saved(req.getCo2Saved());
    if (req.getWaterSaved() != null) o.setWaterSaved(req.getWaterSaved());
    if (req.getWasteAvoided() != null) o.setWasteAvoided(req.getWasteAvoided());

    if (req.getGrade() == null || req.getGrade().isBlank()) {
      o.setGrade(aiGrade(o.getCo2Saved(), null, null));
    }
    if (o.getCo2Saved() == null) {
      o.setCo2Saved(estimateCo2Saved(o.getQtyKg(), o.getGrade()));
    }

    return orderRepository.save(o);
  }

  /**
   * Advance to the next workflow step (DRAFT → CONFIRMED → SHIPPED → DELIVERED).
   * Refuses to advance from terminal/cancelled states.
   */
  @Transactional
  public EcoOrder advanceStatus(Long id, Authentication auth) {
    EcoOrder o =
        orderRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    assertCanRead(securityUserHelper.requireUser(auth), o);
    if (Boolean.TRUE.equals(o.getDeleted()) || o.getStatus() == OrderStatus.cancelled) {
      throw new IllegalArgumentException("Cannot advance a cancelled order");
    }
    int idx = WORKFLOW.indexOf(o.getStatus());
    if (idx < 0 || idx >= WORKFLOW.size() - 1) {
      throw new IllegalArgumentException("Order is already at the final stage");
    }
    o.setStatus(WORKFLOW.get(idx + 1));
    return orderRepository.save(o);
  }

  // ====================================================================
  //  Delete
  // ====================================================================
  @Transactional
  public EcoOrder softDelete(Long id, Authentication auth, String reason) {
    EcoOrder o =
        orderRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    assertCanRead(securityUserHelper.requireUser(auth), o);
    o.setDeleted(true);
    o.setStatus(OrderStatus.cancelled);
    o.setCancelReason(reason == null || reason.isBlank() ? "No reason provided" : reason);
    return orderRepository.save(o);
  }

  @Transactional
  public void delete(Long id, Authentication auth) {
    EcoOrder o =
        orderRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    assertCanRead(securityUserHelper.requireUser(auth), o);
    orderRepository.delete(o);
  }

  // ====================================================================
  //  Helpers
  // ====================================================================
  private Enterprise resolveEnterprise(Authentication auth, Long enterpriseId) {
    User u = securityUserHelper.requireUser(auth);
    if (u.getRole() != Role.ROLE_ADMIN) {
      if (u.getEnterprise() == null) {
        throw new IllegalArgumentException("Forbidden");
      }
      return u.getEnterprise();
    }
    if (enterpriseId != null) {
      Enterprise e =
          enterpriseRepository
              .findById(enterpriseId)
              .orElseThrow(() -> new IllegalArgumentException("Enterprise not found"));
      return e;
    }
    throw new IllegalArgumentException("enterpriseId is required");
  }

  private void assertCanRead(User u, EcoOrder o) {
    if (u.getRole() == Role.ROLE_ADMIN) return;
    if (o.getEnterprise() == null || u.getEnterprise() == null) {
      throw new IllegalArgumentException("Forbidden");
    }
    if (!o.getEnterprise().getId().equals(u.getEnterprise().getId())) {
      throw new IllegalArgumentException("Forbidden");
    }
  }

  private OrderStatus parseStatus(String s) {
    try {
      return OrderStatus.valueOf(s.toLowerCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException(
          "Invalid status: " + s + " (allowed: draft, confirmed, shipped, delivered, cancelled)");
    }
  }

  private EcoGrade parseGrade(String s) {
    try {
      return EcoGrade.valueOf(s.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("Invalid grade: " + s + " (allowed: A, B, C, D, E)");
    }
  }

  /**
   * Tiny rule-based grader used as a server-side fallback when the
   * frontend doesn't supply a grade. The frontend's grader is richer.
   */
  private EcoGrade inferGrade(String material, Integer distanceKm) {
    int score = 0;
    String m = material == null ? "" : material.toLowerCase(Locale.ROOT);
    if (m.contains("recycled") || m.contains("bio") || m.contains("reused")) score += 2;
    if (m.contains("steel") || m.contains("aluminium") || m.contains("aluminum")) score -= 1;
    if (m.contains("virgin")) score -= 2;

    int km = distanceKm == null ? 0 : distanceKm;
    if (km < 50) score += 2;
    else if (km < 150) score += 1;
    else if (km > 500) score -= 1;
    else if (km > 1000) score -= 2;

    if (score >= 3) return EcoGrade.A;
    if (score >= 1) return EcoGrade.B;
    if (score == 0) return EcoGrade.C;
    if (score >= -2) return EcoGrade.D;
    return EcoGrade.E;
  }

  private String generateRef() {
    long count = orderRepository.count() + 2041;
    String candidate;
    do {
      candidate = "ORD-" + count;
      count++;
    } while (orderRepository.findByRef(candidate).isPresent());
    return candidate;
  }

  // Expose helpers for seed data
  public EcoGrade computeGrade(String material, Integer distanceKm) {
    return inferGrade(material, distanceKm);
  }

  public BigDecimal estimateCo2Saved(BigDecimal qtyKg, EcoGrade g) {
    if (qtyKg == null) return null;
    double factor =
        switch (g) {
          case A -> 1.8;
          case B -> 1.2;
          case C -> 0.8;
          case D -> 0.4;
          case E -> 0.1;
        };
    return qtyKg.multiply(BigDecimal.valueOf(factor)).setScale(2, java.math.RoundingMode.HALF_UP);
  }

  @Transactional(readOnly = true)
  public BigDecimal totalCo2Saved(Authentication auth, boolean includeDeleted) {
    return findAll(auth, includeDeleted).stream()
        .map(EcoOrder::getCo2Saved)
        .filter(java.util.Objects::nonNull)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  public double ecoScore(EcoOrder order) {
    if (order == null) {
      return 0.0d;
    }
    return aiService.computeEcoScore(order.getCo2Saved(), null, null).score();
  }

  public BigDecimal pricingImpact(EcoOrder order) {
    if (order == null || order.getQtyKg() == null) {
      return BigDecimal.ZERO;
    }
    BigDecimal base = order.getQtyKg().multiply(BigDecimal.valueOf(0.35d));
    BigDecimal ecoBonus =
        BigDecimal.valueOf(ecoScore(order) / 100.0d).multiply(BigDecimal.valueOf(25.0d));
    return base.add(ecoBonus).setScale(2, java.math.RoundingMode.HALF_UP);
  }

  private EcoGrade aiGrade(BigDecimal co2Saved, Boolean solar, Integer durationHours) {
    AiService.EcoScoreResult result = aiService.computeEcoScore(co2Saved, solar, durationHours);
    String grade = result.grade();
    if (grade == null || grade.isBlank()) {
      return inferGrade(null, null);
    }
    return parseGrade(grade);
  }

  private void validateStatusTransition(OrderStatus current, OrderStatus next) {
    if (current == null || next == null || current == next) {
      return;
    }

    if (current == OrderStatus.cancelled || current == OrderStatus.delivered) {
      throw new IllegalArgumentException("Invalid order status transition");
    }

    int currentIdx = WORKFLOW.indexOf(current);
    int nextIdx = WORKFLOW.indexOf(next);

    if (next != OrderStatus.cancelled && (currentIdx < 0 || nextIdx != currentIdx + 1)) {
      throw new IllegalArgumentException("Invalid order status transition");
    }
  }
}
