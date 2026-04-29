package com.marketplace.backend.dto;

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
  private List<CommentResponse> replies;
}
