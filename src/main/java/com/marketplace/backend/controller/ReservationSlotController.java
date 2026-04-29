package com.marketplace.backend.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.backend.dto.CancelSlotRequest;
import com.marketplace.backend.dto.ReservationSlotRequest;
import com.marketplace.backend.dto.SlotBookRequest;
import com.marketplace.backend.entity.ReservationSlot;
import com.marketplace.backend.service.OpenAiService;
import com.marketplace.backend.service.ReservationSlotService;
import com.marketplace.backend.service.SlotScoringService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
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
  private final OpenAiService openAiService;
  private final SlotScoringService scoringService;

  // =========================================================
  // BASIC CRUD
  // =========================================================

  @GetMapping
  public ResponseEntity<List<ReservationSlot>> list(
    Authentication auth,
    @RequestParam(name = "includeDeleted", defaultValue = "false") boolean includeDeleted) {
    return ResponseEntity.ok(slotService.findAll(auth, includeDeleted));
  }

  @GetMapping("/range")
  public ResponseEntity<List<ReservationSlot>> listRange(
    @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
    @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ResponseEntity.ok(slotService.findInRange(from, to));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ReservationSlot> get(@PathVariable Long id, Authentication auth) {
    return ResponseEntity.ok(slotService.getById(id, auth));
  }

  @PostMapping
  public ResponseEntity<ReservationSlot> create(
    Authentication auth, @Valid @RequestBody ReservationSlotRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(slotService.create(auth, req));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ReservationSlot> update(
    @PathVariable Long id,
    Authentication auth,
    @Valid @RequestBody ReservationSlotRequest req) {
    return ResponseEntity.ok(slotService.update(id, auth, req));
  }

  @PostMapping("/{id}/book")
  public ResponseEntity<ReservationSlot> book(
    @PathVariable Long id,
    Authentication auth,
    @Valid @RequestBody SlotBookRequest req) {
    return ResponseEntity.ok(slotService.book(id, auth, req.getReservedBy()));
  }

  @PostMapping("/{id}/toggle")
  public ResponseEntity<ReservationSlot> toggle(@PathVariable Long id, Authentication auth) {
    return ResponseEntity.ok(slotService.toggleStatus(id, auth));
  }

  @PostMapping("/{id}/cancel")
  public ResponseEntity<ReservationSlot> cancel(
    @PathVariable Long id,
    Authentication auth,
    @Valid @RequestBody(required = false) CancelSlotRequest req) {

    String reason = (req == null) ? null : req.getReason();
    return ResponseEntity.ok(slotService.softDelete(id, auth, reason));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
    slotService.delete(id, auth);
    return ResponseEntity.noContent().build();
  }

  // =========================================================
  // 🤖 AI RECOMMENDATION (REAL + HYBRID)
  // =========================================================

  @GetMapping("/recommend")
  public ResponseEntity<Map<String, Object>> recommend(Authentication auth) {

    try {
      // 1. Get slots
      List<ReservationSlot> slots = slotService.findAll(auth, false);

      // 2. Score + filter
      List<ReservationSlot> topSlots = slots.stream()
        .filter(s -> scoringService.computeScore(s) > 0)
        .sorted((a, b) ->
          Double.compare(
            scoringService.computeScore(b),
            scoringService.computeScore(a)
          )
        )
        .limit(5)
        .toList();

      if (topSlots.isEmpty()) {
        return ResponseEntity.ok(Map.of(
          "message", "No available slots"
        ));
      }

      // 3. Build AI prompt
      String prompt = openAiService.buildSchedulingPrompt(topSlots);

      // 4. Call AI
      String aiRaw = openAiService.ask(prompt);

      // 5. Parse AI JSON
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> aiResult = mapper.readValue(aiRaw, Map.class);

      // 6. Attach backend info (important)
      Long bestId = ((Number) aiResult.get("bestSlotId")).longValue();

      Optional<ReservationSlot> bestSlot = topSlots.stream()
        .filter(s -> s.getId().equals(bestId))
        .findFirst();

      if (bestSlot.isPresent()) {
        aiResult.put("slot", bestSlot.get());
        aiResult.put("backendScore", scoringService.computeScore(bestSlot.get()));
      }

      return ResponseEntity.ok(aiResult);

    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(Map.of(
          "error", "AI recommendation failed",
          "details", e.getMessage()
        ));
    }
  }
}
