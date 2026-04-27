package com.marketplace.backend.repository;

import com.marketplace.backend.entity.StockItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Repository
public interface IStockItemRepository extends JpaRepository<StockItem, Long> {

    // ✅ Get only active (non-deleted) items
    @Query("SELECT s FROM StockItem s WHERE s.deleted = false")
    List<StockItem> findAllActive();

    // ✅ Get only active items with pagination
    @Query("SELECT s FROM StockItem s WHERE s.deleted = false")
    Page<StockItem> findAllActive(Pageable pageable);

    // ✅ Find active item by ID
    @Query("SELECT s FROM StockItem s WHERE s.idStock = :id AND s.deleted = false")
    StockItem findActiveById(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(s.quantity * s.unitPrice), 0) FROM StockItem s WHERE s.deleted = false")
    Double calculateTotalStockValue();

    @Query("SELECT COALESCE(SUM(s.quantity * s.unitPrice), 0) FROM StockItem s WHERE s.product.id_product = :id_product AND s.deleted = false")
    Double calculateTotalValueByProduct(@Param("id_product") Long id_product);

    @Query("""
        SELECT s FROM StockItem s
        WHERE s.deleted = false
          AND (:status IS NULL OR LOWER(s.status) LIKE LOWER(CONCAT('%', :status, '%')))
          AND (:location IS NULL OR LOWER(s.location) LIKE LOWER(CONCAT('%', :location, '%')))
          AND (:productName IS NULL OR LOWER(s.product.name) LIKE LOWER(CONCAT('%', :productName, '%')))
    """)
    List<StockItem> searchStockItems(@Param("status") String status,
                                     @Param("location") String location,
                                     @Param("productName") String productName);

    @Query("SELECT s FROM StockItem s WHERE s.expirationDate < CURRENT_DATE AND s.deleted = false")
    List<StockItem> findExpiredStockItems();

    @Query("SELECT s FROM StockItem s WHERE s.expirationDate BETWEEN CURRENT_DATE AND :limitDate AND s.deleted = false")
    List<StockItem> findNearExpiryStockItems(@Param("limitDate") Date limitDate);

    @Query("SELECT s FROM StockItem s WHERE s.product.id_product = :id_product AND s.deleted = false")
    List<StockItem> findByProductId(@Param("id_product") Long id_product);

    @Query("""
        SELECT s.product.name AS product, SUM(s.quantity) AS total
        FROM StockItem s
        WHERE s.deleted = false
        GROUP BY s.product.name
    """)
    List<Map<String, Object>> getStatsByCategory();

    @Query("""
        SELECT s.location AS location, SUM(s.quantity) AS total
        FROM StockItem s
        WHERE s.deleted = false
        GROUP BY s.location
    """)
    List<Map<String, Object>> getStatsByLocation();

    // Advanced statistics queries
    @Query("SELECT COUNT(DISTINCT s.product.id_product) FROM StockItem s WHERE s.deleted = false")
    Long countDistinctProducts();

    @Query("SELECT AVG(s.unitPrice) FROM StockItem s WHERE s.deleted = false")
    Double getAverageUnitPrice();

    @Query("SELECT s.status, COUNT(s) FROM StockItem s WHERE s.deleted = false GROUP BY s.status")
    List<Object[]> getStatusDistribution();

    @Query("SELECT s.condition, COUNT(s) FROM StockItem s WHERE s.deleted = false GROUP BY s.condition")
    List<Object[]> getConditionDistribution();

    @Query("SELECT s.location, COUNT(s) FROM StockItem s WHERE s.deleted = false GROUP BY s.location")
    List<Object[]> getLocationDistribution();

    @Query(value = "SELECT COUNT(*) FROM stock_item s WHERE s.deleted = false AND s.expiration_date >= CURDATE() AND s.expiration_date <= DATE_ADD(CURDATE(), INTERVAL 7 DAY)", nativeQuery = true)
    Long countExpiringIn7Days();

    @Query(value = "SELECT COUNT(*) FROM stock_item s WHERE s.deleted = false AND s.expiration_date > DATE_ADD(CURDATE(), INTERVAL 30 DAY)", nativeQuery = true)
    Long countHealthyItems();

    @Query("SELECT s.product.name, SUM(s.quantity * s.unitPrice) as totalValue FROM StockItem s WHERE s.deleted = false GROUP BY s.product.name ORDER BY totalValue DESC LIMIT 5")
    List<Object[]> findTop5MostValuable();

    @Query("SELECT s.product.name, SUM(s.quantity) as totalQuantity FROM StockItem s WHERE s.deleted = false GROUP BY s.product.name ORDER BY totalQuantity DESC LIMIT 5")
    List<Object[]> findTop5HighestQuantity();

    @Query("SELECT s.location, SUM(s.quantity * s.unitPrice) as totalValue FROM StockItem s WHERE s.deleted = false GROUP BY s.location")
    List<Object[]> getValueByLocation();

    @Query(value = "SELECT COUNT(*) FROM stock_movement m WHERE m.movement_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)", nativeQuery = true)
    Integer countMovementsLast30Days();

    @Query("SELECT m.movementType, COUNT(m) FROM StockMovement m GROUP BY m.movementType")
    List<Object[]> getMovementsByType();
    // Stock d'une entreprise (liste)
    @Query("SELECT s FROM StockItem s WHERE s.enterprise.id = :enterpriseId AND s.deleted = false")
    List<StockItem> findByEnterpriseId(@Param("enterpriseId") Long enterpriseId);

    // Stock d'une entreprise (paginé)
    @Query("SELECT s FROM StockItem s WHERE s.enterprise.id = :enterpriseId AND s.deleted = false")
    Page<StockItem> findByEnterpriseId(@Param("enterpriseId") Long enterpriseId, Pageable pageable);

    // Recherche filtrée
    @Query("""
    SELECT s FROM StockItem s
    WHERE s.enterprise.id = :enterpriseId AND s.deleted = false
      AND (:status IS NULL OR LOWER(s.status) LIKE LOWER(CONCAT('%', :status, '%')))
      AND (:productName IS NULL OR LOWER(s.product.name) LIKE LOWER(CONCAT('%', :productName, '%')))
""")
    List<StockItem> searchByEnterprise(@Param("enterpriseId") Long enterpriseId,
                                       @Param("status") String status,
                                       @Param("productName") String productName);

    // Valeur totale du stock d'une entreprise
    @Query("SELECT COALESCE(SUM(s.quantity * s.unitPrice), 0) FROM StockItem s WHERE s.enterprise.id = :enterpriseId AND s.deleted = false")
    Double calculateTotalStockValueByEnterprise(@Param("enterpriseId") Long enterpriseId);

    // ── Market stock: items NOT owned by a given enterprise (incl. null owner), non-deleted ──
    @Query("""
        SELECT s FROM StockItem s
        WHERE s.deleted = false
          AND (s.enterprise IS NULL OR s.enterprise.id <> :enterpriseId)
    """)
    List<StockItem> findMarketStock(@Param("enterpriseId") Long enterpriseId);

}