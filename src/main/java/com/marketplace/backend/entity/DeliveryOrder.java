package com.marketplace.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.marketplace.backend.entity.enums.StatutCommande;

@Entity
@Table(name = "delivery_orders")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DeliveryOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_delivery")
    private Long idDelivery;

    @Column(name = "nom_client", nullable = false, length = 100)
    private String nomClient;

    @Column(name = "telephone_client", nullable = false, length = 20)
    private String telephoneClient;

    @Column(name = "adresse_livraison", nullable = false, length = 255)
    private String adresseLivraison;

    @Column(name = "date_prevue", nullable = false)
    private LocalDateTime datePrevue;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false)
    private StatutCommande statut;

    @OneToMany(mappedBy = "deliveryOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("deliveryOrder")
    private List<Shipment> shipments = new ArrayList<>();

    // Constructeurs
    public DeliveryOrder() {}

    // Getters
    public Long getIdDelivery() { return idDelivery; }
    public String getNomClient() { return nomClient; }
    public String getTelephoneClient() { return telephoneClient; }
    public String getAdresseLivraison() { return adresseLivraison; }
    public LocalDateTime getDatePrevue() { return datePrevue; }
    public StatutCommande getStatut() { return statut; }
    public List<Shipment> getShipments() { return shipments; }

    // Setters
    public void setIdDelivery(Long idDelivery) { this.idDelivery = idDelivery; }
    public void setNomClient(String nomClient) { this.nomClient = nomClient; }
    public void setTelephoneClient(String telephoneClient) { this.telephoneClient = telephoneClient; }
    public void setAdresseLivraison(String adresseLivraison) { this.adresseLivraison = adresseLivraison; }
    public void setDatePrevue(LocalDateTime datePrevue) { this.datePrevue = datePrevue; }
    public void setStatut(StatutCommande statut) { this.statut = statut; }
    public void setShipments(List<Shipment> shipments) { this.shipments = shipments; }
}
