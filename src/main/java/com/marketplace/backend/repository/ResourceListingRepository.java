package com.marketplace.backend.repository;

import com.marketplace.backend.entity.ResourceListing;
import com.marketplace.backend.entity.enums.ResourceListingStatus;
import com.marketplace.backend.entity.enums.ListingType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResourceListingRepository extends JpaRepository<ResourceListing, Long> {

  List<ResourceListing> findByStatus(ResourceListingStatus status);

  List<ResourceListing> findByType(ListingType type);

  List<ResourceListing> findByCompanyId(Long companyId);

  @Query(
      "SELECT l FROM ResourceListing l WHERE l.status = :status "
          + "AND (:type IS NULL OR l.type = :type) "
          + "AND (:category IS NULL OR l.product.category = :category) "
          + "AND (:location IS NULL OR LOWER(l.location) LIKE LOWER(CONCAT('%', :location, '%')))")
  List<ResourceListing> search(
      @Param("status") ResourceListingStatus status,
      @Param("type") ListingType type,
      @Param("category") String category,
      @Param("location") String location);
}
