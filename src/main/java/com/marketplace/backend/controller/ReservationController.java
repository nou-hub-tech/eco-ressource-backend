package com.marketplace.backend.controller;

import com.marketplace.backend.dto.CancelReservationRequest;
import com.marketplace.backend.dto.ReservationRequest;
import com.marketplace.backend.entity.Reservation;
import com.marketplace.backend.entity.enums.ReservationStatus;
import com.marketplace.backend.service.ReservationService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
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
  public ResponseEntity<List<Map<String, Object>>> list(
    Authentication auth,
    @RequestParam(name = "includeDeleted", defaultValue = "false")
    boolean includeDeleted) {

    return ResponseEntity.ok(
      reservationService.findAll(auth, includeDeleted).stream().map(this::toResponse).toList()
    );
  }

  @GetMapping("/filter")
  public ResponseEntity<List<Map<String, Object>>> filter(
    Authentication auth,
    @RequestParam(name = "status", required = false) ReservationStatus status,
    @RequestParam(name = "date", required = false) LocalDate date,
    @RequestParam(name = "includeDeleted", defaultValue = "false") boolean includeDeleted) {

    return ResponseEntity.ok(
      reservationService.findFiltered(auth, status, date, includeDeleted).stream().map(this::toResponse).toList()
    );
  }

  @GetMapping("/summary")
  public ResponseEntity<Map<String, Object>> summary(
    Authentication auth,
    @RequestParam(name = "includeDeleted", defaultValue = "false") boolean includeDeleted) {

    return ResponseEntity.ok(reservationService.summarize(auth, includeDeleted));
  }

  @GetMapping("/{id}")
  public ResponseEntity<Map<String, Object>> get(
    @PathVariable Long id,
    Authentication auth) {

    return ResponseEntity.ok(
      toResponse(reservationService.getById(id, auth))
    );
  }

  // =========================================================
  // 🔥 CREATE (ONLY SLOT-BASED)
  // =========================================================

  @PostMapping
  public ResponseEntity<Map<String, Object>> create(
    Authentication auth,
    @RequestBody(required = false) Map<String, Object> payload) {

    return ResponseEntity.ok(
      toResponse(reservationService.create(auth, toRequest(payload)))
    );
  }

  @PostMapping("/with-slot/{slotId}")
  public ResponseEntity<Map<String, Object>> createWithSlot(
    @PathVariable Long slotId,
    @RequestBody(required = false) Map<String, Object> payload,
    Authentication auth) {

    return ResponseEntity.ok(
      toResponse(reservationService.createWithSlot(auth, slotId, toRequest(payload)))
    );
  }

  // =========================================================
  // UPDATE (OPTIONAL — minimal)
  // =========================================================

  @PutMapping("/{id}")
  public ResponseEntity<Map<String, Object>> update(
    @PathVariable Long id,
    Authentication auth,
    @RequestBody Map<String, Object> payload) {

    return ResponseEntity.ok(
      toResponse(reservationService.update(id, auth, toRequest(payload)))
    );
  }

  // =========================================================
  // CANCEL
  // =========================================================

  @PostMapping("/{id}/cancel")
  public ResponseEntity<Map<String, Object>> cancel(
    @PathVariable Long id,
    Authentication auth,
    @RequestBody(required = false) @Valid CancelReservationRequest req) {

    String reason = req == null ? null : req.getReason();

    return ResponseEntity.ok(
      toResponse(reservationService.softDelete(id, auth, reason))
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

  private Map<String, Object> toResponse(Reservation reservation) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("id", reservation.getId());
    body.put("company", reservation.getCompany());
    body.put("machine", reservation.getMachine());
    body.put("date", reservation.getDate());
    body.put("hours", reservation.getHours());
    body.put("startHour", reservation.getStartHour());
    body.put("status", reservation.getStatus() == null ? null : reservation.getStatus().name());
    body.put("solar", Boolean.TRUE.equals(reservation.getSolar()));
    body.put("slotId", reservation.getSlot() == null ? null : reservation.getSlot().getId());
    body.put("enterpriseId", reservation.getEnterprise() == null ? null : reservation.getEnterprise().getId());
    body.put(
      "enterprise",
      reservation.getEnterprise() == null
        ? null
        : Map.of(
          "id", reservation.getEnterprise().getId(),
          "companyName", reservation.getEnterprise().getCompanyName()
        )
    );
    body.put("createdAt", reservation.getCreatedAt());
    body.put("co2Saved", reservation.getCo2Saved());
    body.put("deleted", reservation.getDeleted());
    body.put("cancelReason", reservation.getCancelReason());
    body.put(
      "aiPriority",
      reservationService.predictReservationPriority(
        reservation.getDate(),
        reservation.getHours(),
        reservation.getSlot() == null ? List.of() : List.of(reservation.getSlot())
      )
    );
    body.put("hasConflict", reservationService.hasConflict(reservation));
    return body;
  }

  private ReservationRequest toRequest(Map<String, Object> payload) {
    if (payload == null) {
      return null;
    }

    ReservationRequest req = new ReservationRequest();
    req.setCompanyName(asString(firstPresent(payload, "companyName", "company")));
    req.setMachine(asString(firstPresent(payload, "machine", "item")));
    req.setItem(asString(firstPresent(payload, "item", "machine")));
    LocalDate date = asDate(firstPresent(payload, "date", "fromDate"));
    req.setFromDate(date);
    req.setToDate(asDate(firstPresent(payload, "toDate", "date")));
    req.setStatus(asString(payload.get("status")));
    req.setHours(asInteger(payload.get("hours")));
    req.setStartHour(asInteger(payload.get("startHour")));
    req.setSolar(asBoolean(payload.get("solar")));
    req.setCo2Saved(asDecimal(payload.get("co2Saved")));
    req.setEnterpriseId(asLong(payload.get("enterpriseId")));
    return req;
  }

  private Object firstPresent(Map<String, Object> payload, String primary, String fallback) {
    return payload.containsKey(primary) ? payload.get(primary) : payload.get(fallback);
  }

  private String asString(Object value) {
    return value == null ? null : String.valueOf(value);
  }

  private Integer asInteger(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return number.intValue();
    }
    return Integer.valueOf(String.valueOf(value));
  }

  private Long asLong(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Number number) {
      return number.longValue();
    }
    return Long.valueOf(String.valueOf(value));
  }

  private Boolean asBoolean(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Boolean bool) {
      return bool;
    }
    return Boolean.valueOf(String.valueOf(value));
  }

  private java.math.BigDecimal asDecimal(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof java.math.BigDecimal decimal) {
      return decimal;
    }
    return new java.math.BigDecimal(String.valueOf(value));
  }

  private LocalDate asDate(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof LocalDate date) {
      return date;
    }
    return LocalDate.parse(String.valueOf(value));
  }
}
