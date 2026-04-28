package com.marketplace.backend.repository;

import com.marketplace.backend.entity.Reservation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

  List<Reservation> findByEnterpriseId(Long enterpriseId);
}
