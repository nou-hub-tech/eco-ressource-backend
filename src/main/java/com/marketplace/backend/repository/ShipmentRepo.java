package com.marketplace.backend.repository;

import com.marketplace.backend.entity.Shipment;
import  com.marketplace.backend.entity.enums.StatutExpedition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShipmentRepo extends JpaRepository<Shipment, Long> {

    // ==================== EXISTANTS ====================

    List<Shipment> findByDeliveryOrderIdDelivery(Long deliveryOrderId);
    List<Shipment> findByStatut(StatutExpedition statut);

    // ==================== RECHERCHE ====================

    // Recherche par produit
    List<Shipment> findByProduitId(Long produitId);

    // Recherche par quantité (NOUVEAU)
    List<Shipment> findByQuantite(Double quantite);

    // Recherche par date (exacte)
    @Query("SELECT s FROM Shipment s WHERE DATE(s.dateDepart) = :date")
    List<Shipment> findByDate(@Param("date") LocalDate date);

    // RECHERCHE AVANCÉE avec plusieurs critères (supprimé idTransporter)
    @Query("SELECT s FROM Shipment s WHERE " +
            "(:produitId IS NULL OR s.produitId = :produitId) AND " +
            "(:quantite IS NULL OR s.quantite = :quantite) AND " +
            "(:statut IS NULL OR s.statut = :statut) AND " +
            "(:date IS NULL OR DATE(s.dateDepart) = :date)")
    List<Shipment> rechercheAvancee(
            @Param("produitId") Long produitId,
            @Param("quantite") Double quantite,
            @Param("statut") StatutExpedition statut,
            @Param("date") LocalDate date
    );

    // ==================== STATISTIQUES ====================

    // Top produits (les plus expédiés)
    @Query("SELECT s.produitId, COUNT(s), SUM(s.quantite) FROM Shipment s GROUP BY s.produitId ORDER BY COUNT(s) DESC")
    List<Object[]> findTopProduits();

    // Statistiques par statut
    @Query("SELECT s.statut, COUNT(s) FROM Shipment s GROUP BY s.statut")
    List<Object[]> countShipmentsByStatut();

    // Statistiques par commande
    @Query("SELECT s.statut, COUNT(s) FROM Shipment s WHERE s.deliveryOrder.idDelivery = :orderId GROUP BY s.statut")
    List<Object[]> countShipmentsByStatutForOrder(@Param("orderId") Long orderId);

    // Statistiques par jour
    @Query("SELECT DATE(s.dateDepart), COUNT(s) FROM Shipment s GROUP BY DATE(s.dateDepart) ORDER BY DATE(s.dateDepart) DESC")
    List<Object[]> countShipmentsByDay();

    // Statistiques par mois
    @Query("SELECT YEAR(s.dateDepart), MONTH(s.dateDepart), COUNT(s) FROM Shipment s GROUP BY YEAR(s.dateDepart), MONTH(s.dateDepart) ORDER BY YEAR(s.dateDepart) DESC, MONTH(s.dateDepart) DESC")
    List<Object[]> countShipmentsByMonth();
}