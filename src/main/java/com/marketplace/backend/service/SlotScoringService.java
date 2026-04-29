package com.marketplace.backend.service;

import com.marketplace.backend.entity.ReservationSlot;
import com.marketplace.backend.entity.enums.SlotStatus;
import org.springframework.stereotype.Service;

@Service
public class SlotScoringService {

  // =========================================================
  // 🔥 MAIN SCORE FUNCTION
  // =========================================================
  public double computeScore(ReservationSlot s) {

    if (s == null) return -999;

    double score = 0;

    // =====================================================
    // 1. Availability (mandatory)
    // =====================================================
    if (s.getStatus() != SlotStatus.open) {
      return -999; // hard reject
    }

    // =====================================================
    // 2. Solar bonus
    // =====================================================
    if (Boolean.TRUE.equals(s.getSolar())) {
      score += 20;
    }

    // =====================================================
    // 3. Time-based heuristic
    // =====================================================
    int hour = safeHour(s.getStartHour());

    if (hour >= 0 && hour <= 6) {
      score += 15; // night (cheap)
    } else if (hour >= 12 && hour <= 16) {
      score += 10; // solar peak
    } else if (hour >= 18 && hour <= 22) {
      score -= 10; // peak demand
    }

    // =====================================================
    // 4. Duration bonus
    // =====================================================
    int duration = safeDuration(s);

    score += Math.max(0, 10 - duration);

    return score;
  }

  // =========================================================
  // 🧠 OPTIONAL: EXPLAIN SCORE (VERY USEFUL FOR AI/UX)
  // =========================================================
  public String explainScore(ReservationSlot s) {

    if (s == null) return "Invalid slot";

    StringBuilder reason = new StringBuilder();

    if (s.getStatus() != SlotStatus.open) {
      return "Slot not available";
    }

    if (Boolean.TRUE.equals(s.getSolar())) {
      reason.append("solar energy, ");
    }

    int hour = safeHour(s.getStartHour());

    if (hour >= 0 && hour <= 6) {
      reason.append("low-cost night slot, ");
    } else if (hour >= 12 && hour <= 16) {
      reason.append("solar peak window, ");
    } else if (hour >= 18 && hour <= 22) {
      reason.append("high demand period, ");
    }

    int duration = safeDuration(s);
    if (duration <= 4) {
      reason.append("short duration, ");
    }

    if (reason.length() == 0) {
      return "standard slot";
    }

    return reason.substring(0, reason.length() - 2);
  }

  // =========================================================
  // 🔒 SAFETY HELPERS
  // =========================================================
  private int safeHour(Integer hour) {
    return hour == null ? 0 : hour;
  }

  private int safeDuration(ReservationSlot s) {
    if (s.getStartHour() == null || s.getEndHour() == null) return 0;
    return Math.max(0, s.getEndHour() - s.getStartHour());
  }
}
