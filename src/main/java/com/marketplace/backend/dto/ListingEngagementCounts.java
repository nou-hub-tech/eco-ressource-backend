package com.marketplace.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Compteurs d’interaction pour une annonce (favoris + commentaires), renvoyés après mutations. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingEngagementCounts {

  private Long listingId;
  private long favoriteCount;
  private long commentCount;
}
