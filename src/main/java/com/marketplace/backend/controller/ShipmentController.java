package com.marketplace.backend.controller;

import com.marketplace.backend.entity.Shipment;
import  com.marketplace.backend.entity.enums.StatutExpedition;
import  com.marketplace.backend.service.IShipmentService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shipments")
@CrossOrigin(origins = "http://localhost:4200")
public class ShipmentController {

    private final IShipmentService shipmentService;

    public ShipmentController(IShipmentService shipmentService) {
        this.shipmentService = shipmentService;
    }

    // ==================== CRUD EXISTANTS ====================

    @PostMapping("/add")
    public Shipment addShipment(@RequestBody Shipment s) {
        return shipmentService.addShipment(s);
    }

    @GetMapping("/all")
    public List<Shipment> getAllShipments() {
        return shipmentService.retrieveAllShipments();
    }

    @GetMapping("/{id}")
    public Shipment getShipment(@PathVariable Long id) {
        return shipmentService.retrieveShipment(id);
    }

    @PutMapping("/update/{id}")
    public Shipment updateShipment(@PathVariable Long id, @RequestBody Shipment s) {
        return shipmentService.updateShipment(id, s);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteShipment(@PathVariable Long id) {
        shipmentService.removeShipment(id);
    }

    @GetMapping("/delivery-order/{deliveryOrderId}")
    public List<Shipment> getByDeliveryOrder(@PathVariable Long deliveryOrderId) {
        return shipmentService.getShipmentsByDeliveryOrder(deliveryOrderId);
    }

    @GetMapping("/statut/{statut}")
    public List<Shipment> getByStatut(@PathVariable StatutExpedition statut) {
        return shipmentService.getShipmentsByStatut(statut);
    }

    // ==================== NOUVEAUX ENDPOINTS ====================

    // ✅ RECHERCHE par produit
    @GetMapping("/search/produit")
    public List<Shipment> searchByProduit(@RequestParam Long produitId) {
        return shipmentService.findByProduitId(produitId);
    }

    // ✅ RECHERCHE par quantité (NOUVEAU)
    @GetMapping("/search/quantite")
    public List<Shipment> searchByQuantite(@RequestParam Double quantite) {
        return shipmentService.findByQuantite(quantite);
    }

    // ✅ RECHERCHE par date (exacte)
    @GetMapping("/search/date")
    public List<Shipment> searchByDate(@RequestParam String date) {
        return shipmentService.findByDate(date);
    }

    // ✅ RECHERCHE AVANCÉE (multi-critères)
    @GetMapping("/recherche")
    public List<Shipment> rechercheAvancee(
            @RequestParam(required = false) Long produitId,
            @RequestParam(required = false) Double quantite,
            @RequestParam(required = false) StatutExpedition statut,
            @RequestParam(required = false) String date) {
        return shipmentService.rechercheAvancee(produitId, quantite, statut, date);
    }

    // ==================== TRI ====================

    // ✅ TRI par date de départ
    @GetMapping("/sort/date")
    public List<Shipment> sortByDate(@RequestParam(defaultValue = "desc") String order) {
        return shipmentService.sortByDate(order);
    }

    // ✅ TRI par quantité
    @GetMapping("/sort/quantite")
    public List<Shipment> sortByQuantite(@RequestParam(defaultValue = "desc") String order) {
        return shipmentService.sortByQuantite(order);
    }

    // ==================== STATISTIQUES ====================

    // ✅ STATISTIQUES générales
    @GetMapping("/statistiques")
    public Map<String, Object> getStatistiques() {
        return shipmentService.getStatistiques();
    }

    // ✅ STATISTIQUES par commande
    @GetMapping("/statistiques/commande/{deliveryOrderId}")
    public Map<String, Object> getStatistiquesByCommande(@PathVariable Long deliveryOrderId) {
        return shipmentService.getStatistiquesByCommande(deliveryOrderId);
    }

    // ✅ STATISTIQUES par jour
    @GetMapping("/statistiques/jour")
    public Map<String, Long> getStatistiquesParJour() {
        return shipmentService.getStatistiquesParJour();
    }

    // ✅ STATISTIQUES par mois
    @GetMapping("/statistiques/mois")
    public Map<String, Long> getStatistiquesParMois() {
        return shipmentService.getStatistiquesParMois();
    }
}
