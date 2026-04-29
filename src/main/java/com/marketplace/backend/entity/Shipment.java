package com.marketplace.backend.entity;

import  com.marketplace.backend.entity.enums.StatutExpedition;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipments")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_order_id", nullable = false)
    @JsonIgnoreProperties("shipments")
    private DeliveryOrder deliveryOrder;

    @Column(name = "produit_id", nullable = false)
    private Long produitId;

    @Column(name = "quantite", nullable = false)
    private Double quantite;

    @Column(name = "id_transporter", nullable = false)
    private Long idTransporter;

    @Column(name = "date_depart", nullable = false)
    private LocalDateTime dateDepart;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutExpedition statut;

    // Constructeurs
    public Shipment() {}

    // Getters
    public Long getId() { return id; }
    public DeliveryOrder getDeliveryOrder() { return deliveryOrder; }
    public Long getProduitId() { return produitId; }
    public Double getQuantite() { return quantite; }
    public Long getIdTransporter() { return idTransporter; }
    public LocalDateTime getDateDepart() { return dateDepart; }
    public StatutExpedition getStatut() { return statut; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setDeliveryOrder(DeliveryOrder deliveryOrder) { this.deliveryOrder = deliveryOrder; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }
    public void setQuantite(Double quantite) { this.quantite = quantite; }
    public void setIdTransporter(Long idTransporter) { this.idTransporter = idTransporter; }
    public void setDateDepart(LocalDateTime dateDepart) { this.dateDepart = dateDepart; }
    public void setStatut(StatutExpedition statut) { this.statut = statut; }
}
