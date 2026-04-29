package com.marketplace.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDto {

  private String title;
  private String date;
  private String location;
  private int participants;
  private String status;
  private String type;
}
