package com.marketplace.backend.controller;

import com.marketplace.backend.dto.StockItemRequest;
import com.marketplace.backend.entity.StockItem;
import com.marketplace.backend.service.StockItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Stock Items", description = "Gestion des articles en stock")
public class StockItemController {

  private final StockItemService stockItemService;

  @GetMapping
  @Operation(summary = "Lister les articles (filtre optionnel par productId ou companyId)")
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
  @Operation(summary = "Detail d'un article en stock par ID")
  public ResponseEntity<StockItem> get(@PathVariable Long id) {
    return ResponseEntity.ok(stockItemService.getById(id));
  }

  @PostMapping
  @Operation(summary = "Creer un article en stock")
  public ResponseEntity<StockItem> create(@Valid @RequestBody StockItemRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(stockItemService.create(req));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Modifier un article en stock")
  public ResponseEntity<StockItem> update(
      @PathVariable Long id, @Valid @RequestBody StockItemRequest req) {
    return ResponseEntity.ok(stockItemService.update(id, req));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Supprimer un article en stock")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    stockItemService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
