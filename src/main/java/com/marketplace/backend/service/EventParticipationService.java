package com.marketplace.backend.service;

import com.marketplace.backend.dto.EventParticipationRequest;
import com.marketplace.backend.entity.EventParticipation;
import com.marketplace.backend.entity.PlatformEvent;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.repository.EventParticipationRepository;
import com.marketplace.backend.repository.PlatformEventRepository;
import com.marketplace.backend.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EventParticipationService {

  private final EventParticipationRepository eventParticipationRepository;
  private final UserRepository userRepository;
  private final PlatformEventRepository platformEventRepository;

  @Transactional(readOnly = true)
  public List<EventParticipation> findAll() {
    return eventParticipationRepository.findAll();
  }

  @Transactional(readOnly = true)
  public EventParticipation getById(Long id) {
    return eventParticipationRepository
        .findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Not found"));
  }

  @Transactional
  public EventParticipation create(EventParticipationRequest req) {
    User user =
        userRepository.findById(req.getUserId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
    PlatformEvent platformEvent =
        platformEventRepository
            .findById(req.getPlatformEventId())
            .orElseThrow(() -> new IllegalArgumentException("Platform event not found"));

    EventParticipation eventParticipation =
        EventParticipation.builder().user(user).platformEvent(platformEvent).build();

    EventParticipation saved = eventParticipationRepository.save(eventParticipation);
    recountParticipationCount(platformEvent.getId());
    return saved;
  }

  @Transactional
  public EventParticipation update(Long id, EventParticipationRequest req) {
    EventParticipation eventParticipation =
        eventParticipationRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Not found"));

    Long oldPlatformEventId = eventParticipation.getPlatformEvent().getId();

    User user =
        userRepository.findById(req.getUserId()).orElseThrow(() -> new IllegalArgumentException("User not found"));
    PlatformEvent platformEvent =
        platformEventRepository
            .findById(req.getPlatformEventId())
            .orElseThrow(() -> new IllegalArgumentException("Platform event not found"));

    eventParticipation.setUser(user);
    eventParticipation.setPlatformEvent(platformEvent);

    EventParticipation updated = eventParticipationRepository.save(eventParticipation);

    recountParticipationCount(platformEvent.getId());
    if (!oldPlatformEventId.equals(platformEvent.getId())) {
      recountParticipationCount(oldPlatformEventId);
    }

    return updated;
  }

  @Transactional
  public void delete(Long id) {
    EventParticipation eventParticipation =
        eventParticipationRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Not found"));

    Long platformEventId = eventParticipation.getPlatformEvent().getId();
    eventParticipationRepository.delete(eventParticipation);
    recountParticipationCount(platformEventId);
  }

  @Transactional
  public void recountParticipationCount(Long platformEventId) {
    PlatformEvent platformEvent =
        platformEventRepository
            .findById(platformEventId)
            .orElseThrow(() -> new IllegalArgumentException("Platform event not found"));

    long participationCount = eventParticipationRepository.countByPlatformEventId(platformEventId);
    platformEvent.setParticipants((int) participationCount);
    platformEventRepository.save(platformEvent);
  }
}