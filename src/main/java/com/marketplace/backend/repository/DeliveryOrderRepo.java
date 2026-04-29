package com.marketplace.backend.repository;

import com.marketplace.backend.entity.DeliveryOrder;
import com.marketplace.backend.entity.enums.StatutCommande;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryOrderRepo extends JpaRepository<DeliveryOrder, Long> {

    // ==================== EXISTANTS ====================

    List<DeliveryOrder> findByStatut(StatutCommande statut);
    List<DeliveryOrder> findByNomClientContainingIgnoreCase(String nomClient);

    // ==================== RECHERCHE PAR ADRESSE ====================

    // Recherche par adresse (contient)
    List<DeliveryOrder> findByAdresseLivraisonContainingIgnoreCase(String adresse);

    // ==================== RECHERCHE PAR DATE ====================

    //Recherche par date (date exacte - ignore l'heure)
    @Query("SELECT d FROM DeliveryOrder d WHERE DATE(d.datePrevue) = :date")
    List<DeliveryOrder> findByDate(@Param("date") LocalDate date);

    //Recherche par intervalle de dates (pour statistiques)
    List<DeliveryOrder> findByDatePrevueBetween(LocalDateTime start, LocalDateTime end);

    // ==================== COMMANDES EN RETARD ====================

    @Query("SELECT d FROM DeliveryOrder d WHERE d.statut != 'LIVREE' AND d.datePrevue < :now")
    List<DeliveryOrder> findLateOrders(@Param("now") LocalDateTime now);

    // ==================== STATISTIQUES ====================

    @Query("SELECT d.statut, COUNT(d) FROM DeliveryOrder d GROUP BY d.statut")
    List<Object[]> countOrdersByStatut();

    // ==================== RECHERCHE AVANCÉE ====================

    @Query("SELECT d FROM DeliveryOrder d WHERE " +
            "(:nomClient IS NULL OR LOWER(d.nomClient) LIKE LOWER(CONCAT('%', :nomClient, '%'))) AND " +
            "(:adresse IS NULL OR LOWER(d.adresseLivraison) LIKE LOWER(CONCAT('%', :adresse, '%'))) AND " +
            "(:statut IS NULL OR d.statut = :statut) AND " +
            "(:date IS NULL OR DATE(d.datePrevue) = :date)")
    List<DeliveryOrder> rechercheAvancee(
            @Param("nomClient") String nomClient,
            @Param("adresse") String adresse,
            @Param("statut") StatutCommande statut,
            @Param("date") LocalDate date
    );
}
