package com.marketplace.backend.service;

import com.marketplace.backend.dto.CommentResponse;
import com.marketplace.backend.dto.CreateCommentRequest;
import com.marketplace.backend.entity.Comment;
import com.marketplace.backend.entity.ResourceListing;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.enums.ResourceListingStatus;
import com.marketplace.backend.repository.CommentRepository;
import com.marketplace.backend.repository.ResourceListingRepository;
import com.marketplace.backend.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

  private static final int ANTI_SPAM_WINDOW_SECONDS = 30;
  private static final int ANTI_SPAM_MAX_COMMENTS = 3;

  private final CommentRepository commentRepository;
  private final ResourceListingRepository listingRepository;
  private final UserRepository userRepository;

  @Transactional
  public CommentResponse create(Long listingId, Long userId, CreateCommentRequest req) {
    ResourceListing listing =
        listingRepository
            .findById(listingId)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found"));

    if (listing.getStatus() != ResourceListingStatus.ACTIVE) {
      throw new IllegalArgumentException("Cannot comment on a non-active listing");
    }

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

    long recentCount =
        commentRepository.countByUserIdAndCreatedAtAfter(
            userId, LocalDateTime.now().minusSeconds(ANTI_SPAM_WINDOW_SECONDS));
    if (recentCount >= ANTI_SPAM_MAX_COMMENTS) {
      throw new IllegalArgumentException("Too many comments in a short period. Please wait.");
    }

    Comment parent = null;
    if (req.getParentId() != null) {
      parent =
          commentRepository
              .findById(req.getParentId())
              .orElseThrow(() -> new IllegalArgumentException("Parent comment not found"));
      if (!parent.getListing().getId().equals(listingId)) {
        throw new IllegalArgumentException(
            "Parent comment must belong to the same listing");
      }
    }

    Comment comment =
        Comment.builder()
            .content(req.getContent())
            .user(user)
            .listing(listing)
            .parent(parent)
            .build();
    comment = commentRepository.save(comment);

    return toResponse(comment);
  }

  @Transactional(readOnly = true)
  public List<CommentResponse> findByListing(Long listingId) {
    listingRepository
        .findById(listingId)
        .orElseThrow(() -> new IllegalArgumentException("Listing not found"));

    List<Comment> rootComments =
        commentRepository.findByListing_IdAndParentIsNullOrderByCreatedAtAsc(listingId);

    return rootComments.stream().map(this::toResponseWithReplies).collect(Collectors.toList());
  }

  @Transactional
  public CommentResponse update(Long commentId, Long userId, String newContent) {
    Comment comment =
        commentRepository
            .findById(commentId)
            .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

    if (!comment.getUser().getId().equals(userId)) {
      throw new IllegalArgumentException("Only the comment owner can edit");
    }

    if (newContent == null || newContent.isBlank()) {
      throw new IllegalArgumentException("Comment content cannot be empty");
    }
    if (newContent.length() > 2000) {
      throw new IllegalArgumentException("Comment content must not exceed 2000 characters");
    }

    comment.setContent(newContent);
    comment = commentRepository.save(comment);
    return toResponse(comment);
  }

  @Transactional
  public void delete(Long commentId, User actor, boolean isAdmin) {
    Comment comment =
        commentRepository
            .findById(commentId)
            .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

    boolean isCommentOwner = comment.getUser().getId().equals(actor.getId());
    boolean isListingOwner = ownsListing(actor, comment.getListing());

    if (!isCommentOwner && !isAdmin && !isListingOwner) {
      throw new IllegalArgumentException(
          "Only the comment owner, listing owner, or admin can delete");
    }

    deleteCommentAndDescendants(comment.getId());
  }

  /** Le propriétaire de l’annonce (entreprise ou transporteur) correspond au companyId du listing. */
  private boolean ownsListing(User user, ResourceListing listing) {
    if (listing == null || listing.getCompanyId() == null) {
      return false;
    }
    Long cid = listing.getCompanyId();
    if (user.getEnterprise() != null && cid.equals(user.getEnterprise().getId())) {
      return true;
    }
    return user.getTransporter() != null && cid.equals(user.getTransporter().getId());
  }

  private void deleteCommentAndDescendants(Long commentId) {
    for (Comment child : commentRepository.findByParent_Id(commentId)) {
      deleteCommentAndDescendants(child.getId());
    }
    commentRepository.deleteById(commentId);
  }

  private CommentResponse toResponse(Comment comment) {
    return CommentResponse.builder()
        .id(comment.getId())
        .content(comment.getContent())
        .userId(comment.getUser().getId())
        .userFullName(comment.getUser().getFullName())
        .listingId(comment.getListing().getId())
        .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
        .createdAt(comment.getCreatedAt())
        .replies(List.of())
        .build();
  }

  private CommentResponse toResponseWithReplies(Comment comment) {
    List<CommentResponse> replies =
        comment.getReplies().stream()
            .map(this::toResponseWithReplies)
            .collect(Collectors.toList());

    return CommentResponse.builder()
        .id(comment.getId())
        .content(comment.getContent())
        .userId(comment.getUser().getId())
        .userFullName(comment.getUser().getFullName())
        .listingId(comment.getListing().getId())
        .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
        .createdAt(comment.getCreatedAt())
        .replies(replies)
        .build();
  }
}
