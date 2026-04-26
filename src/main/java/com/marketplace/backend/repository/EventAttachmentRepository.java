package com.marketplace.backend.repository;

import com.marketplace.backend.entity.EventAttachment;
import com.marketplace.backend.entity.PlatformEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventAttachmentRepository extends JpaRepository<EventAttachment, Long> {
  
  List<EventAttachment> findByPlatformEvent(PlatformEvent platformEvent);
  
  List<EventAttachment> findByPlatformEventId(Long eventId);
  
  void deleteByPlatformEventId(Long eventId);
}
