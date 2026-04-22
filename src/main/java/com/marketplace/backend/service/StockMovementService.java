package com.marketplace.backend.service;

import com.marketplace.backend.dto.StockMovementRequest;
import com.marketplace.backend.entity.StockItem;
import com.marketplace.backend.entity.StockMovement;
import com.marketplace.backend.repository.StockItemRepository;
import com.marketplace.backend.repository.StockMovementRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StockMovementService {

  private final StockMovementRepository stockMovementRepository;
  private final StockItemRepository stockItemRepository;

  @Transactional(readOnly = true)
  public List<StockMovement> findAll() {
    return stockMovementRepository.findAll();
  }

  @Transactional(readOnly = true)
  public StockMovement getById(Long id) {
    return stockMovementRepository
        .findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Stock movement not found"));
  }

  @Transactional(readOnly = true)
  public List<StockMovement> findByStockItemId(Long idStock) {
    return stockMovementRepository.findByStockItemIdStock(idStock);
  }

  @Transactional
  public StockMovement create(StockMovementRequest req) {
    StockItem stockItem = null;
    if (req.getIdStock() != null) {
      stockItem =
          stockItemRepository
              .findById(req.getIdStock())
              .orElseThrow(() -> new IllegalArgumentException("Stock item not found"));
    }

    StockMovement m =
        StockMovement.builder()
            .description(req.getDescription())
            .movementDate(
                req.getMovementDate() != null ? req.getMovementDate() : LocalDateTime.now())
            .movementType(req.getMovementType())
            .quantity(req.getQuantity())
            .stockItem(stockItem)
            .status("ACTIVE")
            .build();
    return stockMovementRepository.save(m);
  }

  @Transactional
  public StockMovement update(Long id, StockMovementRequest req) {
    StockMovement m =
        stockMovementRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Stock movement not found"));

    StockItem stockItem = null;
    if (req.getIdStock() != null) {
      stockItem =
          stockItemRepository
              .findById(req.getIdStock())
              .orElseThrow(() -> new IllegalArgumentException("Stock item not found"));
    }

    m.setDescription(req.getDescription());
    m.setMovementDate(
        req.getMovementDate() != null ? req.getMovementDate() : m.getMovementDate());
    m.setMovementType(req.getMovementType());
    m.setQuantity(req.getQuantity());
    m.setStockItem(stockItem);
    return stockMovementRepository.save(m);
  }

  @Transactional
  public void delete(Long id) {
    StockMovement m =
        stockMovementRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Stock movement not found"));
    stockMovementRepository.delete(m);
  }
}
