package com.marketplace.backend.service;

import com.marketplace.backend.dto.StockItemRequest;
import com.marketplace.backend.entity.Product;
import com.marketplace.backend.entity.StockItem;
import com.marketplace.backend.repository.ProductRepository;
import com.marketplace.backend.repository.StockItemRepository;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockItemService {

  private final StockItemRepository stockItemRepository;
  private final ProductRepository productRepository;

  @Transactional(readOnly = true)
  public List<StockItem> findAll() {
    return stockItemRepository.findAll();
  }

  @Transactional(readOnly = true)
  public StockItem getById(Long id) {
    return stockItemRepository
        .findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Stock item not found"));
  }

  @Transactional(readOnly = true)
  public List<StockItem> findByProductId(Long idProduct) {
    return stockItemRepository.findByProductIdProduct(idProduct);
  }

  @Transactional(readOnly = true)
  public List<StockItem> findByCompanyId(Long companyId) {
    return stockItemRepository.findByCompanyId(companyId);
  }

  @Transactional
  public StockItem create(StockItemRequest req) {
    Product product = null;
    if (req.getIdProduct() != null) {
      product =
          productRepository
              .findById(req.getIdProduct())
              .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }

    StockItem s =
        StockItem.builder()
            .companyId(req.getCompanyId())
            .condition(req.getItemCondition())
            .expirationDate(toUtilDate(req.getExpirationDate()))
            .image(req.getImage())
            .location(req.getLocation())
            .quantity(req.getQuantity())
            .status(req.getStatus())
            .unit(req.getUnit())
            .product(product)
            .unitPrice(req.getUnitPrice())
            .deleted(false)
            .build();
    return stockItemRepository.save(s);
  }

  @Transactional
  public StockItem update(Long id, StockItemRequest req) {
    StockItem s =
        stockItemRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Stock item not found"));

    Product product = null;
    if (req.getIdProduct() != null) {
      product =
          productRepository
              .findById(req.getIdProduct())
              .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }

    s.setCompanyId(req.getCompanyId());
    s.setCondition(req.getItemCondition());
    s.setExpirationDate(toUtilDate(req.getExpirationDate()));
    s.setImage(req.getImage());
    s.setLocation(req.getLocation());
    s.setQuantity(req.getQuantity());
    s.setStatus(req.getStatus());
    s.setUnit(req.getUnit());
    s.setProduct(product);
    s.setUnitPrice(req.getUnitPrice());
    return stockItemRepository.save(s);
  }

  @Transactional
  private static java.util.Date toUtilDate(LocalDate ld) {
    if (ld == null) {
      return null;
    }
    return Date.valueOf(ld);
  }

  public void delete(Long id) {
    StockItem s =
        stockItemRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Stock item not found"));
    stockItemRepository.delete(s);
  }
}
