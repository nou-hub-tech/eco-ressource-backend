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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AiService {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final HttpClient httpClient =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  public String predictReservationPriority(
      LocalDate date, Integer hours, List<ReservationSlot> slots) {
    String prompt =
        "Given reservation data, classify priority: LOW, MEDIUM, HIGH based on urgency and demand. "
            + "Return JSON only with field priority. "
            + "date="
            + date
            + ", hours="
            + hours
            + ", slots="
            + summarizeSlots(slots);

    try {
      JsonNode json = callModel(prompt);
      String priority = json.path("priority").asText();
      return priority == null || priority.isBlank() ? "MEDIUM" : priority;
    } catch (Exception ex) {
      return "MEDIUM";
    }
  }

  public Map<Long, Double> suggestBestSlots(List<ReservationSlot> slots, Map<String, Long> density) {
    String prompt =
        "Given slot availability and demand, suggest the best slots for optimal efficiency. "
            + "Return JSON object where keys are slot IDs and values are scores from 0 to 1. "
            + "slots="
            + summarizeSlots(slots)
            + ", density="
            + density;

    try {
      JsonNode json = callModel(prompt).path("scores");
      Map<Long, Double> scores = new HashMap<>();
      json.fields()
          .forEachRemaining(entry -> scores.put(Long.valueOf(entry.getKey()), entry.getValue().asDouble()));
      return scores;
    } catch (Exception ex) {
      return Map.of();
    }
  }

  public EcoScoreResult computeEcoScore(BigDecimal co2Saved, Boolean solar, Integer durationHours) {
    String prompt =
        "Evaluate eco efficiency and assign a grade (A-E) with a score. "
            + "Return JSON only with fields grade and score. "
            + "co2Saved="
            + co2Saved
            + ", solar="
            + solar
            + ", duration="
            + durationHours;

    try {
      JsonNode json = callModel(prompt);
      String grade = json.path("grade").asText();
      double score = json.path("score").asDouble(0.0);
      return new EcoScoreResult(grade, score);
    } catch (Exception ex) {
      return new EcoScoreResult(null, 0.0);
    }
  }

  private JsonNode callModel(String prompt) throws IOException, InterruptedException {
    String apiKey = System.getenv("OPENAI_API_KEY");
    if (apiKey == null || apiKey.isBlank()) {
      throw new IOException("OPENAI_API_KEY is missing");
    }

    Map<String, Object> body = Map.of(
        "model", System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4o-mini"),
        "messages", List.of(
            Map.of("role", "system", "content", "You are a strict JSON API. Respond with JSON only."),
            Map.of("role", "user", "content", prompt)
        ),
        "response_format", Map.of("type", "json_object")
    );

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(System.getenv().getOrDefault("OPENAI_API_URL", "https://api.openai.com/v1/chat/completions")))
            .timeout(Duration.ofSeconds(30))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    JsonNode root = objectMapper.readTree(response.body());
    String content = root.path("choices").path(0).path("message").path("content").asText();
    return objectMapper.readTree(content);
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

  public record EcoScoreResult(String grade, double score) {}
}
