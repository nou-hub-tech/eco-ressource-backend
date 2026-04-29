package com.marketplace.backend.dto;

import java.util.List;
import java.util.Map;

public class StockStatsDTO {
    // Basic stats
    private Long totalItems;
    private Long totalUniqueProducts;
    private Double totalValue;
    private Double averageUnitPrice;
    private Integer totalQuantity;

    // Distribution
    private Map<String, Long> statusDistribution;
    private Map<String, Long> conditionDistribution;
    private Map<String, Long> locationDistribution;

    // Expiry stats
    private Long expiredItems;
    private Long expiringIn7Days;
    private Long expiringIn30Days;
    private Long healthyItems;

    // Value metrics
    private Map<String, Double> valueByCategory;
    private Map<String, Double> valueByLocation;
    private List<TopProductDTO> top5MostValuable;
    private List<TopProductDTO> top5HighestQuantity;

    // Movement stats
    private Integer totalMovementsLast30Days;
    private Map<String, Integer> movementsByType;

    // Health scores
    private Double stockHealthScore;
    private Double turnoverRate;
    private Double averageDaysInStock;

    // Getters and Setters
    public Long getTotalItems() { return totalItems; }
    public void setTotalItems(Long totalItems) { this.totalItems = totalItems; }

    public Long getTotalUniqueProducts() { return totalUniqueProducts; }
    public void setTotalUniqueProducts(Long totalUniqueProducts) { this.totalUniqueProducts = totalUniqueProducts; }

    public Double getTotalValue() { return totalValue; }
    public void setTotalValue(Double totalValue) { this.totalValue = totalValue; }

    public Double getAverageUnitPrice() { return averageUnitPrice; }
    public void setAverageUnitPrice(Double averageUnitPrice) { this.averageUnitPrice = averageUnitPrice; }

    public Integer getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(Integer totalQuantity) { this.totalQuantity = totalQuantity; }

    public Map<String, Long> getStatusDistribution() { return statusDistribution; }
    public void setStatusDistribution(Map<String, Long> statusDistribution) { this.statusDistribution = statusDistribution; }

    public Map<String, Long> getConditionDistribution() { return conditionDistribution; }
    public void setConditionDistribution(Map<String, Long> conditionDistribution) { this.conditionDistribution = conditionDistribution; }

    public Map<String, Long> getLocationDistribution() { return locationDistribution; }
    public void setLocationDistribution(Map<String, Long> locationDistribution) { this.locationDistribution = locationDistribution; }

    public Long getExpiredItems() { return expiredItems; }
    public void setExpiredItems(Long expiredItems) { this.expiredItems = expiredItems; }

    public Long getExpiringIn7Days() { return expiringIn7Days; }
    public void setExpiringIn7Days(Long expiringIn7Days) { this.expiringIn7Days = expiringIn7Days; }

    public Long getExpiringIn30Days() { return expiringIn30Days; }
    public void setExpiringIn30Days(Long expiringIn30Days) { this.expiringIn30Days = expiringIn30Days; }

    public Long getHealthyItems() { return healthyItems; }
    public void setHealthyItems(Long healthyItems) { this.healthyItems = healthyItems; }

    public Map<String, Double> getValueByCategory() { return valueByCategory; }
    public void setValueByCategory(Map<String, Double> valueByCategory) { this.valueByCategory = valueByCategory; }

    public Map<String, Double> getValueByLocation() { return valueByLocation; }
    public void setValueByLocation(Map<String, Double> valueByLocation) { this.valueByLocation = valueByLocation; }

    public List<TopProductDTO> getTop5MostValuable() { return top5MostValuable; }
    public void setTop5MostValuable(List<TopProductDTO> top5MostValuable) { this.top5MostValuable = top5MostValuable; }

    public List<TopProductDTO> getTop5HighestQuantity() { return top5HighestQuantity; }
    public void setTop5HighestQuantity(List<TopProductDTO> top5HighestQuantity) { this.top5HighestQuantity = top5HighestQuantity; }

    public Integer getTotalMovementsLast30Days() { return totalMovementsLast30Days; }
    public void setTotalMovementsLast30Days(Integer totalMovementsLast30Days) { this.totalMovementsLast30Days = totalMovementsLast30Days; }

    public Map<String, Integer> getMovementsByType() { return movementsByType; }
    public void setMovementsByType(Map<String, Integer> movementsByType) { this.movementsByType = movementsByType; }

    public Double getStockHealthScore() { return stockHealthScore; }
    public void setStockHealthScore(Double stockHealthScore) { this.stockHealthScore = stockHealthScore; }

    public Double getTurnoverRate() { return turnoverRate; }
    public void setTurnoverRate(Double turnoverRate) { this.turnoverRate = turnoverRate; }

    public Double getAverageDaysInStock() { return averageDaysInStock; }
    public void setAverageDaysInStock(Double averageDaysInStock) { this.averageDaysInStock = averageDaysInStock; }

    // Inner class for Top Products
    public static class TopProductDTO {
        private String productName;
        private Double totalValue;
        private Integer quantity;

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public Double getTotalValue() { return totalValue; }
        public void setTotalValue(Double totalValue) { this.totalValue = totalValue; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}