package com.marketplace.backend.service;

import com.marketplace.backend.dto.ProductRequest;
import com.marketplace.backend.entity.Product;
import com.marketplace.backend.repository.ProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

  private final ProductRepository productRepository;

  @Transactional(readOnly = true)
  public List<Product> findAll() {
    return productRepository.findAll();
  }

  @Transactional(readOnly = true)
  public Product getById(Long id) {
    return productRepository
        .findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Product not found"));
  }

  @Transactional(readOnly = true)
  public List<Product> findByCategory(String category) {
    return productRepository.findByCategory(category);
  }

  @Transactional
  public Product create(ProductRequest req) {
    Product p =
        Product.builder()
            .name(req.getName())
            .category(req.getCategory())
            .description(req.getDescription())
            .image(req.getImage())
            .materialType(req.getMaterialType())
            .recyclable(req.getRecyclable())
            .build();
    return productRepository.save(p);
  }

  @Transactional
  public Product update(Long id, ProductRequest req) {
    Product p =
        productRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    p.setName(req.getName());
    p.setCategory(req.getCategory());
    p.setDescription(req.getDescription());
    p.setImage(req.getImage());
    p.setMaterialType(req.getMaterialType());
    p.setRecyclable(req.getRecyclable());
    return productRepository.save(p);
  }

  @Transactional
  public void delete(Long id) {
    Product p =
        productRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    productRepository.delete(p);
  }
}
