package com.marketplace.backend.service;

import com.marketplace.backend.dto.ReservationRequest;
import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.Reservation;
import com.marketplace.backend.entity.ReservationSlot;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.enums.ReservationStatus;
import com.marketplace.backend.entity.enums.Role;
import com.marketplace.backend.entity.enums.SlotStatus;
import com.marketplace.backend.repository.EnterpriseRepository;
import com.marketplace.backend.repository.ReservationRepository;
import com.marketplace.backend.repository.ReservationSlotRepository;
import com.marketplace.backend.security.SecurityUserHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReservationService {

  private final ReservationRepository reservationRepository;
  private final ReservationSlotRepository slotRepository;
  private final EnterpriseRepository enterpriseRepository;
  private final SecurityUserHelper securityUserHelper;
  private final AiService aiService;

  // =========================================================
  // READ
  // =========================================================

  @Transactional(readOnly = true)
  public List<Reservation> findAll(Authentication auth, boolean includeDeleted) {
    User u = securityUserHelper.requireUser(auth);

    if (u.getRole() == Role.ROLE_ADMIN) {
      return includeDeleted
        ? reservationRepository.findAll()
        : reservationRepository.findAllActive();
    }

    if (u.getEnterprise() == null) {
      return List.of();
    }

    Long eid = u.getEnterprise().getId();
    List<Reservation> source = includeDeleted
      ? reservationRepository.findAll()
      : reservationRepository.findAllActive();

    return source.stream()
      .filter(r -> isRelatedToEnterprise(r, eid))
      .distinct()
      .toList();
  }

  @Transactional(readOnly = true)
  public Reservation getById(Long id, Authentication auth) {
    Reservation r = reservationRepository.findById(id)
      .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

    assertCanRead(securityUserHelper.requireUser(auth), r);
    return r;
  }

  private void assertCanRead(User u, Reservation r) {
    if (u.getRole() == Role.ROLE_ADMIN) return;

    if (u.getEnterprise() == null) {
      throw new IllegalArgumentException("Forbidden");
    }

    if (!isRelatedToEnterprise(r, u.getEnterprise().getId())) {
      throw new IllegalArgumentException("Forbidden");
    }
  }

  // =========================================================
  // CREATE WITH SLOT (BOOKING)
  // =========================================================

  @Transactional
  public Reservation create(Authentication auth, ReservationRequest req) {
    Enterprise enterprise = resolveEnterprise(auth, req == null ? null : req.getEnterpriseId());

    if (enterprise == null) {
      throw new IllegalArgumentException("Enterprise profile required");
    }

    ReservationStatus status =
      req != null && req.getStatus() != null && !req.getStatus().isBlank()
        ? ReservationStatus.fromJson(req.getStatus())
        : ReservationStatus.PENDING;

    Reservation r = Reservation.builder()
      .company(firstNonBlank(req == null ? null : req.getCompanyName(), enterprise.getCompanyName()))
      .machine(firstNonBlank(req == null ? null : req.getMachine(), req == null ? null : req.getItem()))
      .date(firstNonNull(req == null ? null : req.getFromDate(), req == null ? null : req.getToDate()))
      .startHour(firstNonNull(req == null ? null : req.getStartHour(), 0))
      .hours(firstNonNull(req == null ? null : req.getHours(), 1))
      .solar(Boolean.TRUE.equals(req == null ? null : req.getSolar()))
      .status(status)
      .co2Saved(req == null ? null : req.getCo2Saved())
      .enterprise(enterprise)
      .deleted(false)
      .build();

    r.setStatus(req != null && req.getStatus() != null && !req.getStatus().isBlank()
      ? ReservationStatus.fromJson(req.getStatus())
      : ReservationStatus.PENDING);
    r.setCo2Saved(req == null ? null : req.getCo2Saved());

    return reservationRepository.save(r);
  }

  @Transactional
  public Reservation createWithSlot(Authentication auth, Long slotId, ReservationRequest req) {

    ReservationSlot slot = slotRepository.findById(slotId)
      .orElseThrow(() -> new IllegalArgumentException("Slot not found"));

    if (Boolean.TRUE.equals(slot.getDeleted())) {
      throw new IllegalArgumentException("Slot is deleted");
    }

    if (slot.getStatus() == SlotStatus.booked) {
      throw new IllegalArgumentException("Slot already booked");
    }

    if (slot.getStatus() == SlotStatus.blocked) {
      throw new IllegalArgumentException("Slot is blocked");
    }

    Enterprise enterprise = resolveEnterprise(auth, req == null ? null : req.getEnterpriseId());

    if (slot.getEnterprise() == null || enterprise == null) {
      throw new IllegalArgumentException("Forbidden");
    }

    predictReservationPriority(
      slot.getDate(),
      slot.getEndHour() - slot.getStartHour(),
      List.of(slot)
    );

    Reservation r = Reservation.builder()
      .company(firstNonBlank(req == null ? null : req.getCompanyName(), enterprise.getCompanyName()))
      .machine(firstNonBlank(req == null ? null : req.getMachine(), slot.getMachine()))
      .date(firstNonNull(req == null ? null : req.getFromDate(), slot.getDate()))
      .startHour(firstNonNull(req == null ? null : req.getStartHour(), slot.getStartHour()))
      .hours(firstNonNull(req == null ? null : req.getHours(), slot.getEndHour() - slot.getStartHour()))
      .solar(firstNonNull(req == null ? null : req.getSolar(), slot.getSolar()))
      .status(ReservationStatus.CONFIRMED) // ✅ FIXED enum (lowercase)
      .slot(slot)
      .enterprise(enterprise)
      .deleted(false)
      .build();

    r.setStatus(req != null && req.getStatus() != null && !req.getStatus().isBlank()
      ? ReservationStatus.fromJson(req.getStatus())
      : ReservationStatus.PENDING);
    r.setCo2Saved(req == null ? null : req.getCo2Saved());

    slot.setStatus(SlotStatus.booked);
    slot.setReservation(r);

    reservationRepository.save(r);
    slotRepository.save(slot);

    return r;
  }

  // =========================================================
  // UPDATE
  // =========================================================

  @Transactional
  public Reservation update(Long id, Authentication auth, ReservationRequest updated) {

    Reservation r = reservationRepository.findById(id)
      .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

    User u = securityUserHelper.requireUser(auth);
    assertCanRead(u, r);

    if (Boolean.TRUE.equals(r.getDeleted())) {
      throw new IllegalArgumentException("Cannot edit a cancelled reservation");
    }

    if (updated.getCompanyName() != null && !updated.getCompanyName().isBlank()) {
      r.setCompany(updated.getCompanyName());
    }

    if (updated.getMachine() != null && !updated.getMachine().isBlank()) {
      r.setMachine(updated.getMachine());
    }

    if (updated.getFromDate() != null) {
      r.setDate(updated.getFromDate());
    }

    if (updated.getStartHour() != null) {
      r.setStartHour(updated.getStartHour());
    }

    if (updated.getHours() != null) {
      r.setHours(updated.getHours());
    }

    if (updated.getSolar() != null) {
      r.setSolar(updated.getSolar());
    }

    if (updated.getStatus() != null && !updated.getStatus().isBlank()) {
      ReservationStatus nextStatus = ReservationStatus.fromJson(updated.getStatus());
      r.setStatus(nextStatus);
      if (nextStatus == ReservationStatus.CANCELLED) {
        r.setDeleted(true);
        releaseSlot(r);
      }
    }

    if (updated.getCo2Saved() != null) {
      r.setCo2Saved(updated.getCo2Saved());
    }

    return reservationRepository.save(r);
  }

  @Transactional(readOnly = true)
  public List<Reservation> findFiltered(
    Authentication auth,
    ReservationStatus status,
    LocalDate date,
    boolean includeDeleted) {

    return findAll(auth, includeDeleted).stream()
      .filter(r -> status == null || r.getStatus() == status)
      .filter(r -> date == null || date.equals(r.getDate()))
      .toList();
  }

  @Transactional(readOnly = true)
  public Map<String, Object> summarize(Authentication auth, boolean includeDeleted) {
    List<Reservation> reservations = findAll(auth, includeDeleted);

    int totalHours = reservations.stream()
      .map(Reservation::getHours)
      .filter(h -> h != null)
      .mapToInt(Integer::intValue)
      .sum();

    return Map.of(
      "count", reservations.size(),
      "totalHours", totalHours
    );
  }

  public String predictReservationPriority(
    LocalDate date,
    Integer hours,
    List<ReservationSlot> slots) {

    return aiService.predictReservationPriority(date, hours, slots);
  }

  @Transactional(readOnly = true)
  public boolean hasConflict(Reservation reservation) {
    if (reservation == null || reservation.getDate() == null) {
      return false;
    }

    return reservationRepository.findAllActive().stream()
      .filter(existing -> !existing.getId().equals(reservation.getId()))
      .filter(existing -> existing.getStatus() != ReservationStatus.CANCELLED)
      .anyMatch(existing -> overlaps(existing, reservation));
  }

  // =========================================================
  // SOFT DELETE
  // =========================================================

  @Transactional
  public Reservation softDelete(Long id, Authentication auth, String reason) {

    Reservation r = reservationRepository.findById(id)
      .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

    assertCanRead(securityUserHelper.requireUser(auth), r);

    r.setDeleted(true);
    r.setStatus(ReservationStatus.CANCELLED); // ✅ FIXED enum
    r.setCancelReason(reason == null || reason.isBlank()
      ? "No reason provided"
      : reason);

    releaseSlot(r);

    return reservationRepository.save(r);
  }

  // =========================================================
  // HARD DELETE
  // =========================================================

  @Transactional
  public void delete(Long id, Authentication auth) {

    Reservation r = reservationRepository.findById(id)
      .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

    assertCanRead(securityUserHelper.requireUser(auth), r);

    reservationRepository.delete(r);
  }

  private Enterprise resolveEnterprise(Authentication auth, Long requestedEnterpriseId) {
    User u = securityUserHelper.requireUser(auth);

    if (u.getRole() != Role.ROLE_ADMIN) {
      return u.getEnterprise();
    }

    if (requestedEnterpriseId == null) {
      return u.getEnterprise() != null ? u.getEnterprise() : securityUserHelper.requireEnterprise(auth);
    }

    return enterpriseRepository.findById(requestedEnterpriseId)
      .orElseThrow(() -> new IllegalArgumentException("Enterprise not found"));
  }

  private void releaseSlot(Reservation reservation) {
    if (reservation.getSlot() != null) {
      ReservationSlot slot = reservation.getSlot();
      slot.setStatus(SlotStatus.open);
      slot.setReservation(null);
      slotRepository.save(slot);
    }
  }

  private boolean overlaps(Reservation left, Reservation right) {
    if (!equalsNullable(left.getDate(), right.getDate())) {
      return false;
    }

    if (left.getSlot() != null && right.getSlot() != null
      && equalsNullable(left.getSlot().getId(), right.getSlot().getId())) {
      return true;
    }

    if (!equalsNullable(left.getMachine(), right.getMachine())) {
      return false;
    }

    int leftStart = firstNonNull(left.getStartHour(), 0);
    int leftEnd = leftStart + firstNonNull(left.getHours(), 0);
    int rightStart = firstNonNull(right.getStartHour(), 0);
    int rightEnd = rightStart + firstNonNull(right.getHours(), 0);

    return leftStart < rightEnd && rightStart < leftEnd;
  }

  private <T> T firstNonNull(T left, T right) {
    return left != null ? left : right;
  }

  private String firstNonBlank(String left, String right) {
    if (left != null && !left.isBlank()) {
      return left;
    }
    return right;
  }

  private boolean equalsNullable(Object left, Object right) {
    return left == null ? right == null : left.equals(right);
  }

  private boolean isRelatedToEnterprise(Reservation reservation, Long enterpriseId) {
    if (enterpriseId == null || reservation == null) {
      return false;
    }

    if (reservation.getEnterprise() != null
      && enterpriseId.equals(reservation.getEnterprise().getId())) {
      return true;
    }

    return reservation.getSlot() != null
      && reservation.getSlot().getEnterprise() != null
      && enterpriseId.equals(reservation.getSlot().getEnterprise().getId());
  }
}
