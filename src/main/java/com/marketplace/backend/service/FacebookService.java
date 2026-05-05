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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;

@Service
@RequiredArgsConstructor
@Slf4j
public class FacebookService {

  private final PlatformEventRepository platformEventRepository;

  @Value("${facebook.page.id:}")
  private String pageId;

  @Value("${facebook.page.token:}")
  private String pageToken;

  private final RestTemplate restTemplate = new RestTemplate();

  public String publishEvent(Long eventId, MultipartFile image) {
    if (pageId.isEmpty() || pageToken.isEmpty()) {
      throw new IllegalStateException("Facebook not configured");
    }

    PlatformEvent event = platformEventRepository.findById(eventId)
        .orElseThrow(() -> new IllegalArgumentException("Event not found"));

    String postContent = buildPost(event);

    try {
      String url = String.format("https://graph.facebook.com/v19.0/%s/photos", pageId);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.MULTIPART_FORM_DATA);

      MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
      body.add("message", postContent);
      body.add("access_token", pageToken);
      
      ByteArrayResource resource = new ByteArrayResource(image.getBytes()) {
          @Override
          public String getFilename() {
              return "poster.png";
          }
      };
      body.add("source", resource);

      HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
      ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);

      if (response.getStatusCode().is2xxSuccessful()) {
        log.info("Published event {} to Facebook as photo", eventId);
        return response.getBody();
      } else {
        log.error("Facebook API error: {}", response.getBody());
        throw new RuntimeException("Facebook publish failed: " + response.getBody());
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
