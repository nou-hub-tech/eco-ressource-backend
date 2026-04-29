package com.marketplace.backend.repository;

import com.marketplace.backend.entity.ReservationSlot;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationSlotRepository extends JpaRepository<ReservationSlot, Long> {

  List<ReservationSlot> findByEnterpriseId(Long enterpriseId);

  @Query(
      "SELECT s FROM ReservationSlot s "
          + "WHERE s.enterprise.id = :eid "
          + "AND (s.deleted IS NULL OR s.deleted = false)")
  List<ReservationSlot> findActiveByEnterpriseId(@Param("eid") Long eid);

  @Query("SELECT s FROM ReservationSlot s WHERE (s.deleted IS NULL OR s.deleted = false)")
  List<ReservationSlot> findAllActive();

  /** Heatmap range — all enterprises, active only. */
  @Query(
      "SELECT s FROM ReservationSlot s "
          + "WHERE s.date BETWEEN :from AND :to "
          + "AND (s.deleted IS NULL OR s.deleted = false)")
  List<ReservationSlot> findInRange(
      @Param("from") LocalDate from, @Param("to") LocalDate to);
}
