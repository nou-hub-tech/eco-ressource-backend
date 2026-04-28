package com.marketplace.backend.controller;

import com.marketplace.backend.dto.NotificationDTO;
import com.marketplace.backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/unread/{userId}")
    public ResponseEntity<?> getUnreadNotifications(@PathVariable Long userId) {
        try {
            List<NotificationDTO> notifications = notificationService.getUnreadNotifications(userId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/all/{userId}")
    public ResponseEntity<?> getAllNotifications(@PathVariable Long userId) {
        try {
            List<NotificationDTO> notifications = notificationService.getAllNotifications(userId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendNotification(@RequestBody Map<String, Object> request) {
        try {
            Long userId = ((Number) request.get("userId")).longValue();

            NotificationDTO notification = new NotificationDTO();
            notification.setType((String) request.get("type"));
            notification.setDeliveryOrderId(((Number) request.get("deliveryOrderId")).longValue());
            notification.setClientName((String) request.get("clientName"));
            notification.setMessage((String) request.get("message"));

            if (request.containsKey("problemeType")) {
                notification.setProblemeType((String) request.get("problemeType"));
            }
            if (request.containsKey("retardMinutes")) {
                notification.setRetardMinutes((Integer) request.get("retardMinutes"));
            }
            if (request.containsKey("transporterName")) {
                notification.setTransporterName((String) request.get("transporterName"));
            }

            NotificationDTO saved = notificationService.addNotification(userId, notification);
            return ResponseEntity.ok(Map.of("success", true, "notification", saved));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/mark-read/{userId}")
    public ResponseEntity<?> markAsRead(@PathVariable Long userId, @RequestBody List<String> notificationIds) {
        try {
            int count = notificationService.markAsRead(userId, notificationIds);
            return ResponseEntity.ok(Map.of("success", true, "count", count));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/clear-read/{userId}")
    public ResponseEntity<?> clearReadNotifications(@PathVariable Long userId) {
        try {
            int count = notificationService.clearReadNotifications(userId);
            return ResponseEntity.ok(Map.of("success", true, "count", count));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/clear-all/{userId}")
    public ResponseEntity<?> clearAllNotifications(@PathVariable Long userId) {
        try {
            notificationService.clearAllNotifications(userId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}