package com.marketplace.backend.service;

import com.marketplace.backend.entity.DeliveryOrder;
import com.marketplace.backend.entity.enums.StatutCommande;
import com.marketplace.backend.repository.DeliveryOrderRepo;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DeliveryOrderServiceImpl implements IDeliveryOrderService {

    private final DeliveryOrderRepo deliveryOrderRepo;

    public DeliveryOrderServiceImpl(DeliveryOrderRepo deliveryOrderRepo) {
        this.deliveryOrderRepo = deliveryOrderRepo;
    }

    // ==================== CRUD ====================

    @Override
    public List<DeliveryOrder> retrieveAllDeliveryOrders() {
        return deliveryOrderRepo.findAll();
    }

    @Override
    public DeliveryOrder retrieveDeliveryOrder(Long id) {
        return deliveryOrderRepo.findById(id).orElse(null);
    }

    @Override
    public DeliveryOrder addDeliveryOrder(DeliveryOrder d) {
        if (d.getStatut() == null) {
            d.setStatut(StatutCommande.EN_ATTENTE);
        }
        return deliveryOrderRepo.save(d);
    }

    @Override
    public void removeDeliveryOrder(Long id) {
        deliveryOrderRepo.deleteById(id);
    }

    @Override
    public DeliveryOrder modifyDeliveryOrder(DeliveryOrder d) {
        return deliveryOrderRepo.save(d);
    }

    @Override
    public DeliveryOrder updateDeliveryOrder(Long id, DeliveryOrder d) {
        d.setIdDelivery(id);
        return deliveryOrderRepo.save(d);
    }

    @Override
    public DeliveryOrder updateStatut(Long id, StatutCommande statut) {
        DeliveryOrder order = retrieveDeliveryOrder(id);
        if (order != null) {
            order.setStatut(statut);
            return deliveryOrderRepo.save(order);
        }
        return null;
    }

    @Override
    public List<DeliveryOrder> getDeliveryOrdersByStatut(StatutCommande statut) {
        return deliveryOrderRepo.findByStatut(statut);
    }

    // ==================== RECHERCHE ====================

    @Override
    public List<DeliveryOrder> findByNomClientContaining(String nomClient) {
        return deliveryOrderRepo.findByNomClientContainingIgnoreCase(nomClient);
    }

    @Override
    public List<DeliveryOrder> findByAdresseContaining(String adresse) {
        return deliveryOrderRepo.findByAdresseLivraisonContainingIgnoreCase(adresse);
    }

    @Override
    public List<DeliveryOrder> findByDate(String date) {
        LocalDate localDate = LocalDate.parse(date);
        return deliveryOrderRepo.findByDate(localDate);
    }

    @Override
    public List<DeliveryOrder> rechercheAvancee(String nomClient, String adresse,
                                                StatutCommande statut, String date) {
        LocalDate localDate = null;
        if (date != null && !date.isEmpty()) {
            localDate = LocalDate.parse(date);
        }
        return deliveryOrderRepo.rechercheAvancee(nomClient, adresse, statut, localDate);
    }

    // ==================== TRI ====================

    @Override
    public List<DeliveryOrder> sortByDate(String order) {
        List<DeliveryOrder> orders = deliveryOrderRepo.findAll();
        if ("asc".equalsIgnoreCase(order)) {
            orders.sort(Comparator.comparing(DeliveryOrder::getDatePrevue));
        } else {
            orders.sort(Comparator.comparing(DeliveryOrder::getDatePrevue).reversed());
        }
        return orders;
    }

    @Override
    public List<DeliveryOrder> sortByNomClient(String order) {
        List<DeliveryOrder> orders = deliveryOrderRepo.findAll();
        if ("asc".equalsIgnoreCase(order)) {
            orders.sort(Comparator.comparing(DeliveryOrder::getNomClient));
        } else {
            orders.sort(Comparator.comparing(DeliveryOrder::getNomClient).reversed());
        }
        return orders;
    }

    // ==================== STATISTIQUES ====================

    @Override
    public Map<String, Object> getStatistiques() {
        Map<String, Object> stats = new HashMap<>();

        long total = deliveryOrderRepo.count();
        stats.put("total", total);

        Map<String, Long> parStatut = new HashMap<>();
        for (StatutCommande statut : StatutCommande.values()) {
            long count = deliveryOrderRepo.findByStatut(statut).size();
            parStatut.put(statut.name(), count);
            stats.put("pourcentage_" + statut.name(), total > 0 ? (count * 100.0 / total) : 0);
        }
        stats.put("parStatut", parStatut);

        long enRetard = deliveryOrderRepo.findLateOrders(LocalDateTime.now()).size();
        stats.put("enRetard", enRetard);
        stats.put("pourcentage_enRetard", total > 0 ? (enRetard * 100.0 / total) : 0);

        // ✅ MAIN CORRECT - findByDatePrevueBetween existe maintenant
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextWeek = now.plusDays(7);
        long aVenir = deliveryOrderRepo.findByDatePrevueBetween(now, nextWeek).size();
        stats.put("aVenir_7jours", aVenir);

        return stats;
    }

    @Override
    public Map<String, Long> getStatistiquesParMois() {
        List<DeliveryOrder> orders = deliveryOrderRepo.findAll();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        return orders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getDatePrevue().format(formatter),
                        Collectors.counting()
                ));
    }

    @Override
    public Map<String, Long> getStatistiquesParSemaine() {
        List<DeliveryOrder> orders = deliveryOrderRepo.findAll();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-'W'ww");
        return orders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getDatePrevue().format(formatter),
                        Collectors.counting()
                ));
    }

    @Override
    public Map<String, Long> getStatistiquesParAnnee() {
        List<DeliveryOrder> orders = deliveryOrderRepo.findAll();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy");
        return orders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getDatePrevue().format(formatter),
                        Collectors.counting()
                ));
    }
}