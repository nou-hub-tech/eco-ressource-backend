package com.marketplace.backend.repository;

import com.marketplace.backend.entity.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

  /**
   * Décompte explicite via {@code listing.id} — évite toute ambiguïté avec une propriété
   * fantôme {@code listingId}, et inclut bien racines + réponses (toutes les lignes liées à
   * l’annonce).
   */
  long countByListing_Id(Long listingId);

  List<Comment> findByListing_IdOrderByCreatedAtAsc(Long listingId);

  List<Comment> findByListing_IdAndParentIsNullOrderByCreatedAtAsc(Long listingId);

  long countByUserIdAndCreatedAtAfter(Long userId, java.time.LocalDateTime after);

  /** Réponses directes d’un commentaire (pour suppression récursive). */
  List<Comment> findByParent_Id(Long parentId);
}
