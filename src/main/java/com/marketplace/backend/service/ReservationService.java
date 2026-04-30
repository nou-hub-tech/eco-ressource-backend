package com.marketplace.backend.service;

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

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

  private final ReservationRepository reservationRepository;
  private final ReservationSlotRepository slotRepository;
  private final EnterpriseRepository enterpriseRepository;
  private final SecurityUserHelper securityUserHelper;

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

    return includeDeleted
      ? reservationRepository.findByEnterpriseId(eid)
      : reservationRepository.findActiveByEnterpriseId(eid);
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

    if (r.getEnterprise() == null || u.getEnterprise() == null) {
      throw new IllegalArgumentException("Forbidden");
    }

    if (!r.getEnterprise().getId().equals(u.getEnterprise().getId())) {
      throw new IllegalArgumentException("Forbidden");
    }
  }

  // =========================================================
  // CREATE WITH SLOT (BOOKING)
  // =========================================================

  @Transactional
  public Reservation createWithSlot(Authentication auth, Long slotId) {

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

    User u = securityUserHelper.requireUser(auth);

    if (u.getEnterprise() == null) {
      throw new IllegalArgumentException("Forbidden");
    }

    Enterprise enterprise = u.getEnterprise();

    if (slot.getEnterprise() == null
      || !slot.getEnterprise().getId().equals(enterprise.getId())) {
      throw new IllegalArgumentException("Forbidden");
    }

    Reservation r = Reservation.builder()
      .company(enterprise.getCompanyName())
      .machine(slot.getMachine())
      .date(slot.getDate())
      .startHour(slot.getStartHour())
      .hours(slot.getEndHour() - slot.getStartHour())
      .solar(slot.getSolar())
      .status(ReservationStatus.CONFIRMED) // ✅ FIXED enum (lowercase)
      .slot(slot)
      .enterprise(enterprise)
      .deleted(false)
      .build();

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
  public Reservation update(Long id, Authentication auth, Reservation updated) {

    Reservation r = reservationRepository.findById(id)
      .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));

    User u = securityUserHelper.requireUser(auth);
    assertCanRead(u, r);

    if (Boolean.TRUE.equals(r.getDeleted())) {
      throw new IllegalArgumentException("Cannot edit a cancelled reservation");
    }

    if (updated.getStatus() != null) {
      r.setStatus(updated.getStatus());
    }

    if (updated.getCo2Saved() != null) {
      r.setCo2Saved(updated.getCo2Saved());
    }

    return reservationRepository.save(r);
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

    // free slot
    if (r.getSlot() != null) {
      ReservationSlot slot = r.getSlot();
      slot.setStatus(SlotStatus.open);
      slot.setReservation(null);
      slotRepository.save(slot);
    }

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
}
