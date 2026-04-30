package com.marketplace.backend.service;

import com.marketplace.backend.entity.ReservationSlot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class OpenAiService {

  @Value("${huggingface.api.token:}")
  private String apiToken;

  @Value("${huggingface.api.url:https://router.huggingface.co/v1/chat/completions}")
  private String apiUrl;

  @Value("${huggingface.model:Qwen/Qwen2.5-7B-Instruct-1M}")
  private String model;

  private final RestTemplate restTemplate;

  public OpenAiService() {
    this.restTemplate = new RestTemplate();
  }

  // =========================================================
  // 🔥 MAIN CALL
  // =========================================================
  public String ask(String prompt) {

    // 🔍 DEBUG (remove later if needed)
    String token = resolveApiToken();
    if (token == null || token.isBlank()) {
      return "{\"bestSlotId\":null,\"reason\":\"AI token missing, using backend-only scoring.\",\"confidence\":0}";
    }

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(token);

    Map<String, Object> body = new HashMap<>();
    body.put("model", resolveModel());

    List<Map<String, String>> messages = List.of(
      Map.of("role", "system", "content", "You are a strict JSON API. Respond with JSON only."),
      Map.of("role", "user", "content", prompt)
    );

    body.put("messages", messages);
    body.put("temperature", 0.2);
    body.put("max_tokens", 300);
    body.put("response_format", Map.of("type", "json_object"));

    HttpEntity<Map<String, Object>> request =
      new HttpEntity<>(body, headers);

    try {
      ResponseEntity<Map> response =
        restTemplate.postForEntity(apiUrl, request, Map.class);

      if (response.getBody() == null) {
        throw new RuntimeException("Empty response from AI provider");
      }

      // 🔍 DEBUG

      List<?> choices = (List<?>) response.getBody().get("choices");

      if (choices == null || choices.isEmpty()) {
        throw new RuntimeException("No choices returned from AI provider");
      }

      Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
      Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");

      if (message == null || message.get("content") == null) {
        throw new RuntimeException("Invalid AI response structure");
      }

      return message.get("content").toString();

    } catch (Exception e) {
      return "{\"bestSlotId\":null,\"reason\":\"AI provider unavailable, using backend-only scoring.\",\"confidence\":0}";
    }
  }

  // =========================================================
  // 🧠 PROMPT BUILDER
  // =========================================================
  public String buildSchedulingPrompt(List<ReservationSlot> slots) {

    StringBuilder sb = new StringBuilder();

    sb.append("""
You are an AI scheduling expert.

Your goal:
Choose the BEST slot based on efficiency, cost, and sustainability.

Rules:
- Prefer solar slots
- Prefer off-peak hours (0–6, 12–16)
- Avoid peak hours (18–22)
- Prefer shorter duration

Return STRICT JSON only:
{
  "bestSlotId": number,
  "reason": string,
  "confidence": number
}

Slots:
""");

    for (ReservationSlot s : slots) {
      sb.append(String.format(
        "ID=%d, hour=%d, solar=%s, duration=%d\n",
        s.getId(),
        s.getStartHour(),
        s.getSolar(),
        (s.getEndHour() - s.getStartHour())
      ));
    }

    return sb.toString();
  }

  private String resolveApiToken() {
    for (String envName : List.of("HF_TOKEN", "HUGGINGFACE_API_TOKEN", "HUGGING_FACE_HUB_TOKEN")) {
      String envValue = System.getenv(envName);
      if (envValue != null && !envValue.isBlank()) {
        return envValue;
      }
    }

    if (apiToken == null || apiToken.isBlank()) {
      return null;
    }

    String normalized = apiToken.trim().toLowerCase();
    if (List.of("test", "changeme", "your-api-key").contains(normalized)) {
      return null;
    }

    return apiToken;
  }

  private String resolveModel() {
    String envModel = System.getenv("HF_MODEL");
    if (envModel != null && !envModel.isBlank()) {
      return envModel;
    }

    if (model == null || model.isBlank()) {
      return "Qwen/Qwen2.5-7B-Instruct-1M";
    }

    return model;
  }
}
