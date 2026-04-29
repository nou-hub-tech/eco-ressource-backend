package com.marketplace.backend.repository;

import com.marketplace.backend.entity.Reservation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

  List<Reservation> findByEnterpriseId(Long enterpriseId);

  /** Active rows for an enterprise. NULL deleted is treated as false (legacy rows). */
  @Query(
      "SELECT r FROM Reservation r "
          + "WHERE r.enterprise.id = :eid "
          + "AND (r.deleted IS NULL OR r.deleted = false)")
  List<Reservation> findActiveByEnterpriseId(@Param("eid") Long eid);

  /** All active rows (admin). */
  @Query("SELECT r FROM Reservation r WHERE (r.deleted IS NULL OR r.deleted = false)")
  List<Reservation> findAllActive();
}
