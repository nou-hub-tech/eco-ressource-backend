package com.marketplace.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.util.Date;

@Entity
public class InventoryScan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String barcode;
    private int realQty;
    private String realCondition;
    private String realLocation;

    @Temporal(TemporalType.TIMESTAMP)
    private Date scannedAt;

    @ManyToOne
    @JoinColumn(name = "id_product")
    @JsonIgnoreProperties({"stockItems", "hibernateLazyInitializer"})
    private Product product;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    public int getRealQty() { return realQty; }
    public void setRealQty(int realQty) { this.realQty = realQty; }

    public String getRealCondition() { return realCondition; }
    public void setRealCondition(String realCondition) { this.realCondition = realCondition; }

    public String getRealLocation() { return realLocation; }
    public void setRealLocation(String realLocation) { this.realLocation = realLocation; }

    public Date getScannedAt() { return scannedAt; }
    public void setScannedAt(Date scannedAt) { this.scannedAt = scannedAt; }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
}