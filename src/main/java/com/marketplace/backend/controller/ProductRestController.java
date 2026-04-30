package com.marketplace.backend.controller;

import jakarta.validation.Valid;
import com.marketplace.backend.entity.Product;
import com.marketplace.backend.service.IProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/product")
public class ProductRestController {

    private final IProductService productService;

    public ProductRestController(IProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/retrieve-all-products")
    public List<Product> getProducts() { return productService.retrieveAllProducts(); }

    @GetMapping("/retrieve-product/{id_product}")
    public Product getProduct(@PathVariable Long id_product) { return productService.retrieveProduct(id_product); }

    @PostMapping("/add-product")
    public ResponseEntity<?> addProduct(@Valid @RequestBody Product p) {
        return ResponseEntity.ok(productService.addProduct(p));
    }

    @PutMapping("/update-product")
    public ResponseEntity<?> updateProduct(@Valid @RequestBody Product p) {
        return ResponseEntity.ok(productService.updateProduct(p));
    }

    @DeleteMapping("/remove-product/{id_product}")
    public void deleteProduct(@PathVariable Long id_product) { productService.removeProduct(id_product); }

    // Search
    @GetMapping("/search")
    public List<Product> searchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String materialType) {
        return productService.searchProducts(name, category, materialType);
    }

    @GetMapping("/paginated")
    public ResponseEntity<Map<String, Object>> getProductsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        List<String> allowedSorts = List.of(
                "name",
                "category",
                "materialType"
        );

        if (!allowedSorts.contains(sortBy)) {
            sortBy = "name";
        }

        Sort sort = direction.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() :
                Sort.by(sortBy).ascending();
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        Page<Product> productPage = productService.retrieveAllProducts(pageRequest);
        Map<String, Object> response = new HashMap<>();
        response.put("content", productPage.getContent());
        response.put("totalPages", productPage.getTotalPages());
        response.put("totalElements", productPage.getTotalElements());
        response.put("currentPage", productPage.getNumber());
        return ResponseEntity.ok(response);
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
    @PostMapping("/generate-barcodes")
    public ResponseEntity<?> generateBarcodes() {
        List<Product> products = productService.retrieveAllProducts();
        for (Product p : products) {
            if (p.getBarcode() == null || p.getBarcode().isEmpty()) {
                String barcode = String.format("200%010d", p.getId_product());
                p.setBarcode(barcode);
                productService.updateProduct(p);
            }
        }
        return ResponseEntity.ok(Map.of("message", "Barcodes generated for all products"));
    }
}