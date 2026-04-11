package com.marketplace.backend.controller;

import com.marketplace.backend.dto.CreateListingRequest;
import com.marketplace.backend.dto.ListingResponse;
import com.marketplace.backend.service.ResourceListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/resource-listings")
@RequiredArgsConstructor
@Tag(name = "Resource Listings", description = "CRUD et recherche d'annonces")
public class ResourceListingController {

  private final ResourceListingService listingService;

  @PostMapping
  @Operation(summary = "Creer une annonce (SURPLUS, DEMANDE ou GROUP_BUYING)")
  public ResponseEntity<ListingResponse> create(@Valid @RequestBody CreateListingRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(listingService.create(req));
  }

  @GetMapping
  @Operation(summary = "Lister toutes les annonces actives")
  public ResponseEntity<List<ListingResponse>> list() {
    return ResponseEntity.ok(listingService.findAll());
  }

  @GetMapping("/{id}")
  @Operation(summary = "Detail d'une annonce par ID")
  public ResponseEntity<ListingResponse> getById(@PathVariable Long id) {
    return ResponseEntity.ok(listingService.getById(id));
  }

  @GetMapping("/search")
  @Operation(summary = "Rechercher des annonces par type, categorie, localisation, prix max")
  public ResponseEntity<List<ListingResponse>> search(
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String location,
      @RequestParam(required = false) Double maxPrice) {
    return ResponseEntity.ok(listingService.search(type, category, location, maxPrice));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Modifier une annonce (proprietaire uniquement)")
  public ResponseEntity<ListingResponse> update(
      @PathVariable Long id,
      @RequestParam Long companyId,
      @Valid @RequestBody CreateListingRequest req) {
    return ResponseEntity.ok(listingService.update(id, req, companyId));
  }

  @PostMapping("/{id}/duplicate")
  @Operation(summary = "Dupliquer une annonce existante")
  public ResponseEntity<ListingResponse> duplicate(@PathVariable Long id) {
    return ResponseEntity.status(HttpStatus.CREATED).body(listingService.duplicate(id));
  }

  @PutMapping("/{id}/cancel")
  @Operation(summary = "Annuler une annonce (proprietaire uniquement)")
  public ResponseEntity<Void> cancel(
      @PathVariable Long id, @RequestParam Long companyId) {
    listingService.cancel(id, companyId);
    return ResponseEntity.noContent().build();
  }
}
