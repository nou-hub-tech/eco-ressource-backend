package com.marketplace.backend.controller;

import com.marketplace.backend.dto.CreateListingRequest;
import com.marketplace.backend.dto.ListingDto;
import com.marketplace.backend.dto.ListingModerationRequest;
import com.marketplace.backend.service.ListingService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {

  private final ListingService listingService;

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<List<ListingDto>> all() {
    return ResponseEntity.ok(listingService.findAll());
  }

  @GetMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ListingDto> get(@PathVariable Long id) {
    try {
      return ResponseEntity.ok(listingService.getById(id));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @GetMapping("/my")
  @PreAuthorize("hasAnyRole('ENTERPRISE','ADMIN')")
  public ResponseEntity<List<ListingDto>> my(Authentication auth) {
    try {
      return ResponseEntity.ok(listingService.findMine(auth));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @PostMapping("/create")
  @PreAuthorize("hasAnyRole('ENTERPRISE','ADMIN')")
  public ResponseEntity<ListingDto> create(
      Authentication auth, @Valid @RequestBody CreateListingRequest req) {
    try {
      return ResponseEntity.status(HttpStatus.CREATED).body(listingService.create(auth, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ENTERPRISE','ADMIN')")
  public ResponseEntity<ListingDto> update(
      @PathVariable Long id, Authentication auth, @Valid @RequestBody CreateListingRequest req) {
    try {
      return ResponseEntity.ok(listingService.update(id, auth, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('ENTERPRISE','ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
    try {
      listingService.delete(id, auth);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PatchMapping("/{id}/moderate")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ListingDto> moderate(
      @PathVariable Long id, @Valid @RequestBody ListingModerationRequest req) {
    try {
      return ResponseEntity.ok(listingService.moderate(id, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }
}
