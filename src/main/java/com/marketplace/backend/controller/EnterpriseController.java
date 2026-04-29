package com.marketplace.backend.controller;

import com.marketplace.backend.dto.EnterpriseRequest;
import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.service.EnterpriseService;
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
@RequestMapping("/api/enterprises")
@RequiredArgsConstructor
public class EnterpriseController {

  private final EnterpriseService enterpriseService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<Enterprise>> list() {
    return ResponseEntity.ok(enterpriseService.findAll());
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Enterprise> get(@PathVariable Long id) {
    try {
      return ResponseEntity.ok(enterpriseService.getById(id));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Enterprise> create(@Valid @RequestBody EnterpriseRequest req) {
    try {
      return ResponseEntity.status(HttpStatus.CREATED).body(enterpriseService.create(req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Enterprise> update(
      @PathVariable Long id, @Valid @RequestBody EnterpriseRequest req) {
    try {
      return ResponseEntity.ok(enterpriseService.update(id, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    try {
      enterpriseService.delete(id);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }
}
