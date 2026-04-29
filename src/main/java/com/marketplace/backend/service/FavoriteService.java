package com.marketplace.backend.service;

import com.marketplace.backend.dto.FavoriteResponse;
import com.marketplace.backend.entity.Favorite;
import com.marketplace.backend.entity.ResourceListing;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.repository.FavoriteRepository;
import com.marketplace.backend.repository.ResourceListingRepository;
import com.marketplace.backend.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FavoriteService {

  private final FavoriteRepository favoriteRepository;
  private final ResourceListingRepository listingRepository;
  private final UserRepository userRepository;

  @Transactional
  public FavoriteResponse add(Long listingId, Long userId) {
    ResourceListing listing =
        listingRepository
            .findById(listingId)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found"));

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    if (favoriteRepository.existsByUserIdAndListingId(userId, listingId)) {
      throw new IllegalArgumentException("Already in favorites");
    }

    Favorite favorite = Favorite.builder().user(user).listing(listing).build();
    favorite = favoriteRepository.save(favorite);

    return toResponse(favorite);
  }

  @Transactional
  public void remove(Long listingId, Long userId) {
    Favorite favorite =
        favoriteRepository
            .findByUserIdAndListingId(userId, listingId)
            .orElseThrow(() -> new IllegalArgumentException("Favorite not found"));
    favoriteRepository.delete(favorite);
  }

  @Transactional(readOnly = true)
  public List<FavoriteResponse> findByUser(Long userId) {
    return favoriteRepository.findByUserId(userId).stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  private FavoriteResponse toResponse(Favorite favorite) {
    return FavoriteResponse.builder()
        .id(favorite.getId())
        .userId(favorite.getUser().getId())
        .listingId(favorite.getListing().getId())
        .listingTitle(favorite.getListing().getTitle())
        .build();
  }
}
