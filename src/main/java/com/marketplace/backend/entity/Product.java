package com.marketplace.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

@Entity
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_product")
    private Long id_product;

    @NotBlank
    private String name;

    @NotBlank
    private String category;

    @NotBlank
    private String materialType;

    private boolean recyclable;

    @NotBlank
    @Column(length = 1000)
    private String description;

    @NotBlank
    private String image;

    private String barcode;

    @ManyToOne
    @JoinColumn(name = "enterprise_id")
    private Enterprise enterprise;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<StockItem> stockItems;

    public Long getId_product() { return id_product; }
    public void setId_product(Long id_product) { this.id_product = id_product; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getMaterialType() { return materialType; }
    public void setMaterialType(String materialType) { this.materialType = materialType; }

    public boolean isRecyclable() { return recyclable; }
    public void setRecyclable(boolean recyclable) { this.recyclable = recyclable; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }


    public List<StockItem> getStockItems() { return stockItems; }
    public void setStockItems(List<StockItem> stockItems) { this.stockItems = stockItems; }
}