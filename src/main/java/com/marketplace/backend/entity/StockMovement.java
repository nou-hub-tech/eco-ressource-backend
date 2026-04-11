package com.marketplace.backend.entity;

import jakarta.persistence.*;

import java.util.Date;

@Entity
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String movementType; // IN, OUT, UPDATE

    private int quantity;

    @Temporal(TemporalType.TIMESTAMP)
    private Date movementDate;

    private String description;

    @ManyToOne
    @JoinColumn(name = "id_stock")
    private StockItem stockItem;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) { this.movementType = movementType; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public Date getMovementDate() { return movementDate; }
    public void setMovementDate(Date movementDate) { this.movementDate = movementDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public StockItem getStockItem() { return stockItem; }
    public void setStockItem(StockItem stockItem) { this.stockItem = stockItem; }
}