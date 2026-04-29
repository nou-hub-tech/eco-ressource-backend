package com.marketplace.backend.service;

import com.marketplace.backend.entity.StockItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface IStockItemService {
    List<StockItem> retrieveAllStockItems();

    StockItem addStockItem(StockItem s);

    StockItem updateStockItem(StockItem s);

    StockItem retrieveStockItem(Long id_stock);

    void removeStockItem(Long id_stock);

    StockItem assignStockItemToProduct(Long id_stock, Long id_product);

    Double getTotalStockValue();

    Double getTotalValueByProduct(Long productId);

    List<StockItem> searchStockItems(String status, String location, String productName);

    List<Map<String, Object>> getStatsByCategory();

    List<Map<String, Object>> getStatsByLocation();

    List<StockItem> getExpiredStockItems();

    List<StockItem> getNearExpiryStockItems(int days);

    Page<StockItem> retrieveAllStockItems(Pageable pageable);
}