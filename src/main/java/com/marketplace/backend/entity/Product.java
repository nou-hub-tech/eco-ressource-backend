package com.marketplace.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
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
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_product")
  private Long idProduct;

  @NotBlank private String name;

  @NotBlank private String category;

  @NotBlank private String materialType;

  private boolean recyclable;

  @NotBlank
  @Column(length = 1000)
  private String description;

  @NotBlank private String image;

  private String barcode;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enterprise_id")
  @JsonIgnore
  private Enterprise enterprise;

  @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
  @JsonIgnore
  @Builder.Default
  private List<StockItem> stockItems = new ArrayList<>();

  /** JSON-safe enterprise id (avoids lazy cycle). */
  public Long getEnterpriseId() {
    return enterprise != null ? enterprise.getId() : null;
  }

  /** Alias used by inventory / product REST layer. */
  public Long getId_product() {
    return idProduct;
  }

  public void setId_product(Long id) {
    this.idProduct = id;
  }
}
