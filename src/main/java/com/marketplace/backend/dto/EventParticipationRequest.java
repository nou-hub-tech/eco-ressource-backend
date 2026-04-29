package com.marketplace.backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EventParticipationRequest {

  @NotNull private Long userId;

  @NotNull private Long platformEventId;
}