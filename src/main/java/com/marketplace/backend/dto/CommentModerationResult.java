package com.marketplace.backend.dto;

import com.marketplace.backend.entity.enums.CommentModerationStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommentModerationResult {

  private boolean enabled;
  private boolean accepted;
  private double toxicityScore;
  private String message;
  private CommentModerationStatus status;
  private String displayContent;
  private String reason;
}
