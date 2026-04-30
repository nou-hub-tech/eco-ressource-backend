package com.marketplace.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.backend.entity.ReservationSlot;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiService {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  private final String configuredApiToken;
  private final String configuredApiUrl;
  private final String configuredModel;

  public AiService(
      @Value("${huggingface.api.token:}") String configuredApiToken,
      @Value("${huggingface.api.url:https://router.huggingface.co/v1/chat/completions}")
          String configuredApiUrl,
      @Value("${huggingface.model:Qwen/Qwen2.5-7B-Instruct-1M}") String configuredModel) {
    this.configuredApiToken = configuredApiToken;
    this.configuredApiUrl = configuredApiUrl;
    this.configuredModel = configuredModel;
  }

  public String predictReservationPriority(
      LocalDate date, Integer hours, List<ReservationSlot> slots) {
    String prompt =
        "Classify reservation priority using only LOW, MEDIUM, or HIGH. "
            + "Return strict JSON with fields priority, confidence, and rationale. "
            + "date="
            + date
            + ", hours="
            + hours
            + ", slots="
            + summarizeSlots(slots);

    try {
      JsonNode json = callModel(prompt);
      String priority = json.path("priority").asText("MEDIUM").trim().toUpperCase();
      if (!List.of("LOW", "MEDIUM", "HIGH").contains(priority)) {
        return "MEDIUM";
      }
      return priority;
    } catch (Exception ex) {
      return "MEDIUM";
    }
  }

  public Map<Long, Double> suggestBestSlots(List<ReservationSlot> slots, Map<String, Long> density) {
    if (slots == null || slots.isEmpty()) {
      return Map.of();
    }

    String prompt =
        "Rank the provided slot ids by sustainability, availability, and demand balance. "
            + "Return strict JSON with field scores as an object of slotId -> score from 0 to 1. "
            + "slots="
            + summarizeSlots(slots)
            + ", density="
            + density;

    try {
      JsonNode json = callModel(prompt).path("scores");
      Map<Long, Double> scores = new HashMap<>();
      json.fields()
          .forEachRemaining(entry -> scores.put(Long.valueOf(entry.getKey()), entry.getValue().asDouble()));
      if (!scores.isEmpty()) {
        return scores;
      }
    } catch (Exception ex) {
      // Safe fallback below.
    }

    List<ReservationSlot> ranked = new ArrayList<>(slots);
    ranked.sort(
        (left, right) ->
            Double.compare(fallbackSlotScore(right, density), fallbackSlotScore(left, density)));
    Map<Long, Double> fallback = new HashMap<>();
    double max = ranked.stream().mapToDouble(slot -> fallbackSlotScore(slot, density)).max().orElse(1.0d);
    for (ReservationSlot slot : ranked) {
      fallback.put(slot.getId(), Math.max(0.0d, Math.min(1.0d, fallbackSlotScore(slot, density) / max)));
    }
    return fallback;
  }

  public EcoScoreResult computeEcoScore(BigDecimal co2Saved, Boolean solar, Integer durationHours) {
    String prompt =
        "Evaluate eco efficiency and assign a grade from A to E with a numeric score from 0 to 100. "
            + "Return strict JSON with fields grade and score. "
            + "co2Saved="
            + co2Saved
            + ", solar="
            + solar
            + ", duration="
            + durationHours;

    try {
      JsonNode json = callModel(prompt);
      String grade = json.path("grade").asText().trim().toUpperCase();
      double score = json.path("score").asDouble(0.0);
      if (!List.of("A", "B", "C", "D", "E").contains(grade)) {
        return fallbackEcoScore(co2Saved, solar, durationHours);
      }
      return new EcoScoreResult(grade, Math.max(0.0d, Math.min(100.0d, score)));
    } catch (Exception ex) {
      return fallbackEcoScore(co2Saved, solar, durationHours);
    }
  }

  private JsonNode callModel(String prompt) throws IOException, InterruptedException {
    String apiToken = resolveApiToken();
    if (apiToken == null || apiToken.isBlank()) {
      throw new IOException("HF_TOKEN is missing");
    }

    Map<String, Object> body = Map.of(
        "model",
            resolveModel(),
        "messages", List.of(
            Map.of("role", "system", "content", "You are a strict JSON API. Respond with JSON only."),
            Map.of("role", "user", "content", prompt)
        ),
        "response_format", Map.of("type", "json_object")
    );

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(
                URI.create(resolveApiUrl()))
            .timeout(Duration.ofSeconds(30))
            .header("Authorization", "Bearer " + apiToken)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() < 200 || response.statusCode() >= 300) {
      throw new IOException("AI API call failed with status " + response.statusCode());
    }
    JsonNode root = objectMapper.readTree(response.body());
    String content = root.path("choices").path(0).path("message").path("content").asText();
    if (content == null || content.isBlank()) {
      throw new IOException("AI API returned empty content");
    }
    return objectMapper.readTree(content);
  }

  private String resolveApiToken() {
    for (String envName : List.of("HF_TOKEN", "HUGGINGFACE_API_TOKEN", "HUGGING_FACE_HUB_TOKEN")) {
      String envValue = System.getenv(envName);
      if (envValue != null && !envValue.isBlank()) {
        return envValue;
      }
    }

    if (configuredApiToken == null || configuredApiToken.isBlank()) {
      return null;
    }

    String normalized = configuredApiToken.trim().toLowerCase();
    if (List.of("test", "changeme", "your-api-key").contains(normalized)) {
      return null;
    }

    return configuredApiToken;
  }

  private String resolveApiUrl() {
    String envApiUrl = System.getenv("HF_API_URL");
    if (envApiUrl != null && !envApiUrl.isBlank()) {
      return envApiUrl;
    }
    if (configuredApiUrl == null || configuredApiUrl.isBlank()) {
      return "https://router.huggingface.co/v1/chat/completions";
    }
    return configuredApiUrl;
  }

  private String resolveModel() {
    String envModel = System.getenv("HF_MODEL");
    if (envModel != null && !envModel.isBlank()) {
      return envModel;
    }
    if (configuredModel == null || configuredModel.isBlank()) {
      return "Qwen/Qwen2.5-7B-Instruct-1M";
    }
    return configuredModel;
  }

  private String summarizeSlots(List<ReservationSlot> slots) {
    return slots.stream()
        .map(
            s ->
                "{id=%d,date=%s,start=%d,end=%d,solar=%s,status=%s}"
                    .formatted(
                        s.getId(),
                        s.getDate(),
                        s.getStartHour(),
                        s.getEndHour(),
                        s.getSolar(),
                        s.getStatus()))
        .toList()
        .toString();
  }

  private EcoScoreResult fallbackEcoScore(BigDecimal co2Saved, Boolean solar, Integer durationHours) {
    double score = co2Saved == null ? 0.0d : co2Saved.doubleValue() * 2.5d;
    if (Boolean.TRUE.equals(solar)) {
      score += 18.0d;
    }
    if (durationHours != null && durationHours > 0) {
      score += Math.min(20.0d, durationHours * 2.0d);
    }
    double normalized = Math.max(0.0d, Math.min(100.0d, score));
    String grade;
    if (normalized >= 80.0d) {
      grade = "A";
    } else if (normalized >= 65.0d) {
      grade = "B";
    } else if (normalized >= 45.0d) {
      grade = "C";
    } else if (normalized >= 25.0d) {
      grade = "D";
    } else {
      grade = "E";
    }
    return new EcoScoreResult(grade, normalized);
  }

  private double fallbackSlotScore(ReservationSlot slot, Map<String, Long> density) {
    if (slot == null || slot.getId() == null) {
      return 0.0d;
    }
    double score = 10.0d;
    if (Boolean.TRUE.equals(slot.getSolar())) {
      score += 20.0d;
    }
    if (slot.getStartHour() != null) {
      int hour = slot.getStartHour();
      if (hour >= 0 && hour <= 6) {
        score += 12.0d;
      } else if (hour >= 12 && hour <= 16) {
        score += 10.0d;
      } else if (hour >= 18 && hour <= 22) {
        score -= 6.0d;
      }
    }
    if (slot.getStartHour() != null && slot.getEndHour() != null) {
      score += Math.max(0.0d, 8.0d - (slot.getEndHour() - slot.getStartHour()));
    }
    String key = slot.getDate() + "-" + slot.getStartHour();
    score -= density.getOrDefault(key, 0L) * 2.0d;
    return Math.max(1.0d, score);
  }

  public record EcoScoreResult(String grade, double score) {}
}
