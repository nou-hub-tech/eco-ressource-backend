package com.marketplace.backend.controller;

import com.marketplace.backend.dto.ExchangeRequestDto;
import com.marketplace.backend.dto.ListingDto;
import com.marketplace.backend.dto.ListingModerationRequest;
import com.marketplace.backend.dto.MarketplaceListingRequest;
import com.marketplace.backend.dto.ReservationDto;
import com.marketplace.backend.dto.WalletTransactionDto;
import com.marketplace.backend.service.ListingService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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
  @PreAuthorize("hasAnyAuthority('ROLE_ENTERPRISE','ROLE_ADMIN','ROLE_TRANSPORTER')")
  public ResponseEntity<List<ListingDto>> all() {
    return ResponseEntity.ok(listingService.findAll());
  }

  @GetMapping("/my")
  @PreAuthorize("hasAnyAuthority('ROLE_ENTERPRISE','ROLE_ADMIN')")
  public ResponseEntity<List<ListingDto>> my(Authentication auth) {
    try {
      return ResponseEntity.ok(listingService.findMine(auth));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @GetMapping("/exchange-requests")
  @PreAuthorize("hasAnyAuthority('ROLE_ENTERPRISE','ROLE_ADMIN')")
  public ResponseEntity<List<ExchangeRequestDto>> exchangeRequests(Authentication auth) {
    try {
      return ResponseEntity.ok(listingService.myExchangeRequests(auth));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @GetMapping("/reservations/my")
  @PreAuthorize("hasAnyAuthority('ROLE_ENTERPRISE','ROLE_ADMIN')")
  public ResponseEntity<List<ReservationDto>> reservationsMy(Authentication auth) {
    try {
      return ResponseEntity.ok(listingService.myReservations(auth));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @GetMapping("/wallet/transactions")
  @PreAuthorize("hasAnyAuthority('ROLE_ENTERPRISE','ROLE_ADMIN')")
  public ResponseEntity<List<WalletTransactionDto>> wallet(Authentication auth) {
    return ResponseEntity.ok(listingService.myWalletTransactions(auth));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('ROLE_ENTERPRISE','ROLE_ADMIN','ROLE_TRANSPORTER')")
  public ResponseEntity<ListingDto> get(@PathVariable Long id) {
    try {
      return ResponseEntity.ok(listingService.getById(id));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PostMapping("/create")
  @PreAuthorize("hasAnyAuthority('ROLE_ENTERPRISE','ROLE_ADMIN')")
  public ResponseEntity<ListingDto> create(
      Authentication auth, @Valid @RequestBody MarketplaceListingRequest req) {
    try {
      return ResponseEntity.status(HttpStatus.CREATED).body(listingService.create(auth, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('ROLE_ENTERPRISE','ROLE_ADMIN')")
  public ResponseEntity<ListingDto> update(
      @PathVariable Long id, Authentication auth, @Valid @RequestBody MarketplaceListingRequest req) {
    try {
      return ResponseEntity.ok(listingService.update(id, auth, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('ROLE_ENTERPRISE','ROLE_ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
    try {
      listingService.delete(id, auth);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PatchMapping("/{id}/moderate")
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  public ResponseEntity<ListingDto> moderate(
      @PathVariable Long id, @Valid @RequestBody ListingModerationRequest req) {
    try {
      return ResponseEntity.ok(listingService.moderate(id, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PatchMapping("/exchange-requests/{id}")
  @PreAuthorize("hasAnyAuthority('ROLE_ENTERPRISE','ROLE_ADMIN')")
  public ResponseEntity<ExchangeRequestDto> patchExchange(
      @PathVariable Long id, Authentication auth, @RequestBody Map<String, String> body) {
    try {
      String st = body.get("status");
      if (st == null) {
        return ResponseEntity.badRequest().build();
      }
      return ResponseEntity.ok(listingService.updateExchangeStatus(id, st, auth));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }
}
