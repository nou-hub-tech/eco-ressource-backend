package com.marketplace.backend.controller;

import com.marketplace.backend.entity.Delivery;
import com.marketplace.backend.service.DeliveryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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
}
