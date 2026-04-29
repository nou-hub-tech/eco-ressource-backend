package com.marketplace.backend.controller;

import com.marketplace.backend.entity.StockMovement;
import com.marketplace.backend.service.IStockMovementService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stock-movement")
@CrossOrigin(origins = "http://localhost:4200")
public class StockMovementRestController {

    private final IStockMovementService movementService;

    public StockMovementRestController(IStockMovementService movementService) {
        this.movementService = movementService;
    }

    @GetMapping("/history/{idStock}")
    public List<StockMovement> getHistoryByStock(@PathVariable Long idStock) {
        return movementService.getHistoryByStockItem(idStock);
    }

    @GetMapping("/history")
    public List<StockMovement> getAllHistory() {
        return movementService.getAllHistory();
    }
}