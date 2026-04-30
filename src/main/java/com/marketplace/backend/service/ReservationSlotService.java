package com.marketplace.backend.service;

import com.marketplace.backend.dto.ReservationSlotRequest;
import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.ReservationSlot;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.enums.Role;
import com.marketplace.backend.entity.enums.SlotStatus;
import com.marketplace.backend.repository.EnterpriseRepository;
import com.marketplace.backend.repository.ReservationSlotRepository;
import com.marketplace.backend.security.SecurityUserHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ReservationSlotService {

  private final ReservationSlotRepository slotRepository;
  private final EnterpriseRepository enterpriseRepository;
  private final SecurityUserHelper securityUserHelper;

  // =========================================================
  // READ
  // =========================================================

  @Transactional(readOnly = true)
  public List<ReservationSlot> findAll(Authentication auth, boolean includeDeleted) {

    User u = securityUserHelper.requireUser(auth);

    if (u.getRole() == Role.ROLE_ADMIN) {
      return includeDeleted
        ? slotRepository.findAll()
        : slotRepository.findAllActive();
    }

    if (u.getEnterprise() == null) {
      return List.of();
    }

    Long eid = u.getEnterprise().getId();

    return includeDeleted
      ? slotRepository.findByEnterpriseId(eid)
      : slotRepository.findActiveByEnterpriseId(eid);
  }

  @Transactional(readOnly = true)
  public List<ReservationSlot> findInRange(LocalDate from, LocalDate to) {

    if (from == null || to == null || from.isAfter(to)) {
      throw new IllegalArgumentException("Invalid date range");
    }

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    User u = securityUserHelper.requireUser(auth);

    List<ReservationSlot> slots = slotRepository.findInRange(from, to);

    if (u.getRole() == Role.ROLE_ADMIN) {
      return slots;
    }

    if (u.getEnterprise() == null) {
      return List.of();
    }

    Long eid = u.getEnterprise().getId();

    return slots.stream()
      .filter(s -> s.getEnterprise() != null && eid.equals(s.getEnterprise().getId()))
      .toList();
  }

  @Transactional(readOnly = true)
  public ReservationSlot getById(Long id, Authentication auth) {

    ReservationSlot s = slotRepository.findById(id)
      .orElseThrow(() -> new IllegalArgumentException("Slot not found"));

    assertCanRead(securityUserHelper.requireUser(auth), s);
    return s;
  }

  // =========================================================
  // CREATE
  // =========================================================

  @Transactional
  public ReservationSlot create(Authentication auth, ReservationSlotRequest req) {

    validateRange(req.getStartHour(), req.getEndHour());

    Enterprise e = resolveEnterprise(auth, req.getEnterpriseId());

    SlotStatus status =
      (req.getStatus() == null || req.getStatus().isBlank())
        ? SlotStatus.open
        : parseStatus(req.getStatus());

    ReservationSlot s = ReservationSlot.builder()
      .machine(req.getMachine())
      .date(req.getDate())
      .startHour(req.getStartHour())
      .endHour(req.getEndHour())
      .status(status)
      .solar(Boolean.TRUE.equals(req.getSolar()))
      .discountPct(req.getDiscountPct() == null ? 0 : req.getDiscountPct())
      .enterprise(e)
      .deleted(false)
      .build();

    return slotRepository.save(s);
  }

  // =========================================================
  // 🔥 BOOK SLOT (CLEAN FIX)
  // =========================================================

  @Transactional
  public ReservationSlot book(Long slotId, Authentication auth, String reservedBy) {

    ReservationSlot slot = slotRepository.findById(slotId)
      .orElseThrow(() -> new IllegalArgumentException("Slot not found"));

    assertCanRead(securityUserHelper.requireUser(auth), slot);

    if (Boolean.TRUE.equals(slot.getDeleted())) {
      throw new IllegalArgumentException("Slot is deleted");
    }

    if (slot.getStatus() != SlotStatus.open) {
      throw new IllegalArgumentException("Slot not available");
    }

    // ✅ ONLY status change (no reservedBy anymore)
    slot.setStatus(SlotStatus.booked);

    return slotRepository.save(slot);
  }

  // =========================================================
  // UPDATE
  // =========================================================

  @Transactional
  public ReservationSlot update(Long id, Authentication auth, ReservationSlotRequest req) {

    validateRange(req.getStartHour(), req.getEndHour());

    ReservationSlot s = slotRepository.findById(id)
      .orElseThrow(() -> new IllegalArgumentException("Slot not found"));

    User u = securityUserHelper.requireUser(auth);
    assertCanRead(u, s);

    if (Boolean.TRUE.equals(s.getDeleted())) {
      throw new IllegalArgumentException("Cannot edit a deleted slot");
    }

    if (u.getRole() == Role.ROLE_ADMIN && req.getEnterpriseId() != null) {
      Enterprise e = enterpriseRepository.findById(req.getEnterpriseId())
        .orElseThrow(() -> new IllegalArgumentException("Enterprise not found"));
      s.setEnterprise(e);
    } else if (u.getRole() != Role.ROLE_ADMIN) {
      if (u.getEnterprise() == null) {
        throw new IllegalArgumentException("Forbidden");
      }
      s.setEnterprise(u.getEnterprise());
    }

    s.setMachine(req.getMachine());
    s.setDate(req.getDate());
    s.setStartHour(req.getStartHour());
    s.setEndHour(req.getEndHour());

    if (req.getStatus() != null) {
      s.setStatus(parseStatus(req.getStatus()));
    }

    s.setSolar(Boolean.TRUE.equals(req.getSolar()));

    if (req.getDiscountPct() != null) {
      s.setDiscountPct(req.getDiscountPct());
    }

    return slotRepository.save(s);
  }

  // =========================================================
  // TOGGLE STATUS
  // =========================================================

  @Transactional
  public ReservationSlot toggleStatus(Long id, Authentication auth) {

    ReservationSlot s = slotRepository.findById(id)
      .orElseThrow(() -> new IllegalArgumentException("Slot not found"));

    assertCanRead(securityUserHelper.requireUser(auth), s);

    if (Boolean.TRUE.equals(s.getDeleted())) {
      throw new IllegalArgumentException("Slot is deleted");
    }

    if (s.getStatus() == SlotStatus.booked) {
      throw new IllegalArgumentException("Cannot toggle a booked slot");
    }

    s.setStatus(s.getStatus() == SlotStatus.open
      ? SlotStatus.blocked
      : SlotStatus.open);

    return slotRepository.save(s);
  }

  // =========================================================
  // DELETE
  // =========================================================

  @Transactional
  public ReservationSlot softDelete(Long id, Authentication auth, String reason) {

    ReservationSlot s = slotRepository.findById(id)
      .orElseThrow(() -> new IllegalArgumentException("Slot not found"));

    assertCanRead(securityUserHelper.requireUser(auth), s);

    s.setDeleted(true);
    s.setStatus(SlotStatus.blocked);
    s.setCancelReason(reason == null || reason.isBlank()
      ? "No reason provided"
      : reason);

    return slotRepository.save(s);
  }

  @Transactional
  public void delete(Long id, Authentication auth) {

    ReservationSlot s = slotRepository.findById(id)
      .orElseThrow(() -> new IllegalArgumentException("Slot not found"));

    assertCanRead(securityUserHelper.requireUser(auth), s);

    slotRepository.delete(s);
  }

  // =========================================================
  // HELPERS
  // =========================================================

  private Enterprise resolveEnterprise(Authentication auth, Long enterpriseId) {

    User u = securityUserHelper.requireUser(auth);

    if (u.getRole() != Role.ROLE_ADMIN) {
      if (u.getEnterprise() == null) {
        throw new IllegalArgumentException("Forbidden");
      }

      return u.getEnterprise();
    }

    if (enterpriseId != null) {
      Enterprise e = enterpriseRepository.findById(enterpriseId)
        .orElseThrow(() -> new IllegalArgumentException("Enterprise not found"));

      return e;
    }

    throw new IllegalArgumentException("enterpriseId is required");
  }

  private void assertCanRead(User u, ReservationSlot s) {

    if (u.getRole() == Role.ROLE_ADMIN) return;

    if (s.getEnterprise() == null || u.getEnterprise() == null) {
      throw new IllegalArgumentException("Forbidden");
    }

    if (!s.getEnterprise().getId().equals(u.getEnterprise().getId())) {
      throw new IllegalArgumentException("Forbidden");
    }
  }

  private void validateRange(Integer start, Integer end) {

    if (start == null || end == null) {
      throw new IllegalArgumentException("startHour and endHour are required");
    }

    if (start < 0 || start > 23) {
      throw new IllegalArgumentException("startHour must be 0–23");
    }

    if (end < 1 || end > 24) {
      throw new IllegalArgumentException("endHour must be 1–24");
    }

    if (end <= start) {
      throw new IllegalArgumentException("endHour must be greater than startHour");
    }
  }

  private SlotStatus parseStatus(String s) {

    try {
      return SlotStatus.valueOf(s.toLowerCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException(
        "Invalid status: " + s + " (allowed: open, booked, blocked)");
    }
  }
}
