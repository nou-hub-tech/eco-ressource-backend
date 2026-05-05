package com.marketplace.backend.controller;

import com.marketplace.backend.dto.CommentModerationResult;
import com.marketplace.backend.dto.GeocodingResponse;
import com.marketplace.backend.service.CommentModerationService;
import com.marketplace.backend.service.EmailJsService;
import com.marketplace.backend.service.GeocodingService;
import com.marketplace.backend.service.PerspectiveModerationService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/integrations")
@RequiredArgsConstructor
public class IntegrationDiagnosticsController {

  private final PerspectiveModerationService perspectiveModerationService;
  private final CommentModerationService commentModerationService;
  private final EmailJsService emailJsService;
  private final GeocodingService geocodingService;

  @Value("${perspective.enabled:auto}")
  private String perspectiveMode;

  @Value("${moderation.admin-email:admin@marketplace.com}")
  private String moderationAdminEmail;

  @GetMapping("/status")
  public ResponseEntity<Map<String, Object>> status() {
    return ResponseEntity.ok(
        Map.of(
            "perspectiveMode",
            perspectiveMode,
            "perspectiveConfigured",
            perspectiveModerationService.isConfigured(),
            "emailJsConfigured",
            emailJsService.isConfigured(),
            "openRouteServiceConfigured",
            geocodingService.isOpenRouteServiceConfigured(),
            "moderationAdminEmail",
            moderationAdminEmail));
  }

  @PostMapping("/perspective-test")
  public ResponseEntity<CommentModerationResult> perspectiveTest(
      @RequestBody Map<String, String> body) {
    String content = body.getOrDefault("content", "Bonjour, je suis interesse par cette annonce.");
    return ResponseEntity.ok(commentModerationService.moderate(content));
  }

  @GetMapping("/geocode-test")
  public ResponseEntity<GeocodingResponse> geocodeTest(@RequestParam String q) {
    return ResponseEntity.ok(geocodingService.geocode(q));
  }

  @PostMapping("/emailjs-test")
  public ResponseEntity<Map<String, Object>> emailJsTest(@RequestParam String to) {
    boolean sent =
        emailJsService.sendEmail(
            to,
            "Test EmailJS Eco-Ressource",
            "EmailJS est configure pour le module Gestion Annonces.");
    return ResponseEntity.ok(Map.of("sent", sent, "to", to));
  }
}
