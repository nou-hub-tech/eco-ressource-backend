package com.marketplace.backend.controller;

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
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/enterprise")
@CrossOrigin(origins = "http://localhost:4200")
public class EnterpriseController {

    private final UserRepository userRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final IProductRepository productRepository;
    private final IStockItemRepository stockItemRepository;

    // ── Constructeur (injection par constructeur) ──
    public EnterpriseController(UserRepository userRepository,
                                EnterpriseRepository enterpriseRepository,
                                IProductRepository productRepository,
                                IStockItemRepository stockItemRepository) {
        this.userRepository = userRepository;
        this.enterpriseRepository = enterpriseRepository;
        this.productRepository = productRepository;
        this.stockItemRepository = stockItemRepository;
    }

    // ── Helper : récupère l'enterprise du user connecté via JWT ──
    private Enterprise getEnterprise(Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        return enterpriseRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("No enterprise linked to this user"));
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
}