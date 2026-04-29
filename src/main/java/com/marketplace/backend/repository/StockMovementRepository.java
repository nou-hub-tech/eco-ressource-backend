package com.marketplace.backend.repository;

import com.marketplace.backend.entity.StockMovement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

  List<StockMovement> findByStockItemIdStock(Long idStock);

  List<StockMovement> findByMovementType(String movementType);
}
