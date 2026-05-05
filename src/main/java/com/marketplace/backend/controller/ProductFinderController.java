package com.marketplace.backend.controller;

import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.Product;
import com.marketplace.backend.entity.StockItem;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.repository.EnterpriseRepository;
import com.marketplace.backend.repository.IProductRepository;
import com.marketplace.backend.repository.IStockItemRepository;
import com.marketplace.backend.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/enterprise/finder")
public class ProductFinderController {

    private final IProductRepository   productRepo;
    private final IStockItemRepository stockItemRepo;
    private final UserRepository       userRepo;
    private final EnterpriseRepository enterpriseRepo;

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    public ProductFinderController(
            IProductRepository   productRepo,
            IStockItemRepository stockItemRepo,
            UserRepository       userRepo,
            EnterpriseRepository enterpriseRepo) {
        this.productRepo    = productRepo;
        this.stockItemRepo  = stockItemRepo;
        this.userRepo       = userRepo;
        this.enterpriseRepo = enterpriseRepo;
    }

    private Long getMyEnterpriseId(Authentication auth) {
        try {
            String email = auth.getName();
            User user = userRepo.findByEmail(email).orElse(null);
            if (user == null) return null;
            return enterpriseRepo.findByUserId(user.getId())
                    .map(Enterprise::getId)
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * GET /api/enterprise/finder/market
     * Returns ALL products from OTHER enterprises with available stock.
     * Also includes products with no enterprise (enterprise_id = NULL).
     */
    @GetMapping("/market")
    public ResponseEntity<?> market(
            Authentication auth,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice) {

        Long myEnterpriseId = getMyEnterpriseId(auth);

        // Include ALL products EXCEPT the logged-in user's own products
        List<Product> allProducts = productRepo.findAll().stream()
                .filter(p -> {
                    Long pid = p.getEnterpriseId();
                    // Include if: no enterprise (null) OR different enterprise than mine
                    if (pid == null) return true;
                    return myEnterpriseId == null || !pid.equals(myEnterpriseId);
                })
                .collect(Collectors.toList());

        // Apply search filter
        if (search != null && !search.trim().isEmpty()) {
            String q = search.trim().toLowerCase();
            allProducts = allProducts.stream()
                    .filter(p -> p.getName() != null && p.getName().toLowerCase().contains(q)
                            || (p.getBarcode()     != null && p.getBarcode().toLowerCase().contains(q))
                            || (p.getCategory()    != null && p.getCategory().toLowerCase().contains(q))
                            || (p.getDescription() != null && p.getDescription().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
        }

        // Apply category filter
        if (category != null && !category.trim().isEmpty()) {
            allProducts = allProducts.stream()
                    .filter(p -> category.equalsIgnoreCase(p.getCategory()))
                    .collect(Collectors.toList());
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (Product p : allProducts) {

            // Get ALL non-deleted stock for this product (not just "available")
            List<StockItem> stocks = stockItemRepo.findAll().stream()
                    .filter(s -> s.getProduct() != null
                            && s.getProduct().getId_product().equals(p.getId_product())
                            && !s.isDeleted())
                    .collect(Collectors.toList());

            int totalQty = stocks.stream().mapToInt(StockItem::getQuantity).sum();

            // Include even products with 0 stock so they appear in the finder
            double minUnitPrice = stocks.stream().mapToDouble(StockItem::getUnitPrice).min().orElse(0.0);

            // Price filter only applies if there is stock with a price
            if (minPrice != null && !stocks.isEmpty() && minUnitPrice < minPrice) continue;
            if (maxPrice != null && !stocks.isEmpty() && minUnitPrice > maxPrice) continue;

            // Earliest expiry date
            String earliestExpiry = stocks.stream()
                    .map(StockItem::getExpirationDate)
                    .filter(Objects::nonNull)
                    .min(Comparator.naturalOrder())
                    .map(d -> sdf.format(d))
                    .orElse(null);

            Enterprise seller = p.getEnterprise();

            // Build seller info safely
            Map<String, Object> sellerInfo;
            if (seller != null) {
                String city = (seller.getUser() != null && seller.getUser().getCity() != null)
                        ? seller.getUser().getCity() : "";
                sellerInfo = new LinkedHashMap<>();
                sellerInfo.put("id",          seller.getId());
                sellerInfo.put("companyName", seller.getCompanyName() != null ? seller.getCompanyName() : "Enterprise");
                sellerInfo.put("sector",      seller.getSector() != null ? seller.getSector() : "");
                sellerInfo.put("city",        city);
            } else {
                sellerInfo = new LinkedHashMap<>();
                sellerInfo.put("id",          0);
                sellerInfo.put("companyName", "Platform");
                sellerInfo.put("sector",      "");
                sellerInfo.put("city",        "");
            }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("product",        p);
            row.put("totalQty",       totalQty);
            row.put("minUnitPrice",   minUnitPrice);
            row.put("stockLines",     stocks.size());
            row.put("conditions",     stocks.stream().map(StockItem::getCondition).filter(Objects::nonNull).distinct().collect(Collectors.toList()));
            row.put("locations",      stocks.stream().map(StockItem::getLocation).filter(Objects::nonNull).distinct().collect(Collectors.toList()));
            row.put("earliestExpiry", earliestExpiry);
            row.put("seller",         sellerInfo);

            result.add(row);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/enterprise/finder/market/{barcode}
     * Find a product by barcode (excluding own products).
     */
    @GetMapping("/market/{barcode}")
    public ResponseEntity<?> findByBarcode(@PathVariable String barcode, Authentication auth) {
        Long myEnterpriseId = getMyEnterpriseId(auth);

        Optional<Product> opt = productRepo.findAll().stream()
                .filter(p -> barcode.equals(p.getBarcode()))
                .filter(p -> {
                    Long pid = p.getEnterpriseId();
                    if (pid == null) return true;
                    return myEnterpriseId == null || !pid.equals(myEnterpriseId);
                })
                .findFirst();

        if (opt.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(Map.of("error", "No product found for barcode: " + barcode));
        }

        Product p = opt.get();
        List<StockItem> stocks = stockItemRepo.findAll().stream()
                .filter(s -> s.getProduct() != null
                        && s.getProduct().getId_product().equals(p.getId_product())
                        && !s.isDeleted())
                .collect(Collectors.toList());

        Enterprise seller = p.getEnterprise();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("product", p);
        result.put("stocks",  stocks);

        if (seller != null) {
            String city = (seller.getUser() != null && seller.getUser().getCity() != null)
                    ? seller.getUser().getCity() : "";
            Map<String, Object> sellerInfo = new LinkedHashMap<>();
            sellerInfo.put("id",          seller.getId());
            sellerInfo.put("companyName", seller.getCompanyName() != null ? seller.getCompanyName() : "Enterprise");
            sellerInfo.put("sector",      seller.getSector() != null ? seller.getSector() : "");
            sellerInfo.put("city",        city);
            result.put("seller", sellerInfo);
        } else {
            result.put("seller", Map.of("id", 0, "companyName", "Platform", "sector", "", "city", ""));
        }

        return ResponseEntity.ok(result);
    }
}