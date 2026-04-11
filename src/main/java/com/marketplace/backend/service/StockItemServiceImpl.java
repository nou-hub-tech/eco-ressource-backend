package com.marketplace.backend.service;

import com.marketplace.backend.entity.Product;
import com.marketplace.backend.entity.StockItem;
import com.marketplace.backend.repository.IProductRepository;
import com.marketplace.backend.repository.IStockItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

@Service
public class StockItemServiceImpl implements IStockItemService {

    private final IStockItemRepository stockRepo;
    private final IProductRepository productRepo;
    private final IStockMovementService movementService;

    public StockItemServiceImpl(IStockItemRepository stockRepo,
                                IProductRepository productRepo,
                                IStockMovementService movementService) {
        this.stockRepo = stockRepo;
        this.productRepo = productRepo;
        this.movementService = movementService;
    }

    // ------------------- BASIC CRUD -------------------

    @Override
    public List<StockItem> retrieveAllStockItems() {
        return stockRepo.findAll();
    }

    @Override
    public Page<StockItem> retrieveAllStockItems(Pageable pageable) {
        return stockRepo.findAll(pageable);
    }

    @Override
    public StockItem addStockItem(StockItem s) {
        StockItem saved = stockRepo.save(s);
        movementService.recordMovement("IN", saved.getQuantity(),
                "Stock item added", saved.getId_stock());
        return saved;
    }

    @Override
    public StockItem updateStockItem(StockItem s) {
        StockItem updated = stockRepo.save(s);
        movementService.recordMovement("UPDATE", updated.getQuantity(),
                "Stock item updated", updated.getId_stock());
        return updated;
    }

    @Override
    public void removeStockItem(Long id_stock) {
        StockItem s = stockRepo.findById(id_stock).orElse(null);
        if (s != null) {
            movementService.recordMovement("OUT", s.getQuantity(),
                    "Stock item deleted", id_stock);
        }
        stockRepo.deleteById(id_stock);
    }



    @Override
    public StockItem retrieveStockItem(Long id_stock) {
        return stockRepo.findById(id_stock).orElse(null);
    }



    // ------------------- BUSINESS LOGIC -------------------

    @Override
    public StockItem assignStockItemToProduct(Long id_stock, Long id_product) {
        StockItem s = stockRepo.findById(id_stock).orElse(null);
        Product p = productRepo.findById(id_product).orElse(null);

        if (s != null && p != null) {
            s.setProduct(p);
            return stockRepo.save(s);
        }
        return null;
    }

    @Override
    public Double getTotalStockValue() {
        return stockRepo.calculateTotalStockValue();
    }

    @Override
    public Double getTotalValueByProduct(Long id_product) {
        return stockRepo.calculateTotalValueByProduct(id_product);
    }

    @Override
    public List<StockItem> searchStockItems(String status, String location, String productName) {
        return stockRepo.searchStockItems(status, location, productName);
    }

    // ------------------- STATS (MISSING BEFORE = ERROR FIX) -------------------

    @Override
    public List<Map<String, Object>> getStatsByCategory() {
        return stockRepo.getStatsByCategory();
    }

    @Override
    public List<Map<String, Object>> getStatsByLocation() {
        return stockRepo.getStatsByLocation();
    }

    // ------------------- EXPIRY LOGIC -------------------

    @Override
    public List<StockItem> getExpiredStockItems() {
        return stockRepo.findExpiredStockItems();
    }

    @Override
    public List<StockItem> getNearExpiryStockItems(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, days);
        return stockRepo.findNearExpiryStockItems(cal.getTime());
    }

}