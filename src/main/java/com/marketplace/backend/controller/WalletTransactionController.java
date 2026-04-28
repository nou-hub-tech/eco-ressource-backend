package com.marketplace.backend.controller;

import com.marketplace.backend.dto.WalletTransactionRequest;
import com.marketplace.backend.entity.WalletTransaction;
import com.marketplace.backend.service.WalletTransactionService;
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
@RequestMapping("/api/wallet-transactions")
@RequiredArgsConstructor
public class WalletTransactionController {

  private final WalletTransactionService walletTransactionService;

  @GetMapping
  public ResponseEntity<List<WalletTransaction>> list(Authentication auth) {
    return ResponseEntity.ok(walletTransactionService.findAll(auth));
  }

  @GetMapping("/{id}")
  public ResponseEntity<WalletTransaction> get(@PathVariable Long id, Authentication auth) {
    try {
      return ResponseEntity.ok(walletTransactionService.getById(id, auth));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PostMapping
  public ResponseEntity<WalletTransaction> create(
      Authentication auth, @Valid @RequestBody WalletTransactionRequest req) {
    try {
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(walletTransactionService.create(auth, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @PutMapping("/{id}")
  public ResponseEntity<WalletTransaction> update(
      @PathVariable Long id,
      Authentication auth,
      @Valid @RequestBody WalletTransactionRequest req) {
    try {
      return ResponseEntity.ok(walletTransactionService.update(id, auth, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
    try {
      walletTransactionService.delete(id, auth);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }
}
