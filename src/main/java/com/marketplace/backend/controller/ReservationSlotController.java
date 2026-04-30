package com.marketplace.backend.controller;
import com.marketplace.backend.dto.CancelSlotRequest;
import com.marketplace.backend.dto.ReservationSlotRequest;
import com.marketplace.backend.dto.SlotBookRequest;
import com.marketplace.backend.entity.ReservationSlot;
import com.marketplace.backend.service.ReservationSlotService;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservation-slots")
@RequiredArgsConstructor
public class ReservationSlotController {

  private final ReservationSlotService slotService;

  // =========================================================
  // BASIC CRUD
  // =========================================================

  @GetMapping
  public ResponseEntity<List<Map<String, Object>>> list(
    Authentication auth,
    @RequestParam(name = "includeDeleted", defaultValue = "false") boolean includeDeleted) {
    return ResponseEntity.ok(slotService.findAll(auth, includeDeleted).stream().map(this::toResponse).toList());
  }

  @GetMapping("/range")
  public ResponseEntity<List<Map<String, Object>>> listRange(
    Authentication auth,
    @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ResponseEntity.ok(slotService.findInRange(auth, from, to).stream().map(this::toResponse).toList());
  }

  @GetMapping("/{id}")
  public ResponseEntity<Map<String, Object>> get(@PathVariable Long id, Authentication auth) {
    return ResponseEntity.ok(toResponse(slotService.getById(id, auth)));
  }

  @PostMapping
  public ResponseEntity<Map<String, Object>> create(
    Authentication auth, @RequestBody ReservationSlotRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(slotService.create(auth, req)));
  }

  @PutMapping("/{id}")
  public ResponseEntity<Map<String, Object>> update(
    @PathVariable Long id,
    Authentication auth,
    @RequestBody ReservationSlotRequest req) {
    return ResponseEntity.ok(toResponse(slotService.update(id, auth, req)));
  }

  @PostMapping("/{id}/book")
  public ResponseEntity<Map<String, Object>> book(
    @PathVariable Long id,
    Authentication auth,
    @RequestBody(required = false) SlotBookRequest req) {
    return ResponseEntity.ok(toResponse(slotService.book(id, auth, req == null ? null : req.getReservedBy())));
  }

  @PostMapping("/{id}/toggle")
  public ResponseEntity<Map<String, Object>> toggle(@PathVariable Long id, Authentication auth) {
    return ResponseEntity.ok(toResponse(slotService.toggleStatus(id, auth)));
  }

  @PostMapping("/{id}/cancel")
  public ResponseEntity<Map<String, Object>> cancel(
    @PathVariable Long id,
    Authentication auth,
    @RequestBody(required = false) CancelSlotRequest req) {

    String reason = (req == null) ? null : req.getReason();
    return ResponseEntity.ok(toResponse(slotService.softDelete(id, auth, reason)));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
    slotService.delete(id, auth);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/usage-rate")
  public ResponseEntity<Map<String, Object>> usageRate(
    Authentication auth,
    @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("from", from);
    body.put("to", to);
    body.put("usageRate", slotService.usageRate(auth, from, to));
    return ResponseEntity.ok(body);
  }

  @GetMapping("/demand-density")
  public ResponseEntity<Map<String, Long>> demandDensity(
    Authentication auth,
    @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

    return ResponseEntity.ok(slotService.demandDensity(auth, from, to));
  }

  // =========================================================
  // 🤖 AI RECOMMENDATION (REAL + HYBRID)
  // =========================================================

  @GetMapping("/recommend")
  public ResponseEntity<Map<String, Object>> recommend(Authentication auth) {
    LocalDate from = LocalDate.now();
    LocalDate to = from.plusDays(30);
    Map<String, Long> density = slotService.demandDensity(auth, from, to);
    Map<Long, Double> scores = slotService.suggestBestSlots(auth, from, to);
    List<Map<String, Object>> recommendations = slotService.findInRange(auth, from, to).stream()
      .filter(slot -> scores.containsKey(slot.getId()))
      .sorted((left, right) -> Double.compare(scores.getOrDefault(right.getId(), 0.0d), scores.getOrDefault(left.getId(), 0.0d)))
      .limit(5)
      .map(slot -> {
        Map<String, Object> row = new LinkedHashMap<>(toResponse(slot));
        row.put("score", scores.getOrDefault(slot.getId(), 0.0d));
        row.put("demandDensity", density.getOrDefault(slot.getDate() + "-" + slot.getStartHour(), 0L));
        return row;
      })
      .toList();

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("from", from);
    body.put("to", to);
    body.put("usageRate", slotService.usageRate(auth, from, to));
    body.put("recommendations", recommendations);
    return ResponseEntity.ok(body);
  }

  private Map<String, Object> toResponse(ReservationSlot slot) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("id", slot.getId());
    body.put("machine", slot.getMachine());
    body.put("date", slot.getDate());
    body.put("startHour", slot.getStartHour());
    body.put("endHour", slot.getEndHour());
    body.put("status", slot.getStatus() == null ? null : slot.getStatus().name());
    body.put("solar", Boolean.TRUE.equals(slot.getSolar()));
    body.put("discountPct", slot.getDiscountPct());
    body.put("enterpriseId", slot.getEnterprise() == null ? null : slot.getEnterprise().getId());
    body.put(
      "enterprise",
      slot.getEnterprise() == null
        ? null
        : Map.of(
          "id", slot.getEnterprise().getId(),
          "companyName", slot.getEnterprise().getCompanyName()
        )
    );
    body.put("createdAt", slot.getCreatedAt());
    body.put("deleted", slot.getDeleted());
    body.put("cancelReason", slot.getCancelReason());
    return body;
  }
}
