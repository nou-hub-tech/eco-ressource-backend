package com.marketplace.backend.service;

import com.marketplace.backend.dto.NotificationDTO;  // ← CHANGÉ: model → dto
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final Map<Long, List<NotificationDTO>> userNotifications = new ConcurrentHashMap<>();

    public NotificationDTO addNotification(Long userId, NotificationDTO notification) {
        notification.setId(UUID.randomUUID().toString());
        notification.setTimestamp(Instant.now());
        notification.setRead(false);
        notification.setTargetUserId(userId);

        userNotifications.computeIfAbsent(userId, k -> new ArrayList<>()).add(notification);

        System.out.println(" Notification ajoutée pour l'utilisateur " + userId);
        return notification;
    }

    public List<NotificationDTO> getUnreadNotifications(Long userId) {
        List<NotificationDTO> notifications = userNotifications.getOrDefault(userId, new ArrayList<>());
        return notifications.stream()
                .filter(n -> !n.getRead())
                .collect(Collectors.toList());
    }

    public List<NotificationDTO> getAllNotifications(Long userId) {
        return userNotifications.getOrDefault(userId, new ArrayList<>());
    }

    public int markAsRead(Long userId, List<String> notificationIds) {
        List<NotificationDTO> notifications = userNotifications.get(userId);
        if (notifications == null) return 0;

        int count = 0;
        for (NotificationDTO notif : notifications) {
            if (notificationIds.contains(notif.getId()) && !notif.getRead()) {
                notif.setRead(true);
                count++;
            }
        }
        return count;
    }

    public int clearReadNotifications(Long userId) {
        List<NotificationDTO> notifications = userNotifications.get(userId);
        if (notifications == null) return 0;

        int before = notifications.size();
        notifications.removeIf(NotificationDTO::getRead);
        return before - notifications.size();
    }

    public void clearAllNotifications(Long userId) {
        userNotifications.remove(userId);
    }
}