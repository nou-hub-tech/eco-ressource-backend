package com.marketplace.backend.repository;

import com.marketplace.backend.entity.Product;
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
}