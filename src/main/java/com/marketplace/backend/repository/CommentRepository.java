package com.marketplace.backend.repository;

import com.marketplace.backend.entity.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

  List<Comment> findByListingIdOrderByCreatedAtAsc(Long listingId);

  List<Comment> findByListingIdAndParentIsNullOrderByCreatedAtAsc(Long listingId);

  long countByUserIdAndCreatedAtAfter(Long userId, java.time.LocalDateTime after);
}
