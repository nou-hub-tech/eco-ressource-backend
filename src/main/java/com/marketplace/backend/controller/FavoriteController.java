package com.marketplace.backend.controller;

import com.marketplace.backend.dto.FavoriteResponse;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.security.SecurityUserHelper;
import com.marketplace.backend.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Favorites", description = "Gestion des favoris par annonce")
public class FavoriteController {

  private final FavoriteService favoriteService;
  private final SecurityUserHelper securityUserHelper;

  @PostMapping("/api/resource-listings/{listingId}/favorite")
  @Operation(summary = "Ajouter une annonce aux favoris")
  public ResponseEntity<FavoriteResponse> add(
      @PathVariable Long listingId, Authentication auth) {
    User user = securityUserHelper.requireUser(auth);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(favoriteService.add(listingId, user.getId()));
  }

  @DeleteMapping("/api/resource-listings/{listingId}/favorite")
  @Operation(summary = "Retirer une annonce des favoris")
  public ResponseEntity<Void> remove(
      @PathVariable Long listingId, Authentication auth) {
    User user = securityUserHelper.requireUser(auth);
    favoriteService.remove(listingId, user.getId());
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/api/favorites/me")
  @Operation(summary = "Lister mes favoris")
  public ResponseEntity<List<FavoriteResponse>> myFavorites(Authentication auth) {
    User user = securityUserHelper.requireUser(auth);
    return ResponseEntity.ok(favoriteService.findByUser(user.getId()));
  }
}
