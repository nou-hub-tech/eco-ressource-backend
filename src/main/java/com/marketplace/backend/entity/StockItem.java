package com.marketplace.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stock_item")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_stock")
  private Long idStock;

  @Column(name = "company_id")
  private Long companyId;

  @Column(name = "item_condition")
  private String itemCondition;

  @Column(name = "expiration_date")
  private LocalDate expirationDate;

  private String image;

  private String location;

  @Column(nullable = false)
  private int quantity;

  private String status;

  private String unit;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "id_product")
  private Product product;

  @Column(name = "unit_price", nullable = false)
  private double unitPrice;

  @OneToMany(mappedBy = "stockItem")
  @Builder.Default
  @com.fasterxml.jackson.annotation.JsonIgnore
  private List<StockMovement> stockMovements = new ArrayList<>();
}
