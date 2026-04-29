package com.marketplace.backend.controller;

import com.marketplace.backend.entity.PlatformEvent;
import com.marketplace.backend.service.PlatformEventService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
}
