package com.marketplace.backend.service;

import com.marketplace.backend.dto.StockStatsDTO;
import com.marketplace.backend.entity.StockItem;
import com.marketplace.backend.repository.IStockItemRepository;
import com.marketplace.backend.repository.IStockMovementRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdvancedStatsService {

    private final IStockItemRepository stockRepo;
    private final IStockMovementRepository movementRepo;

    public AdvancedStatsService(IStockItemRepository stockRepo, IStockMovementRepository movementRepo) {
        this.stockRepo = stockRepo;
        this.movementRepo = movementRepo;
    }

    public StockStatsDTO getAdvancedStatistics() {
        StockStatsDTO stats = new StockStatsDTO();

        // Basic stats
        stats.setTotalItems(stockRepo.count());
        stats.setTotalUniqueProducts(stockRepo.countDistinctProducts());
        stats.setTotalValue(stockRepo.calculateTotalStockValue());
        stats.setAverageUnitPrice(stockRepo.getAverageUnitPrice());
        stats.setTotalQuantity(stockRepo.findAll().stream().mapToInt(s -> s.getQuantity()).sum());

        // Distribution stats
        stats.setStatusDistribution(convertToMap(stockRepo.getStatusDistribution()));
        stats.setConditionDistribution(convertToMap(stockRepo.getConditionDistribution()));
        stats.setLocationDistribution(convertToMap(stockRepo.getLocationDistribution()));

        // Expiry stats
        stats.setExpiredItems((long) stockRepo.findExpiredStockItems().size());
        stats.setExpiringIn7Days(stockRepo.countExpiringIn7Days());
        List<StockItem> nearExpiryList = stockRepo.findNearExpiryStockItems(getDateInDays(30));
        stats.setExpiringIn30Days((long) nearExpiryList.size());        stats.setHealthyItems(stockRepo.countHealthyItems());

        // Value metrics
        stats.setValueByLocation(convertValueMap(stockRepo.getValueByLocation()));
        stats.setTop5MostValuable(convertToTopProductDTO(stockRepo.findTop5MostValuable(), true));
        stats.setTop5HighestQuantity(convertToTopProductDTO(stockRepo.findTop5HighestQuantity(), false));

        // Movement stats
        stats.setTotalMovementsLast30Days(stockRepo.countMovementsLast30Days());
        stats.setMovementsByType(convertToIntegerMap(stockRepo.getMovementsByType()));

        // Health scores
        stats.setStockHealthScore(calculateStockHealthScore(stats));
        stats.setTurnoverRate(calculateTurnoverRate(stats));
        stats.setAverageDaysInStock(45.0);

        return stats;
    }

    private Map<String, Long> convertToMap(List<Object[]> data) {
        return data.stream().collect(Collectors.toMap(arr -> arr[0].toString(), arr -> (Long) arr[1]));
    }

    private Map<String, Integer> convertToIntegerMap(List<Object[]> data) {
        return data.stream().collect(Collectors.toMap(arr -> arr[0].toString(), arr -> ((Number) arr[1]).intValue()));
    }

    private Map<String, Double> convertValueMap(List<Object[]> data) {
        Map<String, Double> result = new HashMap<>();
        for (Object[] arr : data) {
            result.put(arr[0].toString(), ((Number) arr[1]).doubleValue());
        }
        return result;
    }

    private List<StockStatsDTO.TopProductDTO> convertToTopProductDTO(List<Object[]> data, boolean byValue) {
        return data.stream().map(arr -> {
            StockStatsDTO.TopProductDTO dto = new StockStatsDTO.TopProductDTO();
            dto.setProductName(arr[0].toString());
            if (byValue) {
                dto.setTotalValue(((Number) arr[1]).doubleValue());
            } else {
                dto.setQuantity(((Number) arr[1]).intValue());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    private Double calculateStockHealthScore(StockStatsDTO stats) {
        double score = 100.0;
        if (stats.getTotalItems() > 0) {
            double expiredPenalty = (stats.getExpiredItems() * 100.0) / stats.getTotalItems();
            score -= expiredPenalty * 0.5;
            double nearExpiryPenalty = (stats.getExpiringIn7Days() * 100.0) / stats.getTotalItems();
            score -= nearExpiryPenalty * 0.3;
        }
        if (stats.getTurnoverRate() != null && stats.getTurnoverRate() > 3) {
            score += 10;
        }
        return Math.max(0, Math.min(100, score));
    }

    private Double calculateTurnoverRate(StockStatsDTO stats) {
        if (stats.getTotalMovementsLast30Days() == null || stats.getTotalQuantity() == 0) {
            return 0.0;
        }
        return (stats.getTotalMovementsLast30Days().doubleValue() / stats.getTotalQuantity()) * 100;
    }
    private Date getDateInDays(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }
}