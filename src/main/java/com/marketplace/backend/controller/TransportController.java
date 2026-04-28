package com.marketplace.backend.controller;

import com.marketplace.backend.dto.DeliveryDto;
import com.marketplace.backend.dto.TransportOfferRequest;
import com.marketplace.backend.service.TransportService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transport")
@RequiredArgsConstructor
public class TransportController {

  private final TransportService transportService;

  @PostMapping("/offer")
  @PreAuthorize("hasAnyRole('TRANSPORTER','ADMIN')")
  public ResponseEntity<Void> offer(
      Authentication auth, @Valid @RequestBody TransportOfferRequest req) {
    try {
      transportService.createOffer(auth, req);
      return ResponseEntity.status(HttpStatus.CREATED).build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @GetMapping("/deliveries/transporter")
  @PreAuthorize("hasAnyRole('TRANSPORTER','ADMIN')")
  public ResponseEntity<List<DeliveryDto>> deliveriesTransporter(Authentication auth) {
    try {
      return ResponseEntity.ok(transportService.deliveriesForTransporter(auth));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @GetMapping("/deliveries/enterprise")
  @PreAuthorize("hasAnyRole('ENTERPRISE','ADMIN')")
  public ResponseEntity<List<DeliveryDto>> deliveriesEnterprise(Authentication auth) {
    try {
      return ResponseEntity.ok(transportService.deliveriesForEnterprise(auth));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }
}
