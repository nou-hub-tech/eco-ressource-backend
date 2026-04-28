package com.marketplace.backend.controller;

import com.marketplace.backend.dto.SolidarityAssociationRequest;
import com.marketplace.backend.entity.SolidarityAssociation;
import com.marketplace.backend.service.SolidarityAssociationService;
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
@RequestMapping("/api/solidarity-associations")
@RequiredArgsConstructor
public class SolidarityAssociationController {

  private final SolidarityAssociationService solidarityAssociationService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<SolidarityAssociation>> list() {
    return ResponseEntity.ok(solidarityAssociationService.findAll());
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SolidarityAssociation> get(@PathVariable Long id) {
    try {
      return ResponseEntity.ok(solidarityAssociationService.getById(id));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SolidarityAssociation> create(
      @Valid @RequestBody SolidarityAssociationRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(solidarityAssociationService.create(req));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<SolidarityAssociation> update(
      @PathVariable Long id, @Valid @RequestBody SolidarityAssociationRequest req) {
    try {
      return ResponseEntity.ok(solidarityAssociationService.update(id, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    try {
      solidarityAssociationService.delete(id);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }
}
