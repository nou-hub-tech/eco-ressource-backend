package com.marketplace.backend.controller;

import com.marketplace.backend.dto.CommentResponse;
import com.marketplace.backend.dto.CreateCommentRequest;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.security.SecurityUserHelper;
import com.marketplace.backend.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Fil de commentaires threades par annonce")
public class CommentController {

  private final CommentService commentService;
  private final SecurityUserHelper securityUserHelper;

  @PostMapping("/api/resource-listings/{listingId}/comments")
  @Operation(summary = "Ajouter un commentaire (reponse threaded via parentId)")
  public ResponseEntity<CommentResponse> create(
      @PathVariable Long listingId,
      Authentication auth,
      @Valid @RequestBody CreateCommentRequest req) {
    User user = securityUserHelper.requireUser(auth);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(commentService.create(listingId, user.getId(), req));
  }

  @GetMapping("/api/resource-listings/{listingId}/comments")
  @Operation(summary = "Lister les commentaires d'une annonce (arbre threade)")
  public ResponseEntity<List<CommentResponse>> list(
      @PathVariable Long listingId, Authentication auth) {
    User user = null;
    if (auth != null
        && auth.isAuthenticated()
        && auth.getName() != null
        && !"anonymousUser".equals(auth.getName())) {
      user = securityUserHelper.requireUser(auth);
    }
    return ResponseEntity.ok(commentService.findByListing(listingId, user));
  }

  @PutMapping("/api/comments/{commentId}")
  @Operation(summary = "Modifier son propre commentaire")
  public ResponseEntity<CommentResponse> update(
      @PathVariable Long commentId,
      Authentication auth,
      @Valid @RequestBody CreateCommentRequest req) {
    User user = securityUserHelper.requireUser(auth);
    return ResponseEntity.ok(
        commentService.update(commentId, user.getId(), req.getContent()));
  }

  @DeleteMapping("/api/comments/{commentId}")
  @Operation(summary = "Supprimer un commentaire (auteur, proprietaire de l’annonce, ou admin)")
  public ResponseEntity<Void> delete(@PathVariable Long commentId, Authentication auth) {
    User user = securityUserHelper.requireUser(auth);
    boolean isAdmin = user.getRole().name().equals("ROLE_ADMIN");
    commentService.delete(commentId, user, isAdmin);
    return ResponseEntity.noContent().build();
  }
}
