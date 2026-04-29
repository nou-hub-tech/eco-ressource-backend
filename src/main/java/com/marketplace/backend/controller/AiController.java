package com.marketplace.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.backend.entity.ReservationSlot;
import com.marketplace.backend.entity.enums.SlotStatus;
import com.marketplace.backend.repository.ReservationSlotRepository;
import com.marketplace.backend.service.OpenAiService;
import com.marketplace.backend.service.SlotScoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

  private final ReservationSlotRepository slotRepository;
  private final SlotScoringService scoringService;
  private final OpenAiService openAiService;

  // =========================================================
  // 🤖 AI RECOMMENDATIONS (HYBRID: BACKEND + AI)
  // =========================================================

  @GetMapping("/recommendations")
  public ResponseEntity<Map<String, Object>> getRecommendations() {

    try {
      // =====================================================
      // 1. LOAD & FILTER SLOTS
      // =====================================================
      List<ReservationSlot> slots = slotRepository.findAll();

      List<ReservationSlot> topSlots = slots.stream()
        .filter(s -> s.getStatus() == SlotStatus.open)
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

      // =====================================================
      // 2. BUILD AI PROMPT
      // =====================================================
      String prompt = openAiService.buildSchedulingPrompt(topSlots);

      // =====================================================
      // 3. CALL OPENAI
      // =====================================================
      String aiRaw = openAiService.ask(prompt);

      // =====================================================
      // 4. PARSE AI RESPONSE
      // =====================================================
      ObjectMapper mapper = new ObjectMapper();
      Map<String, Object> aiResult;

      try {
        aiResult = mapper.readValue(aiRaw, Map.class);
      } catch (Exception e) {
        // fallback if AI returns bad JSON
        return ResponseEntity.ok(Map.of(
          "error", "AI returned invalid JSON",
          "raw", aiRaw
        ));
      }

      // =====================================================
      // 5. ENRICH RESULT WITH BACKEND DATA
      // =====================================================
      Object bestIdObj = aiResult.get("bestSlotId");

      if (bestIdObj instanceof Number bestIdNum) {

        Long bestId = bestIdNum.longValue();

        Optional<ReservationSlot> bestSlot = topSlots.stream()
          .filter(s -> s.getId().equals(bestId))
          .findFirst();

        if (bestSlot.isPresent()) {
          ReservationSlot slot = bestSlot.get();

          aiResult.put("slot", slot);
          aiResult.put("backendScore", scoringService.computeScore(slot));
          aiResult.put("explanation", scoringService.explainScore(slot));
        }
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
