package com.marketplace.backend.controller;

import com.marketplace.backend.dto.ExchangeRequestRequest;
import com.marketplace.backend.entity.ExchangeRequest;
import com.marketplace.backend.service.ExchangeRequestService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exchange-requests")
@RequiredArgsConstructor
public class ExchangeRequestController {

  private final ExchangeRequestService exchangeRequestService;

  @GetMapping
  public ResponseEntity<List<ExchangeRequest>> list(Authentication auth) {
    try {
      return ResponseEntity.ok(exchangeRequestService.findAll(auth));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @GetMapping("/{id}")
  public ResponseEntity<ExchangeRequest> get(@PathVariable Long id, Authentication auth) {
    try {
      return ResponseEntity.ok(exchangeRequestService.getById(id, auth));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PostMapping
  public ResponseEntity<ExchangeRequest> create(
      Authentication auth, @Valid @RequestBody ExchangeRequestRequest req) {
    try {
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(exchangeRequestService.create(auth, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @PutMapping("/{id}")
  public ResponseEntity<ExchangeRequest> update(
      @PathVariable Long id,
      Authentication auth,
      @Valid @RequestBody ExchangeRequestRequest req) {
    try {
      return ResponseEntity.ok(exchangeRequestService.update(id, auth, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PatchMapping("/{id}/status")
  public ResponseEntity<ExchangeRequest> patchStatus(
      @PathVariable Long id, Authentication auth, @RequestBody Map<String, String> body) {
    try {
      String st = body.get("status");
      if (st == null) {
        return ResponseEntity.badRequest().build();
      }
      return ResponseEntity.ok(exchangeRequestService.updateStatus(id, st, auth));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
    try {
      exchangeRequestService.delete(id, auth);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }
}
