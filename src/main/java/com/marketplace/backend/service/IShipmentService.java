package com.marketplace.backend.service;

import com.marketplace.backend.entity.Shipment;
import  com.marketplace.backend.entity.enums.StatutExpedition;
import java.util.List;
import java.util.Map;

public interface IShipmentService {

    // ==================== CRUD ====================

    public List<Shipment> retrieveAllShipments();
    public Shipment retrieveShipment(Long id);
    public Shipment addShipment(Shipment s);
    public void removeShipment(Long id);
    public Shipment modifyShipment(Shipment s);
    public List<Shipment> getShipmentsByDeliveryOrder(Long deliveryOrderId);
    public List<Shipment> getShipmentsByStatut(StatutExpedition statut);
    public Shipment updateShipment(Long id, Shipment s);

    // ==================== RECHERCHE ====================

    List<Shipment> findByProduitId(Long produitId);
    List<Shipment> findByQuantite(Double quantite);
    List<Shipment> findByDate(String date);
    List<Shipment> rechercheAvancee(Long produitId, Double quantite,
                                    StatutExpedition statut, String date);

    // ==================== TRI ====================

    List<Shipment> sortByDate(String order);
    List<Shipment> sortByQuantite(String order);

    // ==================== STATISTIQUES ====================

    Map<String, Object> getStatistiques();
    Map<String, Object> getStatistiquesByCommande(Long deliveryOrderId);
    Map<String, Long> getStatistiquesParJour();
    Map<String, Long> getStatistiquesParMois();
}
