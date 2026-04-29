package com.marketplace.backend.controller;

import com.marketplace.backend.dto.TransportOfferRequest;
import com.marketplace.backend.entity.TransportOffer;
import com.marketplace.backend.service.TransportOfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transport-offers")
@RequiredArgsConstructor
public class TransportOfferController {

  private final TransportOfferService transportOfferService;

  @PostMapping
  public ResponseEntity<TransportOffer> create(
      Authentication auth, @Valid @RequestBody TransportOfferRequest req) {
    try {
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(transportOfferService.create(auth, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }
}
