package com.marketplace.backend.service;

import com.marketplace.backend.dto.CreateListingRequest;
import com.marketplace.backend.dto.GroupPurchaseResponse;
import com.marketplace.backend.dto.ListingMatchResponse;
import com.marketplace.backend.dto.ListingResponse;
import com.marketplace.backend.entity.GroupPurchase;
import com.marketplace.backend.entity.GroupParticipant;
import com.marketplace.backend.entity.PostAttachment;
import com.marketplace.backend.entity.Product;
import com.marketplace.backend.entity.ResourceListing;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.enums.GroupPurchaseStatus;
import com.marketplace.backend.entity.enums.Role;
import com.marketplace.backend.entity.enums.ResourceListingStatus;
import com.marketplace.backend.entity.enums.ListingType;
import com.marketplace.backend.repository.CommentRepository;
import com.marketplace.backend.repository.EnterpriseRepository;
import com.marketplace.backend.repository.FavoriteRepository;
import com.marketplace.backend.repository.GroupParticipantRepository;
import com.marketplace.backend.repository.GroupPurchaseRepository;
import com.marketplace.backend.repository.PostAttachmentRepository;
import com.marketplace.backend.repository.ProductRepository;
import com.marketplace.backend.repository.ResourceListingRepository;
import com.marketplace.backend.repository.TransporterRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResourceListingService {

  private final ResourceListingRepository listingRepository;
  private final ProductRepository productRepository;
  private final PostAttachmentRepository attachmentRepository;
  private final GroupPurchaseRepository groupPurchaseRepository;
  private final GroupParticipantRepository participantRepository;
  private final FavoriteRepository favoriteRepository;
  private final CommentRepository commentRepository;
  private final EnterpriseRepository enterpriseRepository;
  private final TransporterRepository transporterRepository;
  private final RealtimeNotificationService realtimeNotificationService;

  @Transactional
  public ListingResponse create(CreateListingRequest req) {
    Product product =
        productRepository
            .findById(req.getProductId())
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));

    ListingType type;
    try {
      type = ListingType.valueOf(req.getType().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Invalid listing type. Must be SURPLUS, DEMANDE, or GROUP_BUYING");
    }

    if (type == ListingType.GROUP_BUYING) {
      if (req.getTargetQuantity() == null || req.getTargetQuantity() <= 0) {
        throw new IllegalArgumentException(
            "target_quantity is required and must be positive for GROUP_BUYING");
      }
      if (req.getDeadline() == null) {
        throw new IllegalArgumentException("deadline is required for GROUP_BUYING");
      }
      if (req.getDeadline().isBefore(java.time.LocalDateTime.now())) {
        throw new IllegalArgumentException("deadline must be in the future");
      }
    }

    ResourceListing listing =
        ResourceListing.builder()
            .title(req.getTitle())
            .description(req.getDescription())
            .type(type)
            .status(ResourceListingStatus.ACTIVE)
            .quantity(req.getQuantity())
            .unit(req.getUnit())
            .price(req.getPrice())
            .location(req.getLocation())
            .latitude(req.getLatitude())
            .longitude(req.getLongitude())
            .product(product)
            .companyId(req.getCompanyId())
            .build();
    listing = listingRepository.save(listing);

    if (req.getAttachmentUrls() != null) {
      validateAttachmentUrls(req.getAttachmentUrls());
      for (String url : req.getAttachmentUrls()) {
        PostAttachment att =
            PostAttachment.builder().fileUrl(url).listing(listing).build();
        attachmentRepository.save(att);
      }
    }

    if (type == ListingType.GROUP_BUYING) {
      GroupPurchase group =
          GroupPurchase.builder()
              .listing(listing)
              .targetQuantity(req.getTargetQuantity())
              .currentQuantity(0)
              .deadline(req.getDeadline())
              .status(GroupPurchaseStatus.OPEN)
              .build();
      groupPurchaseRepository.save(group);
      listing.setGroupPurchase(group);
    }

    ListingResponse response = toResponse(listing);
    realtimeNotificationService.listingChanged("LISTING_CREATED", listing.getId(), response);
    return response;
  }

  @Transactional(readOnly = true)
  public List<ListingResponse> findAll() {
    return listingRepository.findByStatus(ResourceListingStatus.ACTIVE).stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<ListingResponse> findAllForAdmin() {
    return listingRepository.findAll().stream()
        .map(this::toResponse)
        .sorted(Comparator.comparing(ListingResponse::getCreatedAt).reversed())
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<ListingResponse> findMine(User user) {
    Long companyId = resolveActorCompanyId(user);
    return listingRepository.findByCompanyId(companyId).stream()
        .map(this::toResponse)
        .sorted(Comparator.comparing(ListingResponse::getCreatedAt).reversed())
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public ListingResponse getById(Long id) {
    ResourceListing listing =
        listingRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found"));
    ListingResponse response = toResponse(listing);
    realtimeNotificationService.listingChanged("LISTING_UPDATED", listing.getId(), response);
    return response;
  }

  @Transactional(readOnly = true)
  public List<ListingResponse> search(String type, String category, String location,
      Double maxPrice) {
    ListingType listingType = null;
    if (type != null) {
      try {
        listingType = ListingType.valueOf(type.toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Invalid listing type");
      }
    }

    List<ResourceListing> results =
        listingRepository.search(ResourceListingStatus.ACTIVE, listingType, category, location);

    if (maxPrice != null) {
      results =
          results.stream()
              .filter(
                  l ->
                      l.getPrice() == null
                          || l.getPrice().doubleValue() <= maxPrice)
              .collect(Collectors.toList());
    }

    return results.stream().map(this::toResponse).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<ListingResponse> trending(int limit) {
    return listingRepository.findByStatus(ResourceListingStatus.ACTIVE).stream()
        .map(this::toResponse)
        .sorted(Comparator.comparingInt(this::popularityScore).reversed())
        .limit(Math.max(1, Math.min(limit, 20)))
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<ListingMatchResponse> match(Long listingId, int limit) {
    ListingResponse source = getById(listingId);
    return listingRepository.findByStatus(ResourceListingStatus.ACTIVE).stream()
        .filter(l -> !l.getId().equals(listingId))
        .map(this::toResponse)
        .map(candidate -> toMatch(source, candidate))
        .filter(m -> m.getScore() > 0)
        .sorted(Comparator.comparing(ListingMatchResponse::getScore).reversed())
        .limit(Math.max(1, Math.min(limit, 12)))
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public BigDecimal suggestPrice(Long productId, String category, String location) {
    List<ResourceListing> listings = listingRepository.findByStatus(ResourceListingStatus.ACTIVE);
    List<BigDecimal> prices =
        listings.stream()
            .filter(l -> l.getPrice() != null)
            .filter(l -> productId == null || l.getProduct().getIdProduct().equals(productId))
            .filter(l -> category == null || category.isBlank()
                || category.equalsIgnoreCase(l.getProduct().getCategory()))
            .filter(l -> location == null || location.isBlank()
                || (l.getLocation() != null
                    && l.getLocation().toLowerCase().contains(location.toLowerCase())))
            .map(ResourceListing::getPrice)
            .toList();
    if (prices.isEmpty()) {
      prices = listings.stream()
          .filter(l -> l.getPrice() != null)
          .map(ResourceListing::getPrice)
          .toList();
    }
    if (prices.isEmpty()) {
      return BigDecimal.ZERO;
    }
    BigDecimal total = prices.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    return total.divide(BigDecimal.valueOf(prices.size()), 2, RoundingMode.HALF_UP);
  }

  @Transactional
  public ListingResponse update(Long id, CreateListingRequest req, Long requestingCompanyId) {
    ResourceListing listing =
        listingRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found"));

    if (!listing.getCompanyId().equals(requestingCompanyId)) {
      throw new IllegalArgumentException("Only the listing owner can update");
    }
    if (listing.getStatus() != ResourceListingStatus.ACTIVE) {
      throw new IllegalArgumentException("Only active listings can be updated");
    }

    Product product =
        productRepository
            .findById(req.getProductId())
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));

    listing.setTitle(req.getTitle());
    listing.setDescription(req.getDescription());
    listing.setQuantity(req.getQuantity());
    listing.setUnit(req.getUnit());
    listing.setPrice(req.getPrice());
    listing.setLocation(req.getLocation());
    listing.setLatitude(req.getLatitude());
    listing.setLongitude(req.getLongitude());
    listing.setProduct(product);

    if (req.getAttachmentUrls() != null) {
      validateAttachmentUrls(req.getAttachmentUrls());
      List<PostAttachment> existing = attachmentRepository.findByListingId(id);
      attachmentRepository.deleteAll(existing);
      for (String url : req.getAttachmentUrls()) {
        attachmentRepository.save(
            PostAttachment.builder().fileUrl(url).listing(listing).build());
      }
    }

    listing = listingRepository.save(listing);
    return toResponse(listing);
  }

  @Transactional
  public ListingResponse duplicate(Long id) {
    ResourceListing original =
        listingRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found"));

    ResourceListing copy =
        ResourceListing.builder()
            .title(original.getTitle() + " (copie)")
            .description(original.getDescription())
            .type(original.getType())
            .status(ResourceListingStatus.ACTIVE)
            .quantity(original.getQuantity())
            .unit(original.getUnit())
            .price(original.getPrice())
            .location(original.getLocation())
            .latitude(original.getLatitude())
            .longitude(original.getLongitude())
            .product(original.getProduct())
            .companyId(original.getCompanyId())
            .build();
    copy = listingRepository.save(copy);

    List<PostAttachment> originalAtts = attachmentRepository.findByListingId(id);
    for (PostAttachment att : originalAtts) {
      attachmentRepository.save(
          PostAttachment.builder().fileUrl(att.getFileUrl()).listing(copy).build());
    }

    ListingResponse response = toResponse(copy);
    realtimeNotificationService.listingChanged("LISTING_CREATED", copy.getId(), response);
    return response;
  }

  @Transactional
  public void cancel(Long id, Long companyId) {
    ResourceListing listing =
        listingRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found"));

    if (!listing.getCompanyId().equals(companyId)) {
      throw new IllegalArgumentException("Only the listing owner can cancel");
    }
    if (listing.getStatus() != ResourceListingStatus.ACTIVE) {
      throw new IllegalArgumentException("Only active listings can be cancelled");
    }

    listing.setStatus(ResourceListingStatus.CANCELLED);
    listingRepository.save(listing);

    if (listing.getGroupPurchase() != null) {
      GroupPurchase gp = listing.getGroupPurchase();
      gp.setStatus(GroupPurchaseStatus.CLOSED);
      groupPurchaseRepository.save(gp);
    }
    realtimeNotificationService.listingChanged("LISTING_CANCELLED", listing.getId(), toResponse(listing));
  }

  @Transactional
  public void delete(Long id, User actor) {
    ResourceListing listing =
        listingRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found"));

    boolean isAdmin = actor.getRole() == Role.ROLE_ADMIN;
    boolean isOwner = listing.getCompanyId().equals(resolveActorCompanyIdOrNull(actor));
    if (!isAdmin && !isOwner) {
      throw new IllegalArgumentException("Only the listing owner or admin can delete");
    }

    ListingResponse response = toResponse(listing);
    listingRepository.delete(listing);
    realtimeNotificationService.listingChanged("LISTING_DELETED", id, response);
  }

  private ListingResponse toResponse(ResourceListing listing) {
    List<String> attachmentUrls =
        attachmentRepository.findByListingId(listing.getId()).stream()
            .map(PostAttachment::getFileUrl)
            .collect(Collectors.toList());

    GroupPurchaseResponse gpResponse = null;
    if (listing.getType() == ListingType.GROUP_BUYING) {
      GroupPurchase gp =
          groupPurchaseRepository.findByListingId(listing.getId()).orElse(null);
      if (gp != null) {
        gpResponse =
            GroupPurchaseResponse.builder()
                .id(gp.getId())
                .listingId(listing.getId())
                .targetQuantity(gp.getTargetQuantity())
                .currentQuantity(gp.getCurrentQuantity())
                .remainingQuantity(gp.getTargetQuantity() - gp.getCurrentQuantity())
                .deadline(gp.getDeadline())
                .status(gp.getStatus().name())
                .participants(toParticipantResponses(gp.getId()))
                .build();
      }
    }

    long favCount = favoriteRepository.countByListingId(listing.getId());
    long commentCount = commentRepository.countByListing_Id(listing.getId());

    return ListingResponse.builder()
        .id(listing.getId())
        .title(listing.getTitle())
        .description(listing.getDescription())
        .type(listing.getType().name())
        .status(listing.getStatus().name())
        .quantity(listing.getQuantity())
        .unit(listing.getUnit())
        .price(listing.getPrice())
        .location(listing.getLocation())
        .latitude(listing.getLatitude())
        .longitude(listing.getLongitude())
        .productId(listing.getProduct().getIdProduct())
        .productName(listing.getProduct().getName())
        .productCategory(listing.getProduct().getCategory())
        .companyId(listing.getCompanyId())
        .companyName(resolveCompanyName(listing.getCompanyId()))
        .createdAt(listing.getCreatedAt())
        .attachmentUrls(attachmentUrls)
        .groupPurchase(gpResponse)
        .favoriteCount(favCount)
        .commentCount(commentCount)
        .build();
  }

  private int popularityScore(ListingResponse l) {
    int groupBoost = l.getGroupPurchase() == null ? 0 : l.getGroupPurchase().getParticipants().size() * 5;
    return (int) (l.getFavoriteCount() * 4 + l.getCommentCount() * 3 + groupBoost);
  }

  private ListingMatchResponse toMatch(ListingResponse source, ListingResponse candidate) {
    int score = 0;
    StringBuilder reason = new StringBuilder();
    if (safeEquals(source.getProductCategory(), candidate.getProductCategory())) {
      score += 35;
      reason.append("meme categorie; ");
    }
    if (source.getType() != null && candidate.getType() != null && !source.getType().equals(candidate.getType())) {
      score += 20;
      reason.append("type complementaire; ");
    }
    if (safeContains(source.getLocation(), candidate.getLocation())) {
      score += 20;
      reason.append("localisation proche; ");
    }
    if (source.getPrice() != null && candidate.getPrice() != null) {
      double a = source.getPrice().doubleValue();
      double b = candidate.getPrice().doubleValue();
      if (a > 0 && Math.abs(a - b) / a <= 0.25) {
        score += 15;
        reason.append("prix proche; ");
      }
    }
    score += Math.min(10, popularityScore(candidate));
    return ListingMatchResponse.builder()
        .listing(candidate)
        .score(Math.min(score, 100))
        .reason(reason.length() == 0 ? "annonce pertinente" : reason.toString())
        .build();
  }

  private boolean safeEquals(String a, String b) {
    return a != null && b != null && a.equalsIgnoreCase(b);
  }

  private boolean safeContains(String a, String b) {
    if (a == null || b == null) return false;
    String aa = a.toLowerCase();
    String bb = b.toLowerCase();
    return aa.contains(bb) || bb.contains(aa);
  }

  private List<GroupPurchaseResponse.ParticipantInfo> toParticipantResponses(Long groupId) {
    return participantRepository.findByGroupId(groupId).stream()
        .map(this::toParticipantResponse)
        .collect(Collectors.toList());
  }

  private GroupPurchaseResponse.ParticipantInfo toParticipantResponse(GroupParticipant participant) {
    return GroupPurchaseResponse.ParticipantInfo.builder()
        .id(participant.getId())
        .companyId(participant.getCompanyId())
        .companyName(resolveCompanyName(participant.getCompanyId()))
        .quantity(participant.getQuantity())
        .build();
  }

  private String resolveCompanyName(Long companyId) {
    if (companyId == null) {
      return null;
    }
    return enterpriseRepository
        .findById(companyId)
        .map(e -> e.getCompanyName())
        .orElseGet(
            () ->
                transporterRepository
                    .findById(companyId)
                    .map(t -> t.getCompanyName())
                .orElse(null));
  }

  private Long resolveActorCompanyId(User user) {
    Long companyId = resolveActorCompanyIdOrNull(user);
    if (companyId == null) {
      throw new IllegalArgumentException("Enterprise or transporter profile required");
    }
    return companyId;
  }

  private Long resolveActorCompanyIdOrNull(User user) {
    if (user == null) {
      return null;
    }
    if (user.getEnterprise() != null) {
      return user.getEnterprise().getId();
    }
    if (user.getTransporter() != null) {
      return user.getTransporter().getId();
    }
    return null;
  }

  /** Garde-fou aligné avec le front (~5 Mo fichier → ~7 Mo en Data URL). */
  private static void validateAttachmentUrls(List<String> urls) {
    final int maxChars = 8_000_000;
    for (String url : urls) {
      if (url == null || url.isBlank()) {
        throw new IllegalArgumentException("attachmentUrls ne doit pas contenir d'URL vide");
      }
      if (url.length() > maxChars) {
        throw new IllegalArgumentException(
            "Pièce jointe trop volumineuse (max ~5 Mo côté fichier, envoyer une image plus petite ou utiliser l'URL par défaut)");
      }
    }
  }
}
