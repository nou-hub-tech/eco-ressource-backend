package com.marketplace.backend.repository;

import com.marketplace.backend.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByBarcode(String barcode);

    @Query("SELECT p FROM Product p WHERE " +
            "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:category IS NULL OR LOWER(p.category) LIKE LOWER(CONCAT('%', :category, '%'))) AND " +
            "(:materialType IS NULL OR LOWER(p.materialType) LIKE LOWER(CONCAT('%', :materialType, '%')))")
    List<Product> searchProducts(@Param("name") String name,
                                 @Param("category") String category,
                                 @Param("materialType") String materialType);

    Page<Product> findAll(Pageable pageable);

    // ✅ Requête explicite — obligatoire car le champ s'appelle "enterprise" pas "enterpriseId"
    @Query("SELECT p FROM Product p WHERE p.enterprise.id = :enterpriseId")
    List<Product> findByEnterpriseId(@Param("enterpriseId") Long enterpriseId);

    @Query("SELECT p FROM Product p WHERE p.enterprise.id = :enterpriseId AND " +
            "(:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:category IS NULL OR LOWER(p.category) LIKE LOWER(CONCAT('%', :category, '%')))")
    List<Product> searchByEnterprise(@Param("enterpriseId") Long enterpriseId,
                                     @Param("name") String name,
                                     @Param("category") String category);

    // Market products: eagerly fetch enterprise in one query to avoid N+1, exclude own products
    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.enterprise e WHERE (p.enterprise IS NULL OR e.id <> :enterpriseId)")
    List<Product> findMarketProducts(@Param("enterpriseId") Long enterpriseId);
}