package com.marketplace.backend.controller;

import com.marketplace.backend.dto.ProductRequest;
import com.marketplace.backend.entity.Product;
import com.marketplace.backend.service.ProductService;
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
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Gestion du catalogue produits")
public class ProductController {

  private final ProductService productService;

  @GetMapping
  @Operation(summary = "Lister tous les produits (filtre optionnel par categorie)")
  public ResponseEntity<List<Product>> list(
      @RequestParam(required = false) String category) {
    if (category != null) {
      return ResponseEntity.ok(productService.findByCategory(category));
    }
    return ResponseEntity.ok(productService.findAll());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Detail d'un produit par ID")
  public ResponseEntity<Product> get(@PathVariable Long id) {
    return ResponseEntity.ok(productService.getById(id));
  }

  @PostMapping
  @Operation(summary = "Creer un produit")
  public ResponseEntity<Product> create(@Valid @RequestBody ProductRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(req));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Modifier un produit")
  public ResponseEntity<Product> update(
      @PathVariable Long id, @Valid @RequestBody ProductRequest req) {
    return ResponseEntity.ok(productService.update(id, req));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Supprimer un produit")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    productService.delete(id);
    return ResponseEntity.noContent().build();
  }
}
