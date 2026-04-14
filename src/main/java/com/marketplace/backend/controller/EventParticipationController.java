package com.marketplace.backend.controller;

import com.marketplace.backend.dto.EventParticipationRequest;
import com.marketplace.backend.entity.EventParticipation;
import com.marketplace.backend.service.EventParticipationService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/event-participations")
@RequiredArgsConstructor
public class EventParticipationController {

  private final EventParticipationService eventParticipationService;

  @GetMapping
  public ResponseEntity<List<EventParticipation>> list(
      @RequestParam(required = false) String userId) {
    return ResponseEntity.ok(eventParticipationService.findAll(userId));
  }

  @GetMapping("/{id}")
  public ResponseEntity<EventParticipation> get(@PathVariable Long id) {
    try {
      return ResponseEntity.ok(eventParticipationService.getById(id));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PostMapping
  public ResponseEntity<EventParticipation> create(@Valid @RequestBody EventParticipationRequest req) {
    try {
      return ResponseEntity.status(HttpStatus.CREATED).body(eventParticipationService.create(req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @PutMapping("/{id}")
  public ResponseEntity<EventParticipation> update(
      @PathVariable Long id, @Valid @RequestBody EventParticipationRequest req) {
    try {
      return ResponseEntity.ok(eventParticipationService.update(id, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    try {
      eventParticipationService.delete(id);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }
}