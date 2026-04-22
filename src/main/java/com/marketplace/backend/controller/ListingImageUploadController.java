package com.marketplace.backend.controller;

import com.marketplace.backend.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Upload d’images pour les annonces « ressource » : renvoie une URL courte {@code /files/...} à mettre dans
 * {@code attachmentUrls} au lieu d’une Data URL base64.
 */
@RestController
@RequestMapping("/api/listing-images")
@RequiredArgsConstructor
@Tag(name = "Listing images", description = "Upload multipart pour pièces jointes d’annonces")
public class ListingImageUploadController {

  private final FileStorageService fileStorageService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyAuthority('ROLE_ENTERPRISE','ROLE_ADMIN')")
  @Operation(summary = "Uploader une image (max 5 Mo, image/*) ; réponse url à réutiliser dans attachmentUrls")
  public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
    try {
      String fileName = fileStorageService.storeListingImage(file);
      String relativeUrl = "/files/" + fileName;
      return ResponseEntity.status(HttpStatus.CREATED)
          .body(
              Map.of(
                  "url", relativeUrl,
                  "path", relativeUrl,
                  "filename", fileName));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
    }
  }
}
