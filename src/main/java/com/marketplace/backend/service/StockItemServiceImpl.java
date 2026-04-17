package com.marketplace.backend.service;

import com.marketplace.backend.entity.Product;
import com.marketplace.backend.entity.StockItem;
import com.marketplace.backend.repository.IProductRepository;
import com.marketplace.backend.repository.IStockItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    public List<StockItem> retrieveAllStockItems() {
        return stockRepo.findAllActive();  // ✅ CHANGED
    }

    @Override
    public Page<StockItem> retrieveAllStockItems(Pageable pageable) {
        return stockRepo.findAllActive(pageable);  // ✅ CHANGED
    }

    @Override
    public StockItem addStockItem(StockItem s) {
        s.setDeleted(false);  // ✅ Ensure new items are not deleted
        StockItem saved = stockRepo.save(s);
        movementService.recordMovement("IN", saved.getQuantity(),
                "Stock item added", saved.getIdStock());
        return saved;
    }

    @Override
    public StockItem updateStockItem(StockItem s) {
        StockItem existing = stockRepo.findById(s.getIdStock()).orElse(null);
        if (existing != null) {
            s.setDeleted(existing.isDeleted());  // ✅ Preserve deleted status
        }
        StockItem updated = stockRepo.save(s);
        movementService.recordMovement("UPDATE", updated.getQuantity(),
                "Stock item updated", updated.getIdStock());
        return updated;
    }

    @Override
    @Transactional
    public void removeStockItem(Long id_stock) {
        StockItem s = stockRepo.findById(id_stock).orElse(null);
        if (s != null && !s.isDeleted()) {
            // ✅ Soft delete - just mark as deleted
            s.setDeleted(true);
            stockRepo.save(s);

            // ✅ Record the deletion in movements
            movementService.recordMovement("DELETED", s.getQuantity(),
                    "Stock item soft deleted", s.getIdStock());
        }
    }

    @Override
    public StockItem retrieveStockItem(Long id_stock) {
        return stockRepo.findActiveById(id_stock);  // ✅ CHANGED - only return if not deleted
    }

    @Override
    public StockItem assignStockItemToProduct(Long id_stock, Long id_product) {
        StockItem s = stockRepo.findActiveById(id_stock);  // ✅ CHANGED
        Product p = productRepo.findById(id_product).orElse(null);

        if (s != null && p != null) {
            s.setProduct(p);
            return stockRepo.save(s);
        }
        return null;
    }

    @Override
    public Double getTotalStockValue() {
        return stockRepo.calculateTotalStockValue();  // ✅ Already filters deleted
    }

    @Override
    public Double getTotalValueByProduct(Long id_product) {
        return stockRepo.calculateTotalValueByProduct(id_product);  // ✅ Already filters deleted
    }

    @Override
    public List<StockItem> searchStockItems(String status, String location, String productName) {
        return stockRepo.searchStockItems(status, location, productName);  // ✅ Already filters deleted
    }

    @Override
    public List<Map<String, Object>> getStatsByCategory() {
        return stockRepo.getStatsByCategory();  // ✅ Already filters deleted
    }

    @Override
    public List<Map<String, Object>> getStatsByLocation() {
        return stockRepo.getStatsByLocation();  // ✅ Already filters deleted
    }

    @Override
    public List<StockItem> getExpiredStockItems() {
        return stockRepo.findExpiredStockItems();  // ✅ Already filters deleted
    }

    @Override
    public List<StockItem> getNearExpiryStockItems(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, days);
        return stockRepo.findNearExpiryStockItems(cal.getTime());  // ✅ Already filters deleted
    }

    // ✅ Optional: Add restore method
    @Transactional
    public void restoreStockItem(Long id_stock) {
        StockItem s = stockRepo.findById(id_stock).orElse(null);
        if (s != null && s.isDeleted()) {
            s.setDeleted(false);
            stockRepo.save(s);
            movementService.recordMovement("RESTORED", s.getQuantity(),
                    "Stock item restored", s.getIdStock());
        }
    }
}