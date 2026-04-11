package com.marketplace.backend.repository;

import com.marketplace.backend.entity.Listing;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingRepository extends JpaRepository<Listing, Long> {

  List<Listing> findByEnterpriseId(Long enterpriseId);
}
