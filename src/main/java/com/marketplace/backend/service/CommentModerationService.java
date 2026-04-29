package com.marketplace.backend.service;

import com.marketplace.backend.dto.CommentModerationResult;
import com.marketplace.backend.entity.enums.CommentModerationStatus;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentModerationService {

  private static final List<String> BLOCKED_WORDS =
      List.of("spam", "arnaque", "insulte", "escroc", "haine", "toxique");

  private final PerspectiveModerationService perspectiveModerationService;

  @Value("${perspective.enabled:auto}")
  private String perspectiveEnabled;

  @Value("${perspective.toxicity.medium-threshold:0.45}")
  private double mediumThreshold;

  @Value("${perspective.toxicity.high-threshold:0.78}")
  private double highThreshold;

  public CommentModerationResult moderate(String content) {
    if (isPerspectiveEnabled()) {
      CommentModerationResult result = perspectiveModerationService.moderate(content);
      if (result.isEnabled()) {
        return classify(content, result.getToxicityScore(), result.getReason());
      }
    }

    String normalized = normalize(content);
    for (String word : BLOCKED_WORDS) {
      if (normalized.contains(word)) {
        return CommentModerationResult.builder()
            .enabled(true)
            .accepted(true)
            .toxicityScore(1)
            .message("Commentaire modere: le contenu contient un mot interdit (" + word + ")")
            .status(CommentModerationStatus.BLOCKED)
            .displayContent("Ce commentaire a ete supprime en raison de contenu inapproprie.")
            .reason("mot interdit: " + word)
            .build();
      }
    }
    return CommentModerationResult.builder()
        .enabled(true)
        .accepted(true)
        .toxicityScore(0)
        .message("Commentaire accepte")
        .status(CommentModerationStatus.VISIBLE)
        .displayContent(content)
        .reason(null)
        .build();
  }

  private CommentModerationResult classify(String content, double score, String reason) {
    if (score >= highThreshold) {
      return CommentModerationResult.builder()
          .enabled(true)
          .accepted(true)
          .toxicityScore(score)
          .message("Commentaire modere automatiquement: toxicite elevee")
          .status(CommentModerationStatus.BLOCKED)
          .displayContent("Ce commentaire a ete supprime en raison de toxicite elevee.")
          .reason(reason == null ? "toxicite elevee" : reason)
          .build();
    }
    if (score >= mediumThreshold) {
      return CommentModerationResult.builder()
          .enabled(true)
          .accepted(true)
          .toxicityScore(score)
          .message("Commentaire masque automatiquement: toxicite moyenne")
          .status(CommentModerationStatus.MASKED)
          .displayContent("Ce commentaire a ete masque en raison de toxicite moyenne.")
          .reason(reason == null ? "toxicite moyenne" : reason)
          .build();
    }
    return CommentModerationResult.builder()
        .enabled(true)
        .accepted(true)
        .toxicityScore(score)
        .message("Commentaire accepte")
        .status(CommentModerationStatus.VISIBLE)
        .displayContent(content)
        .reason(reason)
        .build();
  }

  private String normalize(String value) {
    String text = value == null ? "" : value;
    return Normalizer.normalize(text, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
  }

  private boolean isPerspectiveEnabled() {
    if ("true".equalsIgnoreCase(perspectiveEnabled)) {
      return true;
    }
    if ("false".equalsIgnoreCase(perspectiveEnabled)) {
      return false;
    }
    return perspectiveModerationService.isConfigured();
  }
}
