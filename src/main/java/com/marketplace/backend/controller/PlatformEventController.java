package com.marketplace.backend.controller;

import com.marketplace.backend.dto.PlatformEventRequest;
import com.marketplace.backend.dto.PlatformEventResponse;
import com.marketplace.backend.entity.PlatformEvent;
import com.marketplace.backend.service.PlatformEventService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform-events")
@RequiredArgsConstructor
public class PlatformEventController {

  private final PlatformEventService platformEventService;

  @GetMapping
  public ResponseEntity<List<PlatformEventResponse>> list() {
    return ResponseEntity.ok(platformEventService.findAll());
  }

  @GetMapping("/nearby")
  public ResponseEntity<List<PlatformEventResponse>> getNearbyEvents(
      @RequestParam Double latitude,
      @RequestParam Double longitude,
      @RequestParam(defaultValue = "50.0") Double radius) {
    
    if (latitude == null || longitude == null || radius == null || radius <= 0) {
      return ResponseEntity.badRequest().build();
    }
    
    List<PlatformEventResponse> events = platformEventService.findNearbyEvents(latitude, longitude, radius);
    return ResponseEntity.ok(events);
  }

  @GetMapping("/{id}")
  public ResponseEntity<PlatformEventResponse> get(@PathVariable Long id) {
    try {
      return ResponseEntity.ok(platformEventService.getById(id));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PostMapping
  public ResponseEntity<PlatformEventResponse> create(@Valid @RequestBody PlatformEventRequest req) {
       System.out.println("=== CONTROLLER CREATE HIT ===");
    System.out.println("req = " + req);
    return ResponseEntity.status(HttpStatus.CREATED).body(platformEventService.create(req));
  }

  @PutMapping("/{id}")
  public ResponseEntity<PlatformEventResponse> update(
      @PathVariable Long id, @Valid @RequestBody PlatformEventRequest req) {
    try {
      return ResponseEntity.ok(platformEventService.update(id, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    try {
      platformEventService.delete(id);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }
}
