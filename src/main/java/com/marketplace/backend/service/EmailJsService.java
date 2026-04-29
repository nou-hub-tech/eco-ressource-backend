package com.marketplace.backend.service;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailJsService {

  private static final String EMAILJS_URL = "https://api.emailjs.com/api/v1.0/email/send";

  private final RestTemplate restTemplate;

  @Value("${emailjs.service-id:}")
  private String serviceId;

  @Value("${emailjs.template-id:}")
  private String templateId;

  @Value("${emailjs.user-id:}")
  private String userId;

  @Value("${emailjs.access-token:}")
  private String accessToken;

  @Value("${emailjs.origin:http://localhost:4200}")
  private String emailJsOrigin;

  @Value("${emailjs.default-reply-to:noreply@eco-ressource.local}")
  private String defaultReplyTo;

  public boolean isConfigured() {
    return serviceId != null
        && !serviceId.isBlank()
        && templateId != null
        && !templateId.isBlank()
        && userId != null
        && !userId.isBlank();
  }

  public boolean sendEmail(String recipientEmail, String subject, String message) {
    return sendEmail(recipientEmail, subject, message, defaultReplyTo);
  }

  public boolean sendEmail(String recipientEmail, String subject, String message, String replyTo) {
    if (recipientEmail == null || recipientEmail.isBlank()) {
      log.warn("EmailJS ignored: recipient is empty");
      return false;
    }
    if (!isConfigured()) {
      log.warn("EmailJS ignored: service/template/public key is not configured");
      return false;
    }

    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.set("Origin", emailJsOrigin);
      headers.set("Referer", emailJsOrigin.endsWith("/") ? emailJsOrigin : emailJsOrigin + "/");
      headers.set(
          "User-Agent",
          "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Eco-Ressource/1.0");

      Map<String, Object> payload = new java.util.LinkedHashMap<>();
      payload.put("service_id", serviceId);
      payload.put("template_id", templateId);
      payload.put("user_id", userId);
      if (accessToken != null && !accessToken.isBlank()) {
        payload.put("accessToken", accessToken);
      }
      payload.put(
          "template_params",
          Map.of(
              "to_email", recipientEmail,
              "subject", subject,
              "message", message,
              "reply_to", replyTo == null || replyTo.isBlank() ? defaultReplyTo : replyTo));
      ResponseEntity<String> response =
          restTemplate.postForEntity(EMAILJS_URL, new HttpEntity<>(payload, headers), String.class);
      log.info("EmailJS sent to {} with HTTP {}", recipientEmail, response.getStatusCode());
      return response.getStatusCode().is2xxSuccessful();
    } catch (HttpStatusCodeException ex) {
      log.warn(
          "EmailJS failed for {} with HTTP {}: {}",
          recipientEmail,
          ex.getStatusCode(),
          ex.getResponseBodyAsString());
      return false;
    } catch (Exception ignored) {
      log.warn("EmailJS failed for {}: {}", recipientEmail, ignored.getMessage());
      return false;
    }
  }
}
