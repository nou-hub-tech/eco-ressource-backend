package com.marketplace.backend.dto;

import com.marketplace.backend.entity.enums.EventStatus;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class EventSearchRequest {
  
  private String searchTerm; 
  
  private List<EventStatus> statuses; 
  
  private LocalDate dateFrom; 
  
  private LocalDate dateTo; 
  
  private Integer minParticipants; 
  
  private Integer maxParticipants; 

  private String sortBy; 
  
  private String sortDirection; 
  
  private Integer page = 0; 
  
  private Integer size = 20; 
  
  public EventSearchRequest() {
    this.sortBy = "eventDate";
    this.sortDirection = "asc";
  }
}
