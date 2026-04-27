package com.marketplace.backend.controller;

import com.marketplace.backend.dto.CreateListingRequest;
import com.marketplace.backend.dto.ListingMatchResponse;
import com.marketplace.backend.dto.ListingResponse;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.enums.Role;
import com.marketplace.backend.security.SecurityUserHelper;
import com.marketplace.backend.service.ResourceListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
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
  private final SecurityUserHelper securityUserHelper;

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

  @GetMapping("/mine")
  @Operation(summary = "Lister les annonces du profil connecte")
  public ResponseEntity<List<ListingResponse>> mine(Authentication auth) {
    User user = securityUserHelper.requireUser(auth);
    return ResponseEntity.ok(listingService.findMine(user));
  }

  @GetMapping("/admin/all")
  @Operation(summary = "Lister toutes les annonces pour l'administration")
  public ResponseEntity<List<ListingResponse>> adminAll(Authentication auth) {
    User user = securityUserHelper.requireUser(auth);
    if (user.getRole() != Role.ROLE_ADMIN) {
      throw new IllegalArgumentException("Admin role required");
    }
    return ResponseEntity.ok(listingService.findAllForAdmin());
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

  @GetMapping("/trending")
  @Operation(summary = "Lister les annonces tendances selon popularite")
  public ResponseEntity<List<ListingResponse>> trending(
      @RequestParam(defaultValue = "6") int limit) {
    return ResponseEntity.ok(listingService.trending(limit));
  }

  @GetMapping("/{id}/matches")
  @Operation(summary = "Recommandations intelligentes autour d'une annonce")
  public ResponseEntity<List<ListingMatchResponse>> matches(
      @PathVariable Long id, @RequestParam(defaultValue = "6") int limit) {
    return ResponseEntity.ok(listingService.match(id, limit));
  }

  @GetMapping("/price-suggestion")
  @Operation(summary = "Suggestion de prix basee sur les annonces similaires")
  public ResponseEntity<BigDecimal> suggestPrice(
      @RequestParam(required = false) Long productId,
      @RequestParam(required = false) String category,
      @RequestParam(required = false) String location) {
    return ResponseEntity.ok(listingService.suggestPrice(productId, category, location));
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

  @DeleteMapping("/{id}")
  @Operation(summary = "Supprimer une annonce (proprietaire ou admin)")
  public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
    User user = securityUserHelper.requireUser(auth);
    listingService.delete(id, user);
    return ResponseEntity.noContent().build();
  }
}
