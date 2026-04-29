package com.marketplace.backend.service;

import com.marketplace.backend.dto.RealtimeEvent;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RealtimeNotificationService {

  private final SimpMessagingTemplate messagingTemplate;

  public void listingChanged(String type, Long listingId, Object payload) {
    RealtimeEvent<Object> event = event(type, listingId, null, null, payload);
    messagingTemplate.convertAndSend("/topic/listings", event);
    messagingTemplate.convertAndSend("/topic/listings/" + listingId, event);
  }

  public void commentChanged(String type, Long listingId, Object payload) {
    RealtimeEvent<Object> event = event(type, listingId, null, null, payload);
    messagingTemplate.convertAndSend("/topic/listings/" + listingId + "/comments", event);
    messagingTemplate.convertAndSend("/topic/listings", event);
  }

  public void favoriteChanged(Long listingId, Object payload) {
    RealtimeEvent<Object> event = event("FAVORITE_CHANGED", listingId, null, null, payload);
    messagingTemplate.convertAndSend("/topic/listings/" + listingId + "/favorites", event);
    messagingTemplate.convertAndSend("/topic/listings", event);
  }

  public void groupChanged(Long listingId, Long groupId, Object payload) {
    RealtimeEvent<Object> event = event("GROUP_CHANGED", listingId, groupId, null, payload);
    messagingTemplate.convertAndSend("/topic/groups/" + groupId, event);
    messagingTemplate.convertAndSend("/topic/listings/" + listingId, event);
    messagingTemplate.convertAndSend("/topic/listings", event);
  }

  public void notifyUser(Long userId, String type, String message, Object payload) {
    RealtimeEvent<Object> event = event(type, null, null, userId, payload);
    event.setMessage(message);
    messagingTemplate.convertAndSend("/topic/users/" + userId + "/notifications", event);
  }

  public void notifyAdmin(String type, String message, Object payload) {
    RealtimeEvent<Object> event = event(type, null, null, null, payload);
    event.setMessage(message);
    messagingTemplate.convertAndSend("/topic/admin/notifications", event);
  }

  private RealtimeEvent<Object> event(
      String type, Long listingId, Long groupId, Long userId, Object payload) {
    return RealtimeEvent.builder()
        .type(type)
        .listingId(listingId)
        .groupId(groupId)
        .userId(userId)
        .payload(payload)
        .occurredAt(LocalDateTime.now())
        .build();
  }
}
