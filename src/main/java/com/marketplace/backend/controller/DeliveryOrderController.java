package com.marketplace.backend.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.marketplace.backend.entity.DeliveryOrder;
import com.marketplace.backend.entity.enums.StatutCommande;
import com.marketplace.backend.service.IDeliveryOrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/delivery-orders")
@CrossOrigin(origins = "http://localhost:4200")
public class DeliveryOrderController {

    private final IDeliveryOrderService deliveryOrderService;

    public DeliveryOrderController(IDeliveryOrderService deliveryOrderService) {
        this.deliveryOrderService = deliveryOrderService;
    }

    // ==================== CRUD EXISTANTS ====================

    @PostMapping("/add")
    public DeliveryOrder addDeliveryOrder(@RequestBody DeliveryOrder d) {
        return deliveryOrderService.addDeliveryOrder(d);
    }

    @GetMapping("/all")
    public List<DeliveryOrder> getAllDeliveryOrders() {
        return deliveryOrderService.retrieveAllDeliveryOrders();
    }

    @GetMapping("/{id}")
    public DeliveryOrder getDeliveryOrder(@PathVariable Long id) {
        return deliveryOrderService.retrieveDeliveryOrder(id);
    }

    @PutMapping("/update/{id}")
    public DeliveryOrder updateDeliveryOrder(@PathVariable Long id, @RequestBody DeliveryOrder d) {
        return deliveryOrderService.updateDeliveryOrder(id, d);
    }

    @DeleteMapping("/delete/{id}")
    public void deleteDeliveryOrder(@PathVariable Long id) {
        deliveryOrderService.removeDeliveryOrder(id);
    }

    @PatchMapping("/update-statut/{id}")
    public DeliveryOrder updateStatut(@PathVariable Long id, @RequestParam StatutCommande statut) {
        return deliveryOrderService.updateStatut(id, statut);
    }

    @GetMapping("/statut/{statut}")
    public List<DeliveryOrder> getByStatut(@PathVariable StatutCommande statut) {
        return deliveryOrderService.getDeliveryOrdersByStatut(statut);
    }

    // ==================== RECHERCHE ====================

    @GetMapping("/search/nom")
    public List<DeliveryOrder> searchByNomClient(@RequestParam String nomClient) {
        return deliveryOrderService.findByNomClientContaining(nomClient);
    }

    @GetMapping("/search/adresse")
    public List<DeliveryOrder> searchByAdresse(@RequestParam String adresse) {
        return deliveryOrderService.findByAdresseContaining(adresse);
    }

    @GetMapping("/search/date")
    public List<DeliveryOrder> searchByDate(@RequestParam String date) {
        return deliveryOrderService.findByDate(date);
    }

    @GetMapping("/recherche")
    public List<DeliveryOrder> rechercheAvancee(
            @RequestParam(required = false) String nomClient,
            @RequestParam(required = false) String adresse,
            @RequestParam(required = false) StatutCommande statut,
            @RequestParam(required = false) String date) {
        return deliveryOrderService.rechercheAvancee(nomClient, adresse, statut, date);
    }

    // ==================== TRI ====================

    @GetMapping("/sort/date")
    public List<DeliveryOrder> sortByDate(@RequestParam(defaultValue = "desc") String order) {
        return deliveryOrderService.sortByDate(order);
    }

    @GetMapping("/sort/nom")
    public List<DeliveryOrder> sortByNomClient(@RequestParam(defaultValue = "asc") String order) {
        return deliveryOrderService.sortByNomClient(order);
    }

    // ================= STATISTIQUES =================

    @GetMapping("/statistiques")
    public Map<String, Object> getStatistiques() {
        return deliveryOrderService.getStatistiques();
    }

    @GetMapping("/statistiques/mois")
    public Map<String, Long> getStatistiquesParMois() {
        return deliveryOrderService.getStatistiquesParMois();
    }

    @GetMapping("/statistiques/semaine")
    public Map<String, Long> getStatistiquesParSemaine() {
        return deliveryOrderService.getStatistiquesParSemaine();
    }

    @GetMapping("/statistiques/annee")
    public Map<String, Long> getStatistiquesParAnnee() {
        return deliveryOrderService.getStatistiquesParAnnee();
    }

    // ==================== QR CODE ====================

    @GetMapping("/update-by-qr/{id}")
    public ResponseEntity<String> updateByQrCode(@PathVariable Long id) {
        DeliveryOrder order = deliveryOrderService.retrieveDeliveryOrder(id);
        if (order != null) {
            order.setStatut(StatutCommande.LIVREE);
            deliveryOrderService.modifyDeliveryOrder(order);

            String html = "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "    <meta charset='UTF-8'>\n" +
                    "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" +
                    "    <title>Livraison confirmée</title>\n" +
                    "    <style>\n" +
                    "        body { font-family: Arial; text-align: center; padding: 50px; }\n" +
                    "        .success { color: green; font-size: 48px; }\n" +
                    "        .message { font-size: 24px; margin-top: 20px; }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <div class='success'></div>\n" +
                    "    <div class='message'>Commande #" + id + " marquée comme livrée avec succès!</div>\n" +
                    "    <p>Fermez cette page pour continuer.</p>\n" +
                    "</body>\n" +
                    "</html>";

            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
        }
        return ResponseEntity.notFound().build();
    }
}