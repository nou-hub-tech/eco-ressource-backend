package com.marketplace.backend.repository;

import com.marketplace.backend.entity.Favorite;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

  Optional<Favorite> findByUserIdAndListingId(Long userId, Long listingId);

  boolean existsByUserIdAndListingId(Long userId, Long listingId);

  List<Favorite> findByUserId(Long userId);

  long countByListingId(Long listingId);
}
