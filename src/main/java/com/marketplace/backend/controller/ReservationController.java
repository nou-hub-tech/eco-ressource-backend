package com.marketplace.backend.controller;

import com.marketplace.backend.dto.ReservationRequest;
import com.marketplace.backend.entity.Reservation;
import com.marketplace.backend.service.ReservationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class ReservationController {

  private final ReservationService reservationService;

  @GetMapping
  public ResponseEntity<List<Reservation>> list(Authentication auth) {
    try {
      return ResponseEntity.ok(reservationService.findAll(auth));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @GetMapping("/{id}")
  public ResponseEntity<Reservation> get(@PathVariable Long id, Authentication auth) {
    try {
      return ResponseEntity.ok(reservationService.getById(id, auth));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PostMapping
  public ResponseEntity<Reservation> create(
      Authentication auth, @Valid @RequestBody ReservationRequest req) {
    try {
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(reservationService.create(auth, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @PutMapping("/{id}")
  public ResponseEntity<Reservation> update(
      @PathVariable Long id, Authentication auth, @Valid @RequestBody ReservationRequest req) {
    try {
      return ResponseEntity.ok(reservationService.update(id, auth, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
    try {
      reservationService.delete(id, auth);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }
}
