package com.marketplace.backend.service;

import com.marketplace.backend.entity.StockMovement;

import java.util.List;

public interface IStockMovementService {
    List<StockMovement> getHistoryByStockItem(Long idStock);
    List<StockMovement> getAllHistory();
    void recordMovement(String type, int quantity, String description, Long idStock);
}