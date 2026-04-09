package com.marketplace.backend.controller;

import com.marketplace.backend.dto.StockItemRequest;
import com.marketplace.backend.entity.StockItem;
import com.marketplace.backend.service.StockItemService;
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
@RequestMapping("/api/stock-items")
@RequiredArgsConstructor
public class StockItemController {

  private final StockItemService stockItemService;

  @GetMapping
  public ResponseEntity<List<StockItem>> list(
      @RequestParam(required = false) Long productId,
      @RequestParam(required = false) Long companyId) {
    if (productId != null) {
      return ResponseEntity.ok(stockItemService.findByProductId(productId));
    }
    if (companyId != null) {
      return ResponseEntity.ok(stockItemService.findByCompanyId(companyId));
    }
    return ResponseEntity.ok(stockItemService.findAll());
  }

  @GetMapping("/{id}")
  public ResponseEntity<StockItem> get(@PathVariable Long id) {
    try {
      return ResponseEntity.ok(stockItemService.getById(id));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PostMapping
  public ResponseEntity<StockItem> create(@Valid @RequestBody StockItemRequest req) {
    try {
      return ResponseEntity.status(HttpStatus.CREATED).body(stockItemService.create(req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @PutMapping("/{id}")
  public ResponseEntity<StockItem> update(
      @PathVariable Long id, @Valid @RequestBody StockItemRequest req) {
    try {
      return ResponseEntity.ok(stockItemService.update(id, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    try {
      stockItemService.delete(id);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }
}
