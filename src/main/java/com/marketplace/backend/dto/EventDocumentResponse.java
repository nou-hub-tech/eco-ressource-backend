package com.marketplace.backend.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventDocumentResponse {
  private Long id;
  private Long platformEventId;
  private String fileName;
  private String fileType;
  private Long fileSize;
  private LocalDateTime uploadedAt;
}
