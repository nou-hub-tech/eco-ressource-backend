package com.marketplace.backend.repository;

import com.marketplace.backend.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IStockMovementRepository extends JpaRepository<StockMovement, Long> {

    @Query("SELECT m FROM StockMovement m WHERE m.stockItem.id_stock = :idStock ORDER BY m.movementDate DESC")
    List<StockMovement> findByStockItemId(@Param("idStock") Long idStock);
}