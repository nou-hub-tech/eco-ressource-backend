package com.marketplace.backend.service;

import com.marketplace.backend.entity.ReservationSlot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class OpenAiService {

  @Value("${openai.api.key}")
  private String apiKey;

  @Value("${openai.api.url}")
  private String apiUrl;

  private final RestTemplate restTemplate;

  public OpenAiService() {
    this.restTemplate = new RestTemplate();
  }

  // =========================================================
  // 🔥 MAIN CALL
  // =========================================================
  public String ask(String prompt) {

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.setBearerAuth(apiKey);

    Map<String, Object> body = new HashMap<>();
    body.put("model", "gpt-4o-mini");

    List<Map<String, String>> messages = List.of(
      Map.of("role", "user", "content", prompt)
    );

    body.put("messages", messages);
    body.put("temperature", 0.2);
    body.put("max_tokens", 300);

    HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

    ResponseEntity<Map> response =
      restTemplate.postForEntity(apiUrl, request, Map.class);

    // 🔥 SAFE extraction
    if (response.getBody() == null) {
      throw new RuntimeException("Empty response from OpenAI");
    }

    List<?> choices = (List<?>) response.getBody().get("choices");
    if (choices == null || choices.isEmpty()) {
      throw new RuntimeException("No choices returned from OpenAI");
    }

    Map<?, ?> firstChoice = (Map<?, ?>) choices.get(0);
    Map<?, ?> message = (Map<?, ?>) firstChoice.get("message");

    return message.get("content").toString();
  }

  // =========================================================
  // 🧠 OPTIMIZED PROMPT (FOR SCHEDULING)
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
}
