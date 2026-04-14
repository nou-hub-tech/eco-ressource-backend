package com.marketplace.backend.repository;

import com.marketplace.backend.entity.EventParticipation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventParticipationRepository extends JpaRepository<EventParticipation, Long> {

  long countByPlatformEventId(Long platformEventId);

  List<EventParticipation> findByPlatformEventId(Long platformEventId);

  List<EventParticipation> findByUserId(Long userId);

  boolean existsByUserIdAndPlatformEventId(Long userId, Long platformEventId);
}