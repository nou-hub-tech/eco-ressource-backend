package com.marketplace.backend.service;

import com.marketplace.backend.dto.PlatformEventRequest;
import com.marketplace.backend.entity.PlatformEvent;
import com.marketplace.backend.entity.enums.EventStatus;
import com.marketplace.backend.repository.PlatformEventRepository;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlatformEventService {

  private final PlatformEventRepository platformEventRepository;

  @Transactional(readOnly = true)
  public List<PlatformEvent> findAll() {
    return platformEventRepository.findAll();
  }

  @Transactional(readOnly = true)
  public PlatformEvent getById(Long id) {
    return platformEventRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
  }

  @Transactional
  public PlatformEvent create(PlatformEventRequest req) {
    PlatformEvent e =
        PlatformEvent.builder()
            .title(req.getTitle())
            .eventDate(req.getEventDate())
            .location(req.getLocation())
            .participants(req.getParticipants())
            .status(EventStatus.valueOf(req.getStatus().toLowerCase(Locale.ROOT)))
            .typeLabel(req.getTypeLabel())
            .build();
    return platformEventRepository.save(e);
  }

  @Transactional
  public PlatformEvent update(Long id, PlatformEventRequest req) {
    PlatformEvent e =
        platformEventRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    e.setTitle(req.getTitle());
    e.setEventDate(req.getEventDate());
    e.setLocation(req.getLocation());
    e.setParticipants(req.getParticipants());
    e.setStatus(EventStatus.valueOf(req.getStatus().toLowerCase(Locale.ROOT)));
    e.setTypeLabel(req.getTypeLabel());
    return platformEventRepository.save(e);
  }

  @Transactional
  public void delete(Long id) {
    PlatformEvent e =
        platformEventRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    platformEventRepository.delete(e);
  }
}
