package com.marketplace.backend.service;

import com.marketplace.backend.entity.PlatformEvent;
import com.marketplace.backend.repository.PlatformEventRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacebookService {

  private final PlatformEventRepository platformEventRepository;

  @Value("${facebook.page.id:}")
  private String pageId;

  @Value("${facebook.page.token:}")
  private String pageToken;

  public String publishEvent(Long eventId) {
    if (pageId.isEmpty() || pageToken.isEmpty()) {
      throw new IllegalStateException("Facebook not configured");
    }

    PlatformEvent event = platformEventRepository.findById(eventId)
        .orElseThrow(() -> new IllegalArgumentException("Event not found"));

    String postContent = buildPost(event);

    try {
      HttpClient client = HttpClient.newHttpClient();
      String url = String.format(
          "https://graph.facebook.com/v19.0/%s/feed?message=%s&access_token=%s",
          pageId,
          URLEncoder.encode(postContent, StandardCharsets.UTF_8),
          pageToken
      );

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .POST(HttpRequest.BodyPublishers.noBody())
          .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        log.info("Published event {} to Facebook", eventId);
        return response.body();
      } else {
        log.error("Facebook API error: {}", response.body());
        throw new RuntimeException("Facebook publish failed: " + response.body());
      }
    } catch (Exception e) {
      log.error("Error publishing to Facebook", e);
      throw new RuntimeException("Facebook publish failed", e);
    }
  }

  private String buildPost(PlatformEvent event) {
    String date = event.getEventDate()
        .format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"));
    String desc = event.getDescription() != null ? event.getDescription() : "";

    return String.format(
        "🎉 Exciting Event Alert! 🎉\n\n" +
        "📌 %s\n\n" +
        "%s" +
        "📅 Date: %s\n" +
        "📍 Location: %s\n" +
        "👥 Participants: %d\n" +
        "🏷️ Type: %s\n\n" +
        "♻️ Join us in building a sustainable circular economy! 🌍\n" +
        "#CircularEconomy #B2B #Sustainability #EcoRessource",
        event.getTitle(),
        desc.isEmpty() ? "" : "📝 " + desc + "\n\n",
        date,
        event.getLocation(),
        event.getParticipants(),
        event.getTypeLabel()
    );
  }
}
