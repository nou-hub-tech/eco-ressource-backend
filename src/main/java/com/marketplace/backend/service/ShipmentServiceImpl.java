package com.marketplace.backend.service;

import com.marketplace.backend.entity.Shipment;
import  com.marketplace.backend.entity.enums.StatutExpedition;
import com.marketplace.backend.repository.ShipmentRepo;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ShipmentServiceImpl implements IShipmentService {

    private final ShipmentRepo shipmentRepo;

    public ShipmentServiceImpl(ShipmentRepo shipmentRepo) {
        this.shipmentRepo = shipmentRepo;
    }

    // ==================== CRUD ====================

    @Override
    public List<Shipment> retrieveAllShipments() {
        return shipmentRepo.findAll();
    }

    @Override
    public Shipment retrieveShipment(Long id) {
        return shipmentRepo.findById(id).orElse(null);
    }

    @Override
    public Shipment addShipment(Shipment s) {
        if (s.getStatut() == null) {
            s.setStatut(StatutExpedition.EN_ATTENTE);
        }
        return shipmentRepo.save(s);
    }

    @Override
    public void removeShipment(Long id) {
        shipmentRepo.deleteById(id);
    }

    @Override
    public Shipment modifyShipment(Shipment s) {
        return shipmentRepo.save(s);
    }

    @Override
    public Shipment updateShipment(Long id, Shipment s) {
        s.setId(id);
        return shipmentRepo.save(s);
    }

    @Override
    public List<Shipment> getShipmentsByDeliveryOrder(Long deliveryOrderId) {
        return shipmentRepo.findByDeliveryOrderIdDelivery(deliveryOrderId);
    }

    @Override
    public List<Shipment> getShipmentsByStatut(StatutExpedition statut) {
        return shipmentRepo.findByStatut(statut);
    }

    // ==================== RECHERCHE ====================

    @Override
    public List<Shipment> findByProduitId(Long produitId) {
        return shipmentRepo.findByProduitId(produitId);
    }

    @Override
    public List<Shipment> findByQuantite(Double quantite) {
        return shipmentRepo.findByQuantite(quantite);
    }

    @Override
    public List<Shipment> findByDate(String date) {
        LocalDate localDate = LocalDate.parse(date);
        return shipmentRepo.findByDate(localDate);
    }

    @Override
    public List<Shipment> rechercheAvancee(Long produitId, Double quantite,
                                           StatutExpedition statut, String date) {
        LocalDate localDate = null;
        if (date != null && !date.isEmpty()) {
            localDate = LocalDate.parse(date);
        }
        return shipmentRepo.rechercheAvancee(produitId, quantite, statut, localDate);
    }

    // ==================== TRI ====================

    @Override
    public List<Shipment> sortByDate(String order) {
        List<Shipment> shipments = shipmentRepo.findAll();
        if ("asc".equalsIgnoreCase(order)) {
            shipments.sort(Comparator.comparing(Shipment::getDateDepart));
        } else {
            shipments.sort(Comparator.comparing(Shipment::getDateDepart).reversed());
        }
        return shipments;
    }

    @Override
    public List<Shipment> sortByQuantite(String order) {
        List<Shipment> shipments = shipmentRepo.findAll();
        if ("asc".equalsIgnoreCase(order)) {
            shipments.sort(Comparator.comparing(Shipment::getQuantite));
        } else {
            shipments.sort(Comparator.comparing(Shipment::getQuantite).reversed());
        }
        return shipments;
    }

    // ==================== STATISTIQUES ====================

    @Override
    public Map<String, Object> getStatistiques() {
        Map<String, Object> stats = new HashMap<>();

        long total = shipmentRepo.count();
        stats.put("total", total);

        // Par statut
        Map<String, Long> parStatut = new HashMap<>();
        for (StatutExpedition statut : StatutExpedition.values()) {
            long count = shipmentRepo.findByStatut(statut).size();
            parStatut.put(statut.name(), count);
            stats.put("pourcentage_" + statut.name(), total > 0 ? (count * 100.0 / total) : 0);
        }
        stats.put("parStatut", parStatut);

        // Quantité totale
        double quantiteTotale = shipmentRepo.findAll().stream()
                .mapToDouble(Shipment::getQuantite)
                .sum();
        stats.put("quantiteTotale", quantiteTotale);

        // Top produits
        List<Object[]> topProduits = shipmentRepo.findTopProduits();
        stats.put("topProduits", topProduits);

        return stats;
    }

    @Override
    public Map<String, Object> getStatistiquesByCommande(Long deliveryOrderId) {
        Map<String, Object> stats = new HashMap<>();

        List<Shipment> shipments = shipmentRepo.findByDeliveryOrderIdDelivery(deliveryOrderId);
        stats.put("total", shipments.size());

        // Quantité totale
        double quantiteTotale = shipments.stream()
                .mapToDouble(Shipment::getQuantite)
                .sum();
        stats.put("quantiteTotale", quantiteTotale);

        // Par statut
        Map<String, Long> parStatut = shipments.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getStatut().name(),
                        Collectors.counting()
                ));
        stats.put("parStatut", parStatut);

        return stats;
    }

    @Override
    public Map<String, Long> getStatistiquesParJour() {
        List<Object[]> results = shipmentRepo.countShipmentsByDay();
        Map<String, Long> stats = new LinkedHashMap<>();

        for (Object[] result : results) {
            String date = result[0] != null ? result[0].toString() : "N/A";
            Long count = ((Number) result[1]).longValue();
            stats.put(date, count);
        }

        return stats;
    }

    @Override
    public Map<String, Long> getStatistiquesParMois() {
        List<Object[]> results = shipmentRepo.countShipmentsByMonth();
        Map<String, Long> stats = new LinkedHashMap<>();

        for (Object[] result : results) {
            String mois = result[0] + "-" + String.format("%02d", result[1]);
            Long count = ((Number) result[2]).longValue();
            stats.put(mois, count);
        }

        return stats;
    }
}
