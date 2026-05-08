package com.marketplace.backend.service.workspace;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceSmsService {

  private final ObjectMapper objectMapper;

  @Value("${workspace.notifications.sms.enabled:true}")
  private boolean enabled;

  @Value("${workspace.notifications.sms.provider:textbelt}")
  private String provider;

  @Value("${workspace.notifications.sms.timeout-ms:6000}")
  private long timeoutMs;

  @Value("${workspace.notifications.sms.textbelt.endpoint:https://textbelt.com/text}")
  private String textbeltEndpoint;

  @Value("${workspace.notifications.sms.textbelt.key:textbelt_test}")
  private String textbeltKey;

  @Value("${workspace.notifications.sms.textbelt.sender:Eco Ressource}")
  private String textbeltSender;

  public SmsDispatchResult sendReservationConfirmation(String phoneNumber, String message) {
    if (!enabled) {
      return new SmsDispatchResult(false, "disabled", "SMS dispatch is disabled in configuration.");
    }
    if (phoneNumber == null || phoneNumber.isBlank()) {
      return new SmsDispatchResult(false, "missing-number", "No phone number was provided.");
    }

    if (!"textbelt".equalsIgnoreCase(provider)) {
      return new SmsDispatchResult(
          false,
          "unsupported-provider",
          "The configured SMS provider is not supported by this module.");
    }

    try {
      return sendWithTextbelt(phoneNumber.trim(), message == null ? "" : message.trim());
    } catch (Exception exception) {
      log.warn(
          "[WORKSPACE_SMS] SMS dispatch failed for {} via {}: {}",
          phoneNumber,
          provider,
          exception.getMessage());
      return new SmsDispatchResult(false, provider, exception.getMessage());
    }
  }

  private SmsDispatchResult sendWithTextbelt(String phoneNumber, String message)
      throws IOException, InterruptedException {
    HttpClient client =
        HttpClient.newBuilder().connectTimeout(Duration.ofMillis(Math.max(timeoutMs, 1000L))).build();

    Map<String, String> payload = new LinkedHashMap<>();
    payload.put("phone", phoneNumber);
    payload.put("message", message);
    payload.put("key", textbeltKey);
    payload.put("sender", textbeltSender);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(textbeltEndpoint))
            .timeout(Duration.ofMillis(Math.max(timeoutMs, 1000L)))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(toFormBody(payload)))
            .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    JsonNode json = objectMapper.readTree(response.body());
    boolean success = json.path("success").asBoolean(false);
    String detail =
        success
            ? (isTextbeltTestKey()
                ? "Textbelt test mode accepted the SMS request."
                : "Textbelt accepted the SMS request.")
            : json.path("error").asText("Textbelt did not confirm delivery.");

    log.info(
        "[WORKSPACE_SMS] Textbelt response for {}: success={}, detail={}",
        phoneNumber,
        success,
        detail);

    return new SmsDispatchResult(success, "textbelt", detail);
  }

  private boolean isTextbeltTestKey() {
    return textbeltKey != null
        && (textbeltKey.endsWith("_test") || "textbelt_test".equalsIgnoreCase(textbeltKey));
  }

  private String toFormBody(Map<String, String> payload) {
    return payload.entrySet().stream()
        .map(
            entry ->
                URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                    + "="
                    + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
        .reduce((left, right) -> left + "&" + right)
        .orElse("");
  }

  public record SmsDispatchResult(boolean success, String provider, String detail) {}
}
