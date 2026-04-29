package com.marketplace.backend.controller;

import com.marketplace.backend.dto.ListingMarketingRequest;
import com.marketplace.backend.dto.ListingMarketingSuggestion;
import com.marketplace.backend.service.ListingMarketingAiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/listing-ai")
@RequiredArgsConstructor
@Tag(name = "Listing AI", description = "Aide IA metier pour annonces attractives")
public class ListingAiController {

  private final ListingMarketingAiService marketingAiService;

  @PostMapping("/marketing-suggestions")
  @Operation(summary = "Generer un titre, une description et des tags marketing")
  public ResponseEntity<ListingMarketingSuggestion> suggestMarketing(
      @Valid @RequestBody ListingMarketingRequest request) {
    return ResponseEntity.ok(marketingAiService.suggest(request));
  }
}
