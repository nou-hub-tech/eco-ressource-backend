package com.marketplace.backend.controller;

import com.marketplace.backend.dto.EnterpriseRequest;
import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.service.EnterpriseService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.Product;
import com.marketplace.backend.entity.StockItem;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.repository.EnterpriseRepository;
import com.marketplace.backend.repository.IProductRepository;
import com.marketplace.backend.repository.IStockItemRepository;
import com.marketplace.backend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;



@RestController
@RequestMapping("/api/enterprise")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class EnterpriseController {

    private final EnterpriseService enterpriseService;
    private final UserRepository userRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final IProductRepository productRepository;
    private final IStockItemRepository stockItemRepository;




    // ── Helper : récupère l'enterprise du user connecté via JWT ──
    private Enterprise getEnterprise(Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found: " + email));
        return enterpriseRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is not linked to an enterprise"));
    }


  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'ENTERPRISE')")
  public ResponseEntity<List<Enterprise>> list() {
    return ResponseEntity.ok(enterpriseService.findAll());
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Enterprise> get(@PathVariable Long id) {
    try {
      return ResponseEntity.ok(enterpriseService.getById(id));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Enterprise> create(@Valid @RequestBody EnterpriseRequest req) {
    try {
      return ResponseEntity.status(HttpStatus.CREATED).body(enterpriseService.create(req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Enterprise> update(
      @PathVariable Long id, @Valid @RequestBody EnterpriseRequest req) {
    try {
      return ResponseEntity.ok(enterpriseService.update(id, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    try {
      enterpriseService.delete(id);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }


    // ════════════════════════════════════════
    //                PRODUCTS
    // ════════════════════════════════════════

    // GET tous les produits de l'entreprise connectée
    @GetMapping("/products")
    public ResponseEntity<List<Product>> getMyProducts(Authentication auth) {
        Enterprise e = getEnterprise(auth);
        return ResponseEntity.ok(productRepository.findByEnterpriseId(e.getId()));
    }

    // GET recherche filtrée (optionnel : name, category)
    @GetMapping("/products/search")
    public ResponseEntity<List<Product>> searchMyProducts(
            Authentication auth,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category) {
        Enterprise e = getEnterprise(auth);
        return ResponseEntity.ok(
                productRepository.searchByEnterprise(e.getId(), blankToNull(name), blankToNull(category))
        );
    }

    // GET un seul produit par ID (vérifie qu'il appartient à l'entreprise)
    @GetMapping("/products/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id, Authentication auth) {
        Enterprise e = getEnterprise(auth);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        if (!e.getId().equals(product.getEnterpriseId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }
        return ResponseEntity.ok(product);
    }

    // POST créer un produit lié à l'entreprise connectée
    @PostMapping("/products")
    public ResponseEntity<?> addProduct(@RequestBody Product product, Authentication auth) {
        Enterprise e = getEnterprise(auth);
        product.setEnterprise(e);

        Product saved = productRepository.save(product);

        // Génère un barcode automatique si absent
        if (saved.getBarcode() == null || saved.getBarcode().isBlank()) {
            saved.setBarcode(String.format("200%010d", saved.getId_product()));
            saved = productRepository.save(saved);
        }

        return ResponseEntity.ok(saved);
    }

    // PUT modifier un produit (vérifie la propriété)
    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id,
                                           @RequestBody Product product,
                                           Authentication auth) {
        Enterprise e = getEnterprise(auth);
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!e.getId().equals(existing.getEnterpriseId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }

        product.setId_product(id);
        product.setEnterprise(e);

        // Conserve le barcode existant si non fourni
        if (product.getBarcode() == null || product.getBarcode().isBlank()) {
            product.setBarcode(existing.getBarcode());
        }

        return ResponseEntity.ok(productRepository.save(product));
    }

    // DELETE supprimer un produit + soft-delete ses stock items liés
    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id, Authentication auth) {
        Enterprise e = getEnterprise(auth);
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (!e.getId().equals(existing.getEnterpriseId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }

        // Soft-delete tous les stock items liés avant suppression
        List<StockItem> linkedItems = stockItemRepository.findByProductId(id);
        linkedItems.forEach(s -> s.setDeleted(true));
        stockItemRepository.saveAll(linkedItems);

        productRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Product deleted successfully"));
    }

    // ════════════════════════════════════════
    //                STOCK ITEMS
    // ════════════════════════════════════════

    // GET tous les stock items de l'entreprise connectée
    @GetMapping("/stock")
    public ResponseEntity<List<StockItem>> getMyStock(Authentication auth) {
        Enterprise e = getEnterprise(auth);
        return ResponseEntity.ok(stockItemRepository.findByEnterpriseId(e.getId()));
    }

    // GET stock paginé avec tri
    @GetMapping("/stock/paginated")
    public ResponseEntity<Map<String, Object>> getMyStockPaginated(
            Authentication auth,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "quantity") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Enterprise e = getEnterprise(auth);
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Page<StockItem> pg = stockItemRepository.findByEnterpriseId(
                e.getId(), PageRequest.of(page, size, sort));

        Map<String, Object> response = new HashMap<>();
        response.put("content", pg.getContent());
        response.put("totalPages", pg.getTotalPages());
        response.put("totalElements", pg.getTotalElements());
        response.put("currentPage", pg.getNumber());

        return ResponseEntity.ok(response);
    }

    // GET recherche filtrée (status, productName)
    @GetMapping("/stock/search")
    public ResponseEntity<List<StockItem>> searchMyStock(
            Authentication auth,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String productName) {
        Enterprise e = getEnterprise(auth);
        return ResponseEntity.ok(
                stockItemRepository.searchByEnterprise(e.getId(), blankToNull(status), blankToNull(productName))
        );
    }

    // GET valeur totale du stock de l'entreprise
    @GetMapping("/stock/total-value")
    public ResponseEntity<Map<String, Double>> getMyTotalValue(Authentication auth) {
        Enterprise e = getEnterprise(auth);
        Double val = stockItemRepository.calculateTotalStockValueByEnterprise(e.getId());
        return ResponseEntity.ok(Map.of("totalValue", val != null ? val : 0.0));
    }

    // GET un stock item par ID
    @GetMapping("/stock/{id}")
    public ResponseEntity<?> getStockItemById(@PathVariable Long id, Authentication auth) {
        Enterprise e = getEnterprise(auth);
        StockItem item = stockItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("StockItem not found"));
        if (item.getEnterprise() == null || !e.getId().equals(item.getEnterprise().getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }
        return ResponseEntity.ok(item);
    }

    // POST ajouter un stock item lié à l'entreprise connectée
    @PostMapping("/stock")
    public ResponseEntity<?> addStockItem(@RequestBody StockItem item, Authentication auth) {
        Enterprise e = getEnterprise(auth);
        item.setEnterprise(e);
        item.setDeleted(false);

        // Vérifie que le produit lié appartient bien à cette entreprise
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

    // PUT modifier un stock item
    @PutMapping("/stock/{id}")
    public ResponseEntity<?> updateStockItem(@PathVariable Long id,
                                             @RequestBody StockItem item,
                                             Authentication auth) {
        Enterprise e = getEnterprise(auth);
        StockItem existing = stockItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("StockItem not found"));

        if (existing.getEnterprise() == null || !e.getId().equals(existing.getEnterprise().getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }

        item.setIdStock(id);
        item.setEnterprise(e);
        item.setDeleted(false);

        // Recharge le produit depuis la DB si fourni
        if (item.getProduct() != null && item.getProduct().getId_product() != null) {
            productRepository.findById(item.getProduct().getId_product())
                    .ifPresent(item::setProduct);
        }

        return ResponseEntity.ok(stockItemRepository.save(item));
    }

    // DELETE soft-delete un stock item
    @DeleteMapping("/stock/{id}")
    public ResponseEntity<?> deleteStockItem(@PathVariable Long id, Authentication auth) {
        Enterprise e = getEnterprise(auth);
        StockItem existing = stockItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("StockItem not found"));

        if (existing.getEnterprise() == null || !e.getId().equals(existing.getEnterprise().getId())) {
            return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
        }

        existing.setDeleted(true);
        stockItemRepository.save(existing);
        return ResponseEntity.ok(Map.of("message", "Stock item deleted successfully"));
    }

    // ── Utilitaire : transforme chaîne vide en null pour les requêtes JPQL ──
    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    // ═══════════════════════════════════════════════════════════
    //  MARKET STOCK — items from other enterprises (or unowned)
    //  Sorted: items whose product.category matches enterprise
    //  sector come FIRST, then the rest alphabetically.
    // ═══════════════════════════════════════════════════════════
    @GetMapping("/market-stock")
    public ResponseEntity<?> getMarketStock(Authentication auth) {
        Enterprise me = getEnterprise(auth);
        String mySector = me.getSector() != null ? me.getSector().trim().toLowerCase() : "";

        List<StockItem> items = stockItemRepository.findMarketStock(me.getId());

        // Build response DTOs, priority flag = category matches my sector
        java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (StockItem s : items) {
            String productCategory = s.getProduct() != null && s.getProduct().getCategory() != null
                    ? s.getProduct().getCategory().trim().toLowerCase() : "";
            String productName = s.getProduct() != null ? s.getProduct().getName() : "Unknown";
            String productImage = s.getProduct() != null ? s.getProduct().getImage() : null;
            String material = s.getProduct() != null ? s.getProduct().getMaterialType() : null;
            boolean recyclable = s.getProduct() != null && s.getProduct().isRecyclable();
            Long ownerEnterpriseId = s.getEnterprise() != null ? s.getEnterprise().getId() : null;
            String ownerName = s.getEnterprise() != null ? s.getEnterprise().getCompanyName() : "Platform";

            boolean priorityMatch = !mySector.isEmpty() && productCategory.contains(mySector);

            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", s.getIdStock());
            item.put("productName", productName);
            item.put("productImage", productImage);
            item.put("category", s.getProduct() != null ? s.getProduct().getCategory() : null);
            item.put("materialType", material);
            item.put("recyclable", recyclable);
            item.put("quantity", s.getQuantity());
            item.put("unit", s.getUnit());
            item.put("unitPrice", s.getUnitPrice());
            item.put("status", s.getStatus());
            item.put("condition", s.getCondition());
            item.put("location", s.getLocation());
            item.put("ownerEnterpriseId", ownerEnterpriseId);
            item.put("ownerName", ownerName);
            item.put("priorityMatch", priorityMatch);
            result.add(item);
        }

        // Sort: priority matches first, then alphabetically by product name
        result.sort((a, b) -> {
            boolean pa = Boolean.TRUE.equals(a.get("priorityMatch"));
            boolean pb = Boolean.TRUE.equals(b.get("priorityMatch"));
            if (pa != pb) return pa ? -1 : 1;
            return String.valueOf(a.get("productName")).compareTo(String.valueOf(b.get("productName")));
        });

        return ResponseEntity.ok(result);
    }


    // ═══════════════════════════════════════════════════════════
    //  MARKET PRODUCTS — products from other enterprises & admin
    //  Excludes the logged-in enterprise's own products
    // ═══════════════════════════════════════════════════════════
    @GetMapping("/market-products")
    public ResponseEntity<?> getMarketProducts(Authentication auth) {
        Enterprise me = getEnterprise(auth);
        String mySector = me.getSector() != null ? me.getSector().trim().toLowerCase() : "";

        List<Product> products = productRepository.findMarketProducts(me.getId());

        java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Product p : products) {
            String productCategory = p.getCategory() != null ? p.getCategory().trim().toLowerCase() : "";
            boolean priorityMatch = !mySector.isEmpty() && productCategory.contains(mySector);
            String ownerName = p.getEnterprise() != null ? p.getEnterprise().getCompanyName() : "Platform";

            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("id", p.getId_product());
            item.put("productName", p.getName());
            item.put("productImage", p.getImage() != null && p.getImage().startsWith("http")
                    ? p.getImage()
                    : (p.getImage() != null ? "http://localhost:9090/files/" + p.getImage() : null));
            item.put("category", p.getCategory());
            item.put("materialType", p.getMaterialType());
            item.put("recyclable", p.isRecyclable());
            item.put("description", p.getDescription());
            item.put("ownerEnterpriseId", p.getEnterpriseId());
            item.put("ownerName", ownerName);
            item.put("priorityMatch", priorityMatch);

            // Include first available stock item info for reclamation form
            java.util.List<com.marketplace.backend.entity.StockItem> stocks =
                    stockItemRepository.findByProductId(p.getId_product());
            if (!stocks.isEmpty()) {
                com.marketplace.backend.entity.StockItem si = stocks.get(0);
                item.put("stockItemId",  si.getIdStock());
                item.put("stockQty",     si.getQuantity());
                item.put("stockUnit",    si.getUnit());
                item.put("unitPrice",    si.getUnitPrice());
                item.put("totalValue",   si.getQuantity() * si.getUnitPrice());
            }
            result.add(item);
        }

        // Sort: sector matches first, then alphabetically
        result.sort((a, b) -> {
            boolean pa = Boolean.TRUE.equals(a.get("priorityMatch"));
            boolean pb = Boolean.TRUE.equals(b.get("priorityMatch"));
            if (pa != pb) return pa ? -1 : 1;
            return String.valueOf(a.get("productName")).compareTo(String.valueOf(b.get("productName")));
        });

        return ResponseEntity.ok(result);
    }

}
