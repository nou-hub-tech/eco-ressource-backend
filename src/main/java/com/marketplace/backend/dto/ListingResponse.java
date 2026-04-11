package com.marketplace.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ListingResponse {

  private Long id;
  private String title;
  private String description;
  private String type;
  private String status;
  private Integer quantity;
  private String unit;
  private BigDecimal price;
  private String location;
  private Double latitude;
  private Double longitude;
  private Long productId;
  private String productName;
  private String productCategory;
  private Long companyId;
  private LocalDateTime createdAt;
  private List<String> attachmentUrls;
  private GroupPurchaseResponse groupPurchase;
  private long favoriteCount;
  private long commentCount;
}
