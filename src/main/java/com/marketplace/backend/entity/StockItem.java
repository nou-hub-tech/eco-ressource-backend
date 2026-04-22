package com.marketplace.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stock_item")
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

  private int quantity;
  private double unitPrice;
  private String status;
  private String location;
  private String unit;

  @Column(name = "item_condition")
  private String condition;

  private String image;

  @Temporal(TemporalType.DATE)
  private Date expirationDate;

  /** Legacy annonces / filtres par société (optionnel si enterprise est renseigné). */
  @Column(name = "company_id")
  private Long companyId;

  @ManyToOne
  @JoinColumn(name = "id_product")
  private Product product;

  @Column(nullable = false)
  @Builder.Default
  private boolean deleted = false;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enterprise_id")
  private Enterprise enterprise;
}
