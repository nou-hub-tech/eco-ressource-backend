package com.marketplace.backend.controller;

import com.marketplace.backend.dto.StockItemRequest;
import com.marketplace.backend.entity.StockItem;
import com.marketplace.backend.service.StockItemService;
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
@RequestMapping("/api/stock-items")
@RequiredArgsConstructor
public class StockItemController {

  private final StockItemService stockItemService;

  @GetMapping
  public ResponseEntity<List<StockItem>> list(Authentication auth) {
    try {
      return ResponseEntity.ok(stockItemService.findAll(auth));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @GetMapping("/{id}")
  public ResponseEntity<StockItem> get(@PathVariable Long id, Authentication auth) {
    try {
      return ResponseEntity.ok(stockItemService.getById(id, auth));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PostMapping
  public ResponseEntity<StockItem> create(
      Authentication auth, @Valid @RequestBody StockItemRequest req) {
    try {
      return ResponseEntity.status(HttpStatus.CREATED).body(stockItemService.create(auth, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @PutMapping("/{id}")
  public ResponseEntity<StockItem> update(
      @PathVariable Long id, Authentication auth, @Valid @RequestBody StockItemRequest req) {
    try {
      return ResponseEntity.ok(stockItemService.update(id, auth, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
    try {
      stockItemService.delete(id, auth);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }
}
