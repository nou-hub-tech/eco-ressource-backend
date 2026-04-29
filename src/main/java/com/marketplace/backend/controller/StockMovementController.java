package com.marketplace.backend.controller;

import com.marketplace.backend.dto.StockMovementRequest;
import com.marketplace.backend.entity.StockMovement;
import com.marketplace.backend.service.StockMovementService;
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
@RequestMapping("/api/stock-movements")
@RequiredArgsConstructor
@Tag(name = "Stock Movements", description = "Suivi des mouvements de stock (IN, OUT, UPDATE)")
public class StockMovementController {

  private final StockMovementService stockMovementService;

  @GetMapping
  @Operation(summary = "Lister les mouvements (filtre optionnel par stockItemId)")
  public ResponseEntity<List<StockMovement>> list(
      @RequestParam(required = false) Long stockItemId) {
    if (stockItemId != null) {
      return ResponseEntity.ok(stockMovementService.findByStockItemId(stockItemId));
    }
    return ResponseEntity.ok(stockMovementService.findAll());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Detail d'un mouvement par ID")
  public ResponseEntity<StockMovement> get(@PathVariable Long id) {
    return ResponseEntity.ok(stockMovementService.getById(id));
  }

  @PostMapping
  @Operation(summary = "Creer un mouvement de stock")
  public ResponseEntity<StockMovement> create(@Valid @RequestBody StockMovementRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(stockMovementService.create(req));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Modifier un mouvement de stock")
  public ResponseEntity<StockMovement> update(
      @PathVariable Long id, @Valid @RequestBody StockMovementRequest req) {
    return ResponseEntity.ok(stockMovementService.update(id, req));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Supprimer un mouvement de stock")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    stockMovementService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
