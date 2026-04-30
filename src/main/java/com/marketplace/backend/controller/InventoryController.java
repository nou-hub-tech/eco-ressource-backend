package com.marketplace.backend.controller;

import com.marketplace.backend.dto.ScanRequest;
import com.marketplace.backend.entity.InventoryScan;
import com.marketplace.backend.entity.Product;
import com.marketplace.backend.entity.StockItem;
import com.marketplace.backend.repository.IInventoryScanRepository;
import com.marketplace.backend.repository.IProductRepository;
import com.marketplace.backend.repository.IStockItemRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final IProductRepository productRepo;
    private final IStockItemRepository stockItemRepo;
    private final IInventoryScanRepository scanRepo;

    public InventoryController(IProductRepository productRepo,
                               IStockItemRepository stockItemRepo,
                               IInventoryScanRepository scanRepo) {
        this.productRepo = productRepo;
        this.stockItemRepo = stockItemRepo;
        this.scanRepo = scanRepo;
    }

    // POST /inventory/scan — compare real vs digital state
    @PostMapping("/scan")
    public ResponseEntity<?> scan(@RequestBody ScanRequest req) {
        Optional<Product> opt = productRepo.findByBarcode(req.getBarcode());
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "No product found for barcode: " + req.getBarcode()));
        }

        Product product = opt.get();

        // Get digital state from stock
        List<StockItem> stocks = stockItemRepo.findAll().stream()
                .filter(s -> s.getProduct() != null && s.getProduct().getId_product().equals(product.getId_product()))
                .toList();

        StockItem stock = stocks.isEmpty() ? null : stocks.get(0);

        int dbQty = stock != null ? stock.getQuantity() : 0;
        String dbCondition = stock != null && stock.getCondition() != null ? stock.getCondition() : "Unknown";
        String dbLocation = stock != null && stock.getLocation() != null ? stock.getLocation() : "Unknown";

        // Build diff
        List<Map<String, Object>> diff = new ArrayList<>();
        diff.add(buildDiffRow("Quantity", dbQty, req.getRealQty()));
        diff.add(buildDiffRow("Condition", dbCondition, req.getRealCondition()));
        diff.add(buildDiffRow("Location", dbLocation, req.getRealLocation()));

        long mismatches = diff.stream().filter(d -> !(boolean) d.get("match")).count();

        // Save scan to history
        InventoryScan scan = new InventoryScan();
        scan.setBarcode(req.getBarcode());
        scan.setRealQty(req.getRealQty());
        scan.setRealCondition(req.getRealCondition());
        scan.setRealLocation(req.getRealLocation());
        scan.setScannedAt(new Date());
        scan.setProduct(product);
        scanRepo.save(scan);

        // Build response
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("product", product);
        result.put("digital", Map.of("qty", dbQty, "condition", dbCondition, "location", dbLocation));
        result.put("real", Map.of("qty", req.getRealQty(), "condition", req.getRealCondition(), "location", req.getRealLocation()));
        result.put("diff", diff);
        result.put("mismatches", mismatches);
        result.put("expirationDate", stock != null ? stock.getExpirationDate() : null);
        return ResponseEntity.ok(result);
    }

    // GET /inventory/history — all scan history
    @GetMapping("/history")
    public ResponseEntity<?> history() {
        return ResponseEntity.ok(scanRepo.findAllByOrderByScannedAtDesc());
    }

    // GET /inventory/history/{barcode} — history for one product
    @GetMapping("/history/{barcode}")
    public ResponseEntity<?> historyByBarcode(@PathVariable String barcode) {
        return ResponseEntity.ok(scanRepo.findByBarcodeOrderByScannedAtDesc(barcode));
    }

    // GET /inventory/all-with-stock — all products with their digital state
    @GetMapping("/all-with-stock")
    public ResponseEntity<?> allWithStock() {
        List<Product> products = productRepo.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Product p : products) {
            List<StockItem> stocks = stockItemRepo.findAll().stream()
                    .filter(s -> s.getProduct() != null && s.getProduct().getId_product().equals(p.getId_product()))
                    .toList();
            StockItem stock = stocks.isEmpty() ? null : stocks.get(0);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("product", p);
            row.put("qty", stock != null ? stock.getQuantity() : 0);
            row.put("condition", stock != null && stock.getCondition() != null ? stock.getCondition() : "Unknown");
            row.put("location", stock != null && stock.getLocation() != null ? stock.getLocation() : "Unknown");
            row.put("expirationDate", stock != null ? stock.getExpirationDate() : null); // ← from StockItem
            result.add(row);
        }
        return ResponseEntity.ok(result);
    }

    private Map<String, Object> buildDiffRow(String field, Object db, Object real) {
        boolean match = String.valueOf(db).equalsIgnoreCase(String.valueOf(real));
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("field", field);
        row.put("digital", db);
        row.put("real", real);
        row.put("match", match);
        return row;
    }
}