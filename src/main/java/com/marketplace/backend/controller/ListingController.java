package com.marketplace.backend.controller;

import com.marketplace.backend.dto.CreateListingRequest;

import com.marketplace.backend.dto.ExchangeRequestDto;
import com.marketplace.backend.dto.ListingDto;
import com.marketplace.backend.dto.ListingModerationRequest;
import com.marketplace.backend.dto.ReservationDto;
import com.marketplace.backend.dto.WalletTransactionDto;
import com.marketplace.backend.service.ListingService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

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

import org.springframework.web.bind.annotation.*;
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
  public ResponseEntity<List<ListingDto>> all(Authentication auth) {
    return ResponseEntity.ok(listingService.findAllPublished(auth));
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


  // CREATE
  @PostMapping("/create")
  @PreAuthorize("hasAnyRole('ENTERPRISE','ADMIN')")
  public ResponseEntity<ListingDto> create(
          Authentication auth,
          @Valid @RequestBody CreateListingRequest req) {
    try {
      return ResponseEntity.status(HttpStatus.CREATED)
              .body(listingService.create(auth, req));

    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }


  // UPDATE (version propre merged -> updateMine)
  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ENTERPRISE','ADMIN')")
  public ResponseEntity<ListingDto> update(
          @PathVariable Long id,
          Authentication auth,
          @Valid @RequestBody CreateListingRequest req) {
    try {
      return ResponseEntity.ok(listingService.updateMine(id, auth, req));

    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }


  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('ENTERPRISE','ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
    try {
      listingService.deleteMine(id, auth);

      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }


  // MODERATION ADMIN
  @PatchMapping("/{id}/moderate")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<ListingDto> moderate(
          @PathVariable Long id,
          @Valid @RequestBody ListingModerationRequest req) {

    try {
      return ResponseEntity.ok(listingService.moderate(id, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }


  // ===================== EXTRA FEATURES (branche v2) =====================

  // EXCHANGE REQUESTS
  @GetMapping("/exchange-requests")
  @PreAuthorize("hasAnyRole('ENTERPRISE','ADMIN')")
  public ResponseEntity<List<ExchangeRequestDto>> exchangeRequests(Authentication auth) {
    try {
      return ResponseEntity.ok(listingService.myExchangeRequests(auth));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @PatchMapping("/exchange-requests/{id}")
  @PreAuthorize("hasAnyRole('ENTERPRISE','ADMIN')")
  public ResponseEntity<ExchangeRequestDto> patchExchange(
          @PathVariable Long id,
          Authentication auth,
          @RequestBody Map<String, String> body) {

    try {
      String status = body.get("status");
      if (status == null) {
        return ResponseEntity.badRequest().build();
      }
      return ResponseEntity.ok(listingService.updateExchangeStatus(id, status, auth));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  // RESERVATIONS
  @GetMapping("/reservations/my")
  @PreAuthorize("hasAnyRole('ENTERPRISE','ADMIN')")
  public ResponseEntity<List<ReservationDto>> reservationsMy(Authentication auth) {
    try {
      return ResponseEntity.ok(listingService.myReservations(auth));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  // WALLET
  @GetMapping("/wallet/transactions")
  @PreAuthorize("hasAnyRole('ENTERPRISE','ADMIN')")
  public ResponseEntity<List<WalletTransactionDto>> wallet(Authentication auth) {
    return ResponseEntity.ok(listingService.myWalletTransactions(auth));
  }
}

