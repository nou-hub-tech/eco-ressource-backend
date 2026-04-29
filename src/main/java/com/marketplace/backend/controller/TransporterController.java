package com.marketplace.backend.controller;

import com.marketplace.backend.dto.TransporterRequest;
import com.marketplace.backend.entity.Transporter;
import com.marketplace.backend.service.TransporterService;
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
@RequestMapping("/api/transporters")
@RequiredArgsConstructor
public class TransporterController {

  private final TransporterService transporterService;

  @GetMapping

  @PreAuthorize("permitAll()")  // ← CHANGEMENT: permet à tous les rôles d'accéder

  public ResponseEntity<List<Transporter>> list() {
    return ResponseEntity.ok(transporterService.findAll());
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Transporter> get(@PathVariable Long id) {
    try {
      return ResponseEntity.ok(transporterService.getById(id));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Transporter> create(@Valid @RequestBody TransporterRequest req) {
    try {
      return ResponseEntity.status(HttpStatus.CREATED).body(transporterService.create(req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Transporter> update(
      @PathVariable Long id, @Valid @RequestBody TransporterRequest req) {
    try {
      return ResponseEntity.ok(transporterService.update(id, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    try {
      transporterService.delete(id);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }
}
