package com.marketplace.backend.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GroupPurchaseResponse {

  private Long id;
  private Long listingId;
  private Integer targetQuantity;
  private Integer currentQuantity;
  private Integer remainingQuantity;
  private LocalDateTime deadline;
  private String status;
  private List<ParticipantInfo> participants;

  @Data
  @Builder
  public static class ParticipantInfo {
    private Long id;
    private Long companyId;
    private Integer quantity;
  }
}
