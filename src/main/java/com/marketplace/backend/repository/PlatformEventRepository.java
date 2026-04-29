package com.marketplace.backend.repository;

import com.marketplace.backend.entity.PlatformEvent;
import com.marketplace.backend.entity.enums.EventStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PlatformEventRepository extends JpaRepository<PlatformEvent, Long> {

  @Query("SELECT e FROM PlatformEvent e WHERE " +
         "(:searchTerm IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
         "LOWER(e.location) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
         "LOWER(e.typeLabel) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
         "(:statuses IS NULL OR e.status IN :statuses) AND " +
         "(:dateFrom IS NULL OR e.eventDate >= :dateFrom) AND " +
         "(:dateTo IS NULL OR e.eventDate <= :dateTo) AND " +
         "(:minParticipants IS NULL OR e.participants >= :minParticipants) AND " +
         "(:maxParticipants IS NULL OR e.participants <= :maxParticipants)")
  Page<PlatformEvent> searchEvents(
      @Param("searchTerm") String searchTerm,
      @Param("statuses") List<EventStatus> statuses,
      @Param("dateFrom") LocalDate dateFrom,
      @Param("dateTo") LocalDate dateTo,
      @Param("minParticipants") Integer minParticipants,
      @Param("maxParticipants") Integer maxParticipants,
      Pageable pageable);

  List<PlatformEvent> findByEventDate(LocalDate eventDate);

}
