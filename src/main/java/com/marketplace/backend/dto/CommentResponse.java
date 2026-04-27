package com.marketplace.backend.dto;

import com.marketplace.backend.entity.enums.CommentModerationStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommentResponse {

  private Long id;
  private String content;
  private Long userId;
  private String userFullName;
  private Long listingId;
  private Long parentId;
  private LocalDateTime createdAt;
  private CommentModerationStatus moderationStatus;
  private Double toxicityScore;
  private String moderationReason;
  private List<CommentResponse> replies;
}
