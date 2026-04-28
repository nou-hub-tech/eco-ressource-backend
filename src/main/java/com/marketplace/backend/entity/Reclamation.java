package com.marketplace.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reclamations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reclamation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enterprise_id", nullable = false)
    private Enterprise enterprise;

    // The enterprise whose stock is being claimed against
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_enterprise_id")
    private Enterprise targetEnterprise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_item_id")
    private StockItem stockItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(length = 100)
    private String defectType;

    @Column(length = 255)
    private String imageUrl;

    @Column(length = 50)
    private String status;

    @Column(name = "damaged_quantity")
    private Integer damagedQuantity;

    @Column(name = "damaged_unit", length = 20)
    private String damagedUnit;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String resolutionNotes;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = "PENDING";
        }
    }
}