package com.marketplace.backend.controller;

import com.marketplace.backend.dto.DeliveryRequest;
import com.marketplace.backend.entity.Delivery;
import com.marketplace.backend.service.DeliveryService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

  private final DeliveryService deliveryService;

  @GetMapping
  public ResponseEntity<List<Delivery>> list(Authentication auth) {
    try {
      return ResponseEntity.ok(deliveryService.findAll(auth));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @GetMapping("/{id}")
  public ResponseEntity<Delivery> get(@PathVariable Long id, Authentication auth) {
    try {
      return ResponseEntity.ok(deliveryService.getById(id, auth));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PostMapping
  public ResponseEntity<Delivery> create(
      Authentication auth, @Valid @RequestBody DeliveryRequest req) {
    try {
      return ResponseEntity.status(HttpStatus.CREATED).body(deliveryService.create(auth, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @PutMapping("/{id}")
  public ResponseEntity<Delivery> update(
      @PathVariable Long id, Authentication auth, @Valid @RequestBody DeliveryRequest req) {
    try {
      return ResponseEntity.ok(deliveryService.update(id, auth, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
    try {
      deliveryService.delete(id, auth);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }
}
