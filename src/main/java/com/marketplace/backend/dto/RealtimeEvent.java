package com.marketplace.backend.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealtimeEvent<T> {
  private String type;
  private Long listingId;
  private Long groupId;
  private Long userId;
  private String message;
  private T payload;
  private LocalDateTime occurredAt;
}
