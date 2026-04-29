package com.marketplace.backend.repository;

import com.marketplace.backend.entity.GroupPurchase;
import com.marketplace.backend.entity.enums.GroupPurchaseStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupPurchaseRepository extends JpaRepository<GroupPurchase, Long> {

  Optional<GroupPurchase> findByListingId(Long listingId);

  List<GroupPurchase> findByStatus(GroupPurchaseStatus status);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT g FROM GroupPurchase g WHERE g.id = :id")
  Optional<GroupPurchase> findByIdForUpdate(@Param("id") Long id);
}
