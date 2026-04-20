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
public class escrow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idescrow;
    private String project;
    private Double amount;
    @Enumerated(EnumType.STRING)
    private EscrowStatus status;
    private String createdAt;
    private String releaseDate;
    private Long deliveryOrderId;
    private Long linkedInvoiceId;
    private Long idStock;             
}
