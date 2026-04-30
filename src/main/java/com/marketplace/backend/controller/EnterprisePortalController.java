package com.marketplace.backend.controller;

import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.Product;
import com.marketplace.backend.entity.StockItem;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.repository.EnterpriseRepository;
import com.marketplace.backend.repository.IProductRepository;
import com.marketplace.backend.repository.IStockItemRepository;
import com.marketplace.backend.repository.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Portail entreprise (module produit / stock) — chemins {@code /api/enterprise/**}.
 */
@RestController
@RequestMapping("/api/enterprise")
public class EnterprisePortalController {

  private final UserRepository userRepository;
  private final EnterpriseRepository enterpriseRepository;
  private final IProductRepository productRepository;
  private final IStockItemRepository stockItemRepository;

  public EnterprisePortalController(
      UserRepository userRepository,
      EnterpriseRepository enterpriseRepository,
      IProductRepository productRepository,
      IStockItemRepository stockItemRepository) {
    this.userRepository = userRepository;
    this.enterpriseRepository = enterpriseRepository;
    this.productRepository = productRepository;
    this.stockItemRepository = stockItemRepository;
  }

  private Enterprise getEnterprise(Authentication auth) {
    String email = auth.getName();
    User user =
        userRepository
            .findByEmailWithProfiles(email)
            .orElseThrow(() -> new RuntimeException("User not found: " + email));
    return enterpriseRepository
        .findByUserId(user.getId())
        .orElseThrow(() -> new RuntimeException("No enterprise linked to this user"));
  }

  @GetMapping("/products")
  public ResponseEntity<List<Product>> getMyProducts(Authentication auth) {
    Enterprise e = getEnterprise(auth);
    return ResponseEntity.ok(productRepository.findByEnterpriseId(e.getId()));
  }

  @GetMapping("/products/search")
  public ResponseEntity<List<Product>> searchMyProducts(
      Authentication auth,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String category) {
    Enterprise e = getEnterprise(auth);
    return ResponseEntity.ok(
        productRepository.searchByEnterprise(e.getId(), blankToNull(name), blankToNull(category)));
  }

  @GetMapping("/products/{id}")
  public ResponseEntity<?> getProductById(@PathVariable Long id, Authentication auth) {
    Enterprise e = getEnterprise(auth);
    Product product =
        productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
    if (!e.getId().equals(product.getEnterpriseId())) {
      return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
    }
    return ResponseEntity.ok(product);
  }

  @PostMapping("/products")
  public ResponseEntity<?> addProduct(@RequestBody Product product, Authentication auth) {
    Enterprise e = getEnterprise(auth);
    product.setEnterprise(e);

    Product saved = productRepository.save(product);

    if (saved.getBarcode() == null || saved.getBarcode().isBlank()) {
      saved.setBarcode(String.format("200%010d", saved.getId_product()));
      saved = productRepository.save(saved);
    }

    return ResponseEntity.ok(saved);
  }

  @PutMapping("/products/{id}")
  public ResponseEntity<?> updateProduct(
      @PathVariable Long id, @RequestBody Product product, Authentication auth) {
    Enterprise e = getEnterprise(auth);
    Product existing =
        productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));

    if (!e.getId().equals(existing.getEnterpriseId())) {
      return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
    }

    product.setId_product(id);
    product.setEnterprise(e);

    if (product.getBarcode() == null || product.getBarcode().isBlank()) {
      product.setBarcode(existing.getBarcode());
    }

    return ResponseEntity.ok(productRepository.save(product));
  }

  @DeleteMapping("/products/{id}")
  public ResponseEntity<?> deleteProduct(@PathVariable Long id, Authentication auth) {
    Enterprise e = getEnterprise(auth);
    Product existing =
        productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));

    if (!e.getId().equals(existing.getEnterpriseId())) {
      return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
    }

    List<StockItem> linkedItems = stockItemRepository.findByProductId(id);
    linkedItems.forEach(s -> s.setDeleted(true));
    stockItemRepository.saveAll(linkedItems);

    productRepository.deleteById(id);
    return ResponseEntity.ok(Map.of("message", "Product deleted successfully"));
  }

  @GetMapping("/stock")
  public ResponseEntity<List<StockItem>> getMyStock(Authentication auth) {
    Enterprise e = getEnterprise(auth);
    return ResponseEntity.ok(stockItemRepository.findByEnterpriseId(e.getId()));
  }

  @GetMapping("/stock/paginated")
  public ResponseEntity<Map<String, Object>> getMyStockPaginated(
      Authentication auth,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "quantity") String sortBy,
      @RequestParam(defaultValue = "desc") String direction) {

    Enterprise e = getEnterprise(auth);
    Sort sort =
        direction.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();

    Page<StockItem> pg =
        stockItemRepository.findByEnterpriseId(e.getId(), PageRequest.of(page, size, sort));

    Map<String, Object> response = new HashMap<>();
    response.put("content", pg.getContent());
    response.put("totalPages", pg.getTotalPages());
    response.put("totalElements", pg.getTotalElements());
    response.put("currentPage", pg.getNumber());

    return ResponseEntity.ok(response);
  }

  @GetMapping("/stock/search")
  public ResponseEntity<List<StockItem>> searchMyStock(
      Authentication auth,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String productName) {
    Enterprise e = getEnterprise(auth);
    return ResponseEntity.ok(
        stockItemRepository.searchByEnterprise(e.getId(), blankToNull(status), blankToNull(productName)));
  }

  @GetMapping("/stock/total-value")
  public ResponseEntity<Map<String, Double>> getMyTotalValue(Authentication auth) {
    Enterprise e = getEnterprise(auth);
    Double val = stockItemRepository.calculateTotalStockValueByEnterprise(e.getId());
    return ResponseEntity.ok(Map.of("totalValue", val != null ? val : 0.0));
  }

  @GetMapping("/stock/{id}")
  public ResponseEntity<?> getStockItemById(@PathVariable Long id, Authentication auth) {
    Enterprise e = getEnterprise(auth);
    StockItem item =
        stockItemRepository.findById(id).orElseThrow(() -> new RuntimeException("StockItem not found"));
    if (item.getEnterprise() == null || !e.getId().equals(item.getEnterprise().getId())) {
      return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
    }
    return ResponseEntity.ok(item);
  }

  @PostMapping("/stock")
  public ResponseEntity<?> addStockItem(@RequestBody StockItem item, Authentication auth) {
    Enterprise e = getEnterprise(auth);
    item.setEnterprise(e);
    item.setDeleted(false);

    if (item.getProduct() != null && item.getProduct().getId_product() != null) {
      Optional<Product> prod = productRepository.findById(item.getProduct().getId_product());
      if (prod.isEmpty() || !e.getId().equals(prod.get().getEnterpriseId())) {
        return ResponseEntity.badRequest()
            .body(Map.of("error", "Product not found or does not belong to your enterprise"));
      }
      item.setProduct(prod.get());
    }

    return ResponseEntity.ok(stockItemRepository.save(item));
  }

  @PutMapping("/stock/{id}")
  public ResponseEntity<?> updateStockItem(
      @PathVariable Long id, @RequestBody StockItem item, Authentication auth) {
    Enterprise e = getEnterprise(auth);
    StockItem existing =
        stockItemRepository.findById(id).orElseThrow(() -> new RuntimeException("StockItem not found"));

    if (existing.getEnterprise() == null || !e.getId().equals(existing.getEnterprise().getId())) {
      return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
    }

    item.setIdStock(id);
    item.setEnterprise(e);
    item.setDeleted(false);

    if (item.getProduct() != null && item.getProduct().getId_product() != null) {
      productRepository.findById(item.getProduct().getId_product()).ifPresent(item::setProduct);
    }

    return ResponseEntity.ok(stockItemRepository.save(item));
  }

  @DeleteMapping("/stock/{id}")
  public ResponseEntity<?> deleteStockItem(@PathVariable Long id, Authentication auth) {
    Enterprise e = getEnterprise(auth);
    StockItem existing =
        stockItemRepository.findById(id).orElseThrow(() -> new RuntimeException("StockItem not found"));

    if (existing.getEnterprise() == null || !e.getId().equals(existing.getEnterprise().getId())) {
      return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
    }

    existing.setDeleted(true);
    stockItemRepository.save(existing);
    return ResponseEntity.ok(Map.of("message", "Stock item deleted successfully"));
  }

  private String blankToNull(String s) {
    return (s == null || s.isBlank()) ? null : s;
  }
}
