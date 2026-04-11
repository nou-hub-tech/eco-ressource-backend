package com.marketplace.backend.service;

import com.marketplace.backend.entity.StockItem;
import com.marketplace.backend.entity.StockMovement;
import com.marketplace.backend.repository.IStockItemRepository;
import com.marketplace.backend.repository.IStockMovementRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class StockMovementServiceImpl implements IStockMovementService {

    private final IStockMovementRepository movementRepo;
    private final IStockItemRepository stockItemRepo;

    public StockMovementServiceImpl(IStockMovementRepository movementRepo,
                                    IStockItemRepository stockItemRepo) {
        this.movementRepo = movementRepo;
        this.stockItemRepo = stockItemRepo;
    }

    @Override
    public List<StockMovement> getHistoryByStockItem(Long idStock) {
        return movementRepo.findByStockItemId(idStock);  // ← updated method name
    }

    @Override
    public List<StockMovement> getAllHistory() {
        return movementRepo.findAll();
    }

    @Override
    public void recordMovement(String type, int quantity, String description, Long idStock) {
        StockItem stockItem = stockItemRepo.findById(idStock).orElse(null);
        if (stockItem == null) return;

        StockMovement movement = new StockMovement();
        movement.setMovementType(type);
        movement.setQuantity(quantity);
        movement.setDescription(description);
        movement.setMovementDate(new Date());
        movement.setStockItem(stockItem);
        movementRepo.save(movement);
    }

}