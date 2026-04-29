package com.marketplace.backend.service;

import com.marketplace.backend.dto.GeocodingResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeocodingService {

  private final RestTemplate restTemplate;

  @Value("${openrouteservice.api.key:}")
  private String openRouteServiceKey;

  public boolean isOpenRouteServiceConfigured() {
    return openRouteServiceKey != null && !openRouteServiceKey.isBlank();
  }

  public GeocodingResponse geocode(String query) {
    if (query == null || query.isBlank()) {
      throw new IllegalArgumentException("La localisation est obligatoire");
    }
    if (isOpenRouteServiceConfigured()) {
      GeocodingResponse response = geocodeOpenRouteService(query);
      if (response != null) {
        return response;
      }
    }
    GeocodingResponse fallback = geocodeNominatim(query);
    if (fallback == null) {
      throw new IllegalArgumentException("Localisation introuvable");
    }
    return fallback;
  }

  @SuppressWarnings("unchecked")
  private GeocodingResponse geocodeOpenRouteService(String query) {
    try {
      String url =
          UriComponentsBuilder.fromUriString(
                  "https://api.openrouteservice.org/geocode/search")
              .queryParam("text", query)
              .queryParam("size", 1)
              .build()
              .toUriString();
      HttpHeaders headers = new HttpHeaders();
      headers.set("Authorization", openRouteServiceKey);
      ResponseEntity<Map> response =
          restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
      Map<String, Object> body = response.getBody();
      if (body == null) return null;
      List<Object> features = (List<Object>) body.get("features");
      if (features == null || features.isEmpty()) return null;
      Map<String, Object> feature = (Map<String, Object>) features.get(0);
      Map<String, Object> geometry = (Map<String, Object>) feature.get("geometry");
      Map<String, Object> props = (Map<String, Object>) feature.get("properties");
      List<Object> coords = (List<Object>) geometry.get("coordinates");
      return GeocodingResponse.builder()
          .label(String.valueOf(props.getOrDefault("label", query)))
          .longitude(asDouble(coords.get(0)))
          .latitude(asDouble(coords.get(1)))
          .provider("openrouteservice")
          .build();
    } catch (Exception ignored) {
      log.warn("OpenRouteService geocoding failed, fallback to Nominatim: {}", ignored.getMessage());
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private GeocodingResponse geocodeNominatim(String query) {
    try {
      String url =
          UriComponentsBuilder.fromUriString("https://nominatim.openstreetmap.org/search")
              .queryParam("q", query)
              .queryParam("format", "json")
              .queryParam("limit", 1)
              .build()
              .toUriString();
      HttpHeaders headers = new HttpHeaders();
      headers.set("User-Agent", "eco-ressource-b2b/1.0");
      ResponseEntity<List> response =
          restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), List.class);
      List<Map<String, Object>> body = response.getBody();
      if (body == null || body.isEmpty()) return null;
      Map<String, Object> item = body.get(0);
      return GeocodingResponse.builder()
          .label(String.valueOf(item.getOrDefault("display_name", query)))
          .latitude(Double.valueOf(String.valueOf(item.get("lat"))))
          .longitude(Double.valueOf(String.valueOf(item.get("lon"))))
          .provider("nominatim")
          .build();
    } catch (Exception ignored) {
      log.warn("Nominatim geocoding failed: {}", ignored.getMessage());
      return null;
    }
  }

  private Double asDouble(Object value) {
    return value instanceof Number n ? n.doubleValue() : Double.valueOf(String.valueOf(value));
  }
}
