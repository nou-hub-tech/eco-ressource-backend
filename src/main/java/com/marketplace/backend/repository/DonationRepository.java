package com.marketplace.backend.repository;

import com.marketplace.backend.entity.Donation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DonationRepository extends JpaRepository<Donation, Long> {
  List<Donation> findByAssociationId(Long associationId);
}
