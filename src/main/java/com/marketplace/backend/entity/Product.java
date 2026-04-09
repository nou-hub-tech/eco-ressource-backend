package com.marketplace.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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

  private String category;

  @Column(length = 1000)
  private String description;

  private String image;

  @Column(name = "material_type")
  private String materialType;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private boolean recyclable;

  @OneToMany(mappedBy = "product")
  @Builder.Default
  @com.fasterxml.jackson.annotation.JsonIgnore
  private List<StockItem> stockItems = new ArrayList<>();
}
