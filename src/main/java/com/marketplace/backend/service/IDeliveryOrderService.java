package com.marketplace.backend.service;



import com.marketplace.backend.entity.DeliveryOrder;
import com.marketplace.backend.entity.enums.StatutCommande;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface IDeliveryOrderService {

    // ==================== CRUD ====================

    public List<DeliveryOrder> retrieveAllDeliveryOrders();
    public DeliveryOrder retrieveDeliveryOrder(Long id);
    public DeliveryOrder addDeliveryOrder(DeliveryOrder d);
    public void removeDeliveryOrder(Long id);
    public DeliveryOrder modifyDeliveryOrder(DeliveryOrder d);
    public List<DeliveryOrder> getDeliveryOrdersByStatut(StatutCommande statut);
    public DeliveryOrder updateStatut(Long id, StatutCommande statut);
    public DeliveryOrder updateDeliveryOrder(Long id, DeliveryOrder d);

    // ==================== RECHERCHE ====================

    List<DeliveryOrder> findByNomClientContaining(String nomClient);
    List<DeliveryOrder> findByAdresseContaining(String adresse);
    List<DeliveryOrder> findByDate(String date);
    List<DeliveryOrder> rechercheAvancee(String nomClient, String adresse,
                                         StatutCommande statut, String date);

    // ==================== TRI ====================

    List<DeliveryOrder> sortByDate(String order);
    List<DeliveryOrder> sortByNomClient(String order);

    // ==================== STATISTIQUES ====================

    Map<String, Object> getStatistiques();
    Map<String, Long> getStatistiquesParMois();
    Map<String, Long> getStatistiquesParSemaine();
    Map<String, Long> getStatistiquesParAnnee();
}
