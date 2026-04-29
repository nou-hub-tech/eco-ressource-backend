package com.marketplace.backend.controller;

import com.marketplace.backend.entity.WalletTransaction;
import com.marketplace.backend.service.WalletTransactionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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
}
