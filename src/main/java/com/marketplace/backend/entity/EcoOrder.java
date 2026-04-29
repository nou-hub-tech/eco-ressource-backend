package com.marketplace.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.marketplace.backend.entity.enums.EcoGrade;
import com.marketplace.backend.entity.enums.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Procurement order in the circular-economy module.
 * <p>Named {@code EcoOrder} (not {@code Order}) because {@code ORDER}
 * is a reserved SQL keyword and JPA's default table-name derivation
 * would collide. Mapped explicitly to the {@code eco_orders} table.
 */
@Entity
@Table(name = "eco_orders")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcoOrder {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Human-friendly reference, e.g. "ORD-2041". */
  @Column(nullable = false, unique = true)
  private String ref;

  /** Buying company (display name; convenient for admin views). */
  @Column(nullable = false)
  private String companyName;

  @Column(nullable = false)
  private String material;

  /** Quantity in kilograms. */
  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal qtyKg;

  @Column(nullable = false)
  private String supplier;

  /** Distance from supplier to buyer (km). */
  @Column(nullable = false)
  private Integer distanceKm;

  @Column(nullable = false)
  private LocalDate orderDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 1)
  private EcoGrade grade;

  /** Computed eco metrics — cached for reporting / PDF generation. */
  @Column(name = "co2_saved", precision = 10, scale = 2)
  private BigDecimal co2Saved;

  @Column(name = "water_saved", precision = 10, scale = 2)
  private BigDecimal waterSaved;

  @Column(name = "waste_avoided", precision = 10, scale = 2)
  private BigDecimal wasteAvoided;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enterprise_id")
  private Enterprise enterprise;

  // ===== Soft delete =====
  @Column(name = "deleted")
  @Builder.Default
  private Boolean deleted = false;

  @Column(name = "cancel_reason", length = 500)
  private String cancelReason;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @PrePersist
  public void prePersist() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
    if (deleted == null) {
      deleted = false;
    }
    if (orderDate == null) {
      orderDate = LocalDate.now();
    }
  }
}
