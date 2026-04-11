package com.marketplace.backend.controller;

import jakarta.validation.Valid;
import com.marketplace.backend.entity.StockItem;
import com.marketplace.backend.service.IProductService;
import com.marketplace.backend.service.IStockItemService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/stockitem")
@CrossOrigin(origins = "http://localhost:4200")
public class StockItemRestController {

    private final IStockItemService stockItemService;
    private final IProductService productService;

    public StockItemRestController(IStockItemService stockItemService, IProductService productService) {
        this.stockItemService = stockItemService;
        this.productService = productService;
    }

    @GetMapping("/retrieve-all-stockitems")
    public List<StockItem> getStockItems() { return stockItemService.retrieveAllStockItems(); }

    @GetMapping("/retrieve-stockitem/{id_stock}")
    public StockItem getStockItem(@PathVariable Long id_stock) {
        return stockItemService.retrieveStockItem(id_stock);
    }

    @PostMapping("/add-stockitem")
    public ResponseEntity<?> addStockItem(@Valid @RequestBody StockItem s) {
        return ResponseEntity.ok(stockItemService.addStockItem(s));
    }

    @PutMapping("/update-stockitem")
    public ResponseEntity<?> updateStockItem(@Valid @RequestBody StockItem s) {
        return ResponseEntity.ok(stockItemService.updateStockItem(s));
    }

    @DeleteMapping("/remove-stockitem/{id_stock}")
    public void deleteStockItem(@PathVariable Long id_stock) {
        stockItemService.removeStockItem(id_stock);
    }

    @PutMapping("/assign-stockitem-to-product/{id_stock}/{id_product}")
    public StockItem assignStockItemToProduct(@PathVariable Long id_stock,
                                              @PathVariable Long id_product) {
        return stockItemService.assignStockItemToProduct(id_stock, id_product);
    }

    @GetMapping("/total-value")
    public ResponseEntity<Map<String, Double>> getTotalStockValue() {
        Map<String, Double> response = new HashMap<>();
        response.put("totalValue", stockItemService.getTotalStockValue());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/total-value/{id_product}")
    public ResponseEntity<Map<String, Double>> getTotalValueByProduct(@PathVariable Long id_product) {
        Map<String, Double> response = new HashMap<>();
        response.put("totalValue", stockItemService.getTotalValueByProduct(id_product));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public List<StockItem> searchStockItems(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String productName) {
        return stockItemService.searchStockItems(status, location, productName);
    }

    @GetMapping("/stats/category")
    public ResponseEntity<List<Map<String, Object>>> getStatsByCategory() {
        return ResponseEntity.ok(stockItemService.getStatsByCategory());
    }

    @GetMapping("/stats/location")
    public ResponseEntity<List<Map<String, Object>>> getStatsByLocation() {
        return ResponseEntity.ok(stockItemService.getStatsByLocation());
    }

    @GetMapping("/expired")
    public ResponseEntity<List<StockItem>> getExpiredStockItems() {
        return ResponseEntity.ok(stockItemService.getExpiredStockItems());
    }

    @GetMapping("/near-expiry/{days}")
    public ResponseEntity<List<StockItem>> getNearExpiryStockItems(@PathVariable int days) {
        return ResponseEntity.ok(stockItemService.getNearExpiryStockItems(days));
    }

    @GetMapping("/paginated")
    public Page<StockItem> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "quantity") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        List<String> allowedSorts = List.of("quantity", "unitPrice", "location", "status");
        if (!allowedSorts.contains(sortBy)) sortBy = "quantity";

        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        return stockItemService.retrieveAllStockItems(pageable);
    }

    // ✅ NEW: Import from Excel
    @PostMapping("/import")
    public ResponseEntity<?> importFromExcel(@RequestBody List<Map<String, Object>> rows) {
        List<StockItem> saved = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> row = rows.get(i);
            try {
                StockItem item = new StockItem();
                item.setQuantity(Integer.parseInt(row.get("Quantity").toString()));
                item.setUnitPrice(Double.parseDouble(row.get("Unit Price (DT)").toString()));
                item.setUnit(row.getOrDefault("Unit", "").toString());
                item.setStatus(row.getOrDefault("Status", "").toString());
                item.setLocation(row.getOrDefault("Location", "").toString());
                item.setCondition(row.getOrDefault("Condition", "").toString());

                String productName = row.getOrDefault("Product", "").toString();
                productService.retrieveAllProducts().stream()
                        .filter(p -> p.getName().equalsIgnoreCase(productName))
                        .findFirst()
                        .ifPresent(item::setProduct);

                saved.add(stockItemService.addStockItem(item));
            } catch (Exception e) {
                errors.add("Row " + (i + 1) + ": " + e.getMessage());
            }
        }

        return ResponseEntity.ok(Map.of("saved", saved.size(), "errors", errors));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });
        return ResponseEntity.badRequest().body(errors);
    }
}