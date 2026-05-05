package com.marketplace.backend.controller;

import com.marketplace.backend.dto.GeocodingResponse;
import com.marketplace.backend.service.GeocodingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/geocoding")
@RequiredArgsConstructor
@Tag(name = "Geocoding", description = "Conversion localisation vers coordonnees")
public class GeocodingController {

  private final GeocodingService geocodingService;

  @GetMapping
  @Operation(summary = "Geocoder une ville ou adresse")
  public ResponseEntity<GeocodingResponse> geocode(@RequestParam String q) {
    return ResponseEntity.ok(geocodingService.geocode(q));
  }
}
