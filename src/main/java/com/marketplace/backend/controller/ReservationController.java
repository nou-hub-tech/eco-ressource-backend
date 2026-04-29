package com.marketplace.backend.controller;

import com.marketplace.backend.dto.CancelReservationRequest;
import com.marketplace.backend.entity.Reservation;
import com.marketplace.backend.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

  private final ReservationService reservationService;

  // =========================================================
  // READ
  // =========================================================

  @GetMapping
  public ResponseEntity<List<Reservation>> list(
    Authentication auth,
    @RequestParam(name = "includeDeleted", defaultValue = "false")
    boolean includeDeleted) {

    return ResponseEntity.ok(
      reservationService.findAll(auth, includeDeleted)
    );
  }

  @GetMapping("/{id}")
  public ResponseEntity<Reservation> get(
    @PathVariable Long id,
    Authentication auth) {

    return ResponseEntity.ok(
      reservationService.getById(id, auth)
    );
  }

  // =========================================================
  // 🔥 CREATE (ONLY SLOT-BASED)
  // =========================================================

  @PostMapping("/with-slot/{slotId}")
  public ResponseEntity<Reservation> createWithSlot(
    @PathVariable Long slotId,
    Authentication auth) {

    return ResponseEntity.ok(
      reservationService.createWithSlot(auth, slotId)
    );
  }

  // =========================================================
  // UPDATE (OPTIONAL — minimal)
  // =========================================================

  @PutMapping("/{id}")
  public ResponseEntity<Reservation> update(
    @PathVariable Long id,
    Authentication auth,
    @RequestBody Reservation updated) {

    return ResponseEntity.ok(
      reservationService.update(id, auth, updated)
    );
  }

  // =========================================================
  // CANCEL
  // =========================================================

  @PostMapping("/{id}/cancel")
  public ResponseEntity<Reservation> cancel(
    @PathVariable Long id,
    Authentication auth,
    @RequestBody(required = false) @Valid CancelReservationRequest req) {

    String reason = req == null ? null : req.getReason();

    return ResponseEntity.ok(
      reservationService.softDelete(id, auth, reason)
    );
  }

  // =========================================================
  // DELETE
  // =========================================================

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(
    @PathVariable Long id,
    Authentication auth) {

    reservationService.delete(id, auth);
    return ResponseEntity.noContent().build();
  }
}
