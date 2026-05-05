package com.marketplace.backend.dto;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ListingMarketingSuggestion {

  private String improvedTitle;
  private String improvedDescription;
  private String callToAction;
  private List<String> tags;
  private List<String> sellingPoints;
  private String materialType;
  private Double suggestedPrice;
  private String priceExplanation;
  private Integer qualityScore;
}
