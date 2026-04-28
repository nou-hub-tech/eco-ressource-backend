package com.marketplace.backend.controller;

import com.marketplace.backend.dto.TransportOfferRequest;
import com.marketplace.backend.entity.TransportOffer;
import com.marketplace.backend.service.TransportOfferService;
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
@RequestMapping("/api/transport-offers")
@RequiredArgsConstructor
public class TransportOfferController {

  private final TransportOfferService transportOfferService;

  @GetMapping
  public ResponseEntity<List<TransportOffer>> list(Authentication auth) {
    try {
      return ResponseEntity.ok(transportOfferService.findAll(auth));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @GetMapping("/{id}")
  public ResponseEntity<TransportOffer> get(@PathVariable Long id, Authentication auth) {
    try {
      return ResponseEntity.ok(transportOfferService.getById(id, auth));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

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

  @PutMapping("/{id}")
  public ResponseEntity<TransportOffer> update(
      @PathVariable Long id, Authentication auth, @Valid @RequestBody TransportOfferRequest req) {
    try {
      return ResponseEntity.ok(transportOfferService.update(id, auth, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
    try {
      transportOfferService.delete(id, auth);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }
}
