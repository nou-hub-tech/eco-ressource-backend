package com.marketplace.backend.controller;

import com.marketplace.backend.dto.PlatformEventRequest;
import com.marketplace.backend.entity.PlatformEvent;
import com.marketplace.backend.service.PlatformEventService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform-events")
@RequiredArgsConstructor
public class PlatformEventController {

  private final PlatformEventService platformEventService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<PlatformEvent>> list() {
    return ResponseEntity.ok(platformEventService.findAll());
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<PlatformEvent> get(@PathVariable Long id) {
    try {
      return ResponseEntity.ok(platformEventService.getById(id));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<PlatformEvent> create(@Valid @RequestBody PlatformEventRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(platformEventService.create(req));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<PlatformEvent> update(
      @PathVariable Long id, @Valid @RequestBody PlatformEventRequest req) {
    try {
      return ResponseEntity.ok(platformEventService.update(id, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    try {
      platformEventService.delete(id);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }
}
