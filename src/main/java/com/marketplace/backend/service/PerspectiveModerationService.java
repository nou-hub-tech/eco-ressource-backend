package com.marketplace.backend.service;

import com.marketplace.backend.dto.CommentModerationResult;
import com.marketplace.backend.entity.enums.CommentModerationStatus;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class PerspectiveModerationService {

  private static final String PERSPECTIVE_URL =
      "https://commentanalyzer.googleapis.com/v1alpha1/comments:analyze?key=";

  private final RestTemplate restTemplate;

  @Value("${perspective.api.key:}")
  private String apiKey;

  @Value("${perspective.toxicity.threshold:0.78}")
  private double toxicityThreshold;

  public boolean isConfigured() {
    return apiKey != null && !apiKey.isBlank();
  }

  public CommentModerationResult moderate(String content) {
    if (apiKey == null || apiKey.isBlank()) {
      return CommentModerationResult.builder()
          .enabled(false)
          .accepted(true)
          .toxicityScore(0)
          .message("Moderation Perspective API desactivee")
          .status(CommentModerationStatus.VISIBLE)
          .displayContent(content)
          .reason("Perspective API desactivee")
          .build();
    }

    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      Map<String, Object> body =
          Map.of(
              "comment", Map.of("text", content),
              "languages", List.of("fr", "en"),
              "requestedAttributes", Map.of("TOXICITY", Map.of()));

      @SuppressWarnings("unchecked")
      Map<String, Object> response =
          restTemplate.postForObject(
              PERSPECTIVE_URL + apiKey, new HttpEntity<>(body, headers), Map.class);

      double score = extractToxicityScore(response);
      boolean accepted = score < toxicityThreshold;
      log.info("Perspective moderation score={} threshold={}", score, toxicityThreshold);
      return CommentModerationResult.builder()
          .enabled(true)
          .accepted(accepted)
          .toxicityScore(score)
          .message(
              accepted
                  ? "Commentaire accepte par la moderation"
                  : "Commentaire refuse: contenu potentiellement toxique")
          .status(accepted ? CommentModerationStatus.VISIBLE : CommentModerationStatus.BLOCKED)
          .displayContent(content)
          .reason("Score de toxicite Perspective: " + score)
          .build();
    } catch (Exception ex) {
      log.warn("Perspective API failed, fallback to local moderation: {}", ex.getMessage());
      return CommentModerationResult.builder()
          .enabled(true)
          .accepted(true)
          .toxicityScore(0)
          .message("Perspective API indisponible, moderation non bloquante")
          .status(CommentModerationStatus.VISIBLE)
          .displayContent(content)
          .reason("Perspective API indisponible")
          .build();
    }
  }

  @SuppressWarnings("unchecked")
  private double extractToxicityScore(Map<String, Object> response) {
    if (response == null) {
      return 0;
    }
    Object attrsObj = response.get("attributeScores");
    if (!(attrsObj instanceof Map<?, ?> attrs)) {
      return 0;
    }
    Object toxicityObj = attrs.get("TOXICITY");
    if (!(toxicityObj instanceof Map<?, ?> toxicity)) {
      return 0;
    }
    Object summaryObj = toxicity.get("summaryScore");
    if (!(summaryObj instanceof Map<?, ?> summary)) {
      return 0;
    }
    Object value = summary.get("value");
    return value instanceof Number n ? n.doubleValue() : 0;
  }
}
