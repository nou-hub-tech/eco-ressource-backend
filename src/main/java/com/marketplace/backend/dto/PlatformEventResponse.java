package com.marketplace.backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class PlatformEventResponse {



  private Long id;
  private String title;
  private LocalDate eventDate;
  private String location;
  private Double latitude;
  private Double longitude;
  private Integer participants;
  private String status;
  private String typeLabel;
  private LocalDateTime createdAt;

  private Double distance;

    
}
