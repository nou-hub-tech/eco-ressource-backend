package com.marketplace.backend.entity.finance;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "invoice_number", unique = true, nullable = false)
    private String invoiceNumber;
    @Column(name = "client_name", nullable = false)
    private String clientName;
    @Column(name = "project", nullable = false)
    private String project;
    @Column(name = "amount_ht", nullable = false)
    private Double amountHT;
    @Column(name = "tva", nullable = false)
    private Double tva = 9.0;
    @Column(name = "amount_ttc", nullable = false)
    private Double amountTTC;
    @Column(name = "status", nullable = false)
    private String status = "UNPAID"; // PAID / UNPAID
    private String issueDate;
    // Liaison livraison (nullable — rempli lors intégration)
    private Long deliveryOrderId;
    private Long linkedEscrowId;
    private String deliveredAt;
    // Lien vers stock_item
    private Long idStock;
}