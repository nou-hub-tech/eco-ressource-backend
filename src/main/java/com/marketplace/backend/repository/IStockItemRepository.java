package com.marketplace.backend.repository;

import com.marketplace.backend.entity.StockItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Repository
public interface IStockItemRepository extends JpaRepository<StockItem, Long> {

    @Query("SELECT COALESCE(SUM(s.quantity * s.unitPrice), 0) FROM StockItem s")
    Double calculateTotalStockValue();

    @Query("SELECT COALESCE(SUM(s.quantity * s.unitPrice), 0) FROM StockItem s WHERE s.product.id_product = :id_product")
    Double calculateTotalValueByProduct(@Param("id_product") Long id_product);

    @Query("SELECT s FROM StockItem s WHERE " +
            "(:status IS NULL OR LOWER(s.status) LIKE LOWER(CONCAT('%', :status, '%'))) AND " +
            "(:location IS NULL OR LOWER(s.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
            "(:productName IS NULL OR LOWER(s.product.name) LIKE LOWER(CONCAT('%', :productName, '%')))")
    List<StockItem> searchStockItems(@Param("status") String status,
                                     @Param("location") String location,
                                     @Param("productName") String productName);

    @Query("SELECT s FROM StockItem s WHERE s.expirationDate < CURRENT_DATE")
    List<StockItem> findExpiredStockItems();

    @Query("SELECT s FROM StockItem s WHERE s.expirationDate BETWEEN CURRENT_DATE AND :limitDate")
    List<StockItem> findNearExpiryStockItems(@Param("limitDate") Date limitDate);

    @Query("""
SELECT s.product.name AS product, SUM(s.quantity) AS total
FROM StockItem s
GROUP BY s.product.name
""")
    List<Map<String, Object>> getStatsByCategory();

    @Query("""
SELECT s.location AS location, SUM(s.quantity) AS total
FROM StockItem s
GROUP BY s.location
""")
    List<Map<String, Object>> getStatsByLocation();}