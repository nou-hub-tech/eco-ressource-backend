package com.marketplace.backend.controller;

import com.marketplace.backend.dto.StockMovementRequest;
import com.marketplace.backend.entity.StockMovement;
import com.marketplace.backend.service.StockMovementService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock-movements")
@RequiredArgsConstructor
public class StockMovementController {

  private final StockMovementService stockMovementService;

  @GetMapping
  public ResponseEntity<List<StockMovement>> list(
      @RequestParam(required = false) Long stockItemId) {
    if (stockItemId != null) {
      return ResponseEntity.ok(stockMovementService.findByStockItemId(stockItemId));
    }
    return ResponseEntity.ok(stockMovementService.findAll());
  }

  @GetMapping("/{id}")
  public ResponseEntity<StockMovement> get(@PathVariable Long id) {
    try {
      return ResponseEntity.ok(stockMovementService.getById(id));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PostMapping
  public ResponseEntity<StockMovement> create(@Valid @RequestBody StockMovementRequest req) {
    try {
      return ResponseEntity.status(HttpStatus.CREATED).body(stockMovementService.create(req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @PutMapping("/{id}")
  public ResponseEntity<StockMovement> update(
      @PathVariable Long id, @Valid @RequestBody StockMovementRequest req) {
    try {
      return ResponseEntity.ok(stockMovementService.update(id, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    try {
      stockMovementService.delete(id);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }
}
