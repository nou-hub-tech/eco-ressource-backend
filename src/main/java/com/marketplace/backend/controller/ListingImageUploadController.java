package com.marketplace.backend.controller;

import com.marketplace.backend.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
  @Operation(
      summary = "Uploader une image d’annonce (multipart)",
      description =
          "Champ formulaire **`file`** (image/*, max 5 Mo). Réponse **201** : JSON avec **`url`** et **`path`** "
              + "au format `/files/{nom}` à réutiliser dans **`attachmentUrls`** lors de la création ou mise à jour "
              + "d’une annonce ressource (évite les Data URL base64 en base). "
              + "Le fichier est ensuite servi en **GET** `/files/{filename}` (voir tag fichiers).")
  @ApiResponses({
    @ApiResponse(
        responseCode = "201",
        description = "Fichier enregistré",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples =
                    @ExampleObject(
                        value =
                            "{\"url\":\"/files/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg\","
                                + "\"path\":\"/files/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg\","
                                + "\"filename\":\"a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg\"}"))),
    @ApiResponse(
        responseCode = "400",
        description = "Fichier vide, type non image, ou taille > 5 Mo",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                examples = @ExampleObject(value = "{\"error\":\"...\"}"))),
    @ApiResponse(responseCode = "401", description = "JWT manquant ou invalide"),
    @ApiResponse(responseCode = "403", description = "Rôle ROLE_ENTERPRISE ou ROLE_ADMIN requis")
  })
  public ResponseEntity<?> upload(
      @Parameter(
          description = "Champ multipart **`file`** : image (JPEG, PNG, WebP, etc.), types **image/***, max 5 Mo",
          required = true,
          content =
              @Content(
                  mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                  schema = @Schema(type = "string", format = "binary")))
      @RequestParam("file")
      MultipartFile file) {
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
