package com.marketplace.backend.service;

import com.marketplace.backend.dto.EventSearchRequest;
import com.marketplace.backend.dto.PlatformEventRequest;
import com.marketplace.backend.dto.PlatformEventResponse;
import com.marketplace.backend.entity.PlatformEvent;
import com.marketplace.backend.entity.enums.EventStatus;
import com.marketplace.backend.repository.PlatformEventRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlatformEventService {

  private final PlatformEventRepository platformEventRepository;

  @Transactional(readOnly = true)
  public List<PlatformEventResponse> findAll() {
    return platformEventRepository.findAll().stream().map(this::mapToResponse).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public PlatformEventResponse  getById(Long id) {
    return mapToResponse(platformEventRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found")));
  }

 @Transactional
  public PlatformEventResponse create(PlatformEventRequest req) {
    

    PlatformEvent e =
        PlatformEvent.builder()
            .title(req.getTitle())
            .eventDate(req.getEventDate())
            .location(req.getLocation())
            .latitude(req.getLatitude())
            .longitude(req.getLongitude())
            .participants(req.getParticipants())
            .status(EventStatus.valueOf(req.getStatus().toLowerCase(Locale.ROOT)))
            .typeLabel(req.getTypeLabel())
            .description(req.getDescription())
            .build();
    

            

    return mapToResponse(platformEventRepository.save(e)); 
  }

  
  @Transactional
  public PlatformEventResponse update(Long id, PlatformEventRequest req) {
    PlatformEvent e =
        platformEventRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Not found"));

    e.setTitle(req.getTitle());
    e.setEventDate(req.getEventDate());
    e.setLocation(req.getLocation());
    e.setLatitude(req.getLatitude());
    e.setLongitude(req.getLongitude());
    e.setParticipants(req.getParticipants());
    e.setStatus(EventStatus.valueOf(req.getStatus().toLowerCase(Locale.ROOT)));
    e.setTypeLabel(req.getTypeLabel());
    e.setDescription(req.getDescription());

     return mapToResponse(platformEventRepository.save(e));
  }

  @Transactional
  public void delete(Long id) {
    PlatformEvent e =
        platformEventRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    platformEventRepository.delete(e);
  }

@Transactional(readOnly = true)
public List<PlatformEventResponse> findNearbyEvents(Double latitude, Double longitude, Double radius) {

  return platformEventRepository.findAll()
      .stream()
      .filter(e -> e.getLatitude() != null && e.getLongitude() != null)
      .map(e -> toNearbyResponse(e, latitude, longitude))
      .filter(r -> r.getDistance() <= radius)
      .sorted(Comparator.comparingDouble(PlatformEventResponse::getDistance))
      .collect(Collectors.toList());
}

private PlatformEventResponse toNearbyResponse(PlatformEvent e, Double lat, Double lon) {
  double distance = calculateDistanceKm(lat, lon, e.getLatitude(), e.getLongitude());

  return PlatformEventResponse.builder()
      .id(e.getId())
      .title(e.getTitle())
      .eventDate(e.getEventDate())
      .location(e.getLocation())
      .latitude(e.getLatitude())
      .longitude(e.getLongitude())
      .participants(e.getParticipants())
      .status(e.getStatus().name())
      .typeLabel(e.getTypeLabel())
      .description(e.getDescription())
      .createdAt(e.getCreatedAt())
      .distance(distance)
      .build();
}



  private EventStatus parseStatus(String status) {
    try {
      return EventStatus.valueOf(status.toLowerCase(Locale.ROOT));
    } catch (Exception ex) {
      throw new IllegalArgumentException("Invalid event status: " + status);
    }
  }

  private double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
    final double earthRadiusKm = 6371.0;

    double dLat = Math.toRadians(lat2 - lat1);
    double dLon = Math.toRadians(lon2 - lon1);

    double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
        + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
        * Math.sin(dLon / 2) * Math.sin(dLon / 2);

    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return earthRadiusKm * c;
  }

  @Transactional(readOnly = true)
  public Page<PlatformEventResponse> searchEvents(EventSearchRequest searchRequest) {
    Sort.Direction direction = "desc".equalsIgnoreCase(searchRequest.getSortDirection()) 
        ? Sort.Direction.DESC : Sort.Direction.ASC;
    
    Pageable pageable = PageRequest.of(
        searchRequest.getPage(), 
        searchRequest.getSize(), 
        Sort.by(direction, searchRequest.getSortBy())
    );
    
    Page<PlatformEvent> eventPage = platformEventRepository.searchEvents(
        searchRequest.getSearchTerm(),
        searchRequest.getStatuses(),
        searchRequest.getDateFrom(),
        searchRequest.getDateTo(),
        searchRequest.getMinParticipants(),
        searchRequest.getMaxParticipants(),
        pageable
    );
    
    return eventPage.map(this::mapToResponse);
  }

  private PlatformEventResponse mapToResponse(PlatformEvent e) {
  return PlatformEventResponse.builder()
      .id(e.getId())
      .title(e.getTitle())
      .eventDate(e.getEventDate())
      .location(e.getLocation())
      .latitude(e.getLatitude())
      .longitude(e.getLongitude())
      .participants(e.getParticipants())
      .status(e.getStatus().name())
      .typeLabel(e.getTypeLabel())
      .description(e.getDescription())
      .createdAt(e.getCreatedAt())
      .build();
}
}
