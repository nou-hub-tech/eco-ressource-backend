package com.marketplace.backend.config;

import com.marketplace.backend.service.PerspectiveModerationService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class IntegrationStartupLogger {

  private final PerspectiveModerationService perspectiveModerationService;

  @Value("${perspective.enabled:auto}")
  private String perspectiveEnabled;

  @Value("${emailjs.service-id:}")
  private String emailJsServiceId;

  @Value("${emailjs.template-id:}")
  private String emailJsTemplateId;

  @Value("${emailjs.user-id:}")
  private String emailJsUserId;

  @Value("${openrouteservice.api.key:}")
  private String openRouteServiceKey;

  @Value("${moderation.admin-email:admin@marketplace.com}")
  private String moderationAdminEmail;

  @PostConstruct
  public void logIntegrations() {
    boolean perspectiveReady = perspectiveModerationService.isConfigured();
    boolean emailReady =
        !emailJsServiceId.isBlank() && !emailJsTemplateId.isBlank() && !emailJsUserId.isBlank();
    boolean orsReady = openRouteServiceKey != null && !openRouteServiceKey.isBlank();

    log.info("Module annonces - Perspective mode: {}, key configured: {}", perspectiveEnabled, perspectiveReady);
    log.info("Module annonces - EmailJS configured: {}", emailReady);
    log.info("Module annonces - OpenRouteService configured: {}", orsReady);
    log.info("Module annonces - Moderation admin email: {}", moderationAdminEmail);
  }
}
