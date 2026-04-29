package com.marketplace.backend.repository;

import com.marketplace.backend.entity.StockItem;
import com.marketplace.backend.entity.StockMovement;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IStockMovementRepository extends JpaRepository<StockMovement, Long> {
    @Transactional

    void deleteByStockItem_IdStock(Long idStock); // ✅ already correct

    List<StockMovement> findByStockItem_IdStock(Long idStock); // ✅ already correct, used in service now

    List<StockMovement> findByStockItem(StockItem stockItem);
}