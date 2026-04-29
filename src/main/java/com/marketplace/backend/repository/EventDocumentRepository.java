package com.marketplace.backend.repository;

import com.marketplace.backend.entity.EventDocument;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventDocumentRepository extends JpaRepository<EventDocument, Long> {
  List<EventDocument> findByPlatformEventId(Long platformEventId);
}
