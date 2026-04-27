package com.marketplace.backend.repository;

import com.marketplace.backend.entity.Reclamation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ReclamationRepository extends JpaRepository<Reclamation, Long> {

    List<Reclamation> findByEnterpriseId(Long enterpriseId);

    List<Reclamation> findByEnterpriseIdAndStatus(Long enterpriseId, String status);

    @Query("SELECT r FROM Reclamation r WHERE r.enterprise.id = :enterpriseId ORDER BY r.createdAt DESC")
    List<Reclamation> findByEnterpriseIdOrderByCreatedAtDesc(@Param("enterpriseId") Long enterpriseId);

    // Reclamations received by this enterprise (about their stock)
    @Query("SELECT r FROM Reclamation r LEFT JOIN FETCH r.enterprise LEFT JOIN FETCH r.stockItem LEFT JOIN FETCH r.product WHERE r.targetEnterprise.id = :enterpriseId ORDER BY r.createdAt DESC")
    List<Reclamation> findReceivedByEnterpriseId(@Param("enterpriseId") Long enterpriseId);
}