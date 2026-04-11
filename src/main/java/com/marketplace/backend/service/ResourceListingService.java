package com.marketplace.backend.service;

import com.marketplace.backend.dto.CreateListingRequest;
import com.marketplace.backend.dto.GroupPurchaseResponse;
import com.marketplace.backend.dto.ListingResponse;
import com.marketplace.backend.entity.GroupPurchase;
import com.marketplace.backend.entity.PostAttachment;
import com.marketplace.backend.entity.Product;
import com.marketplace.backend.entity.ResourceListing;
import com.marketplace.backend.entity.enums.GroupPurchaseStatus;
import com.marketplace.backend.entity.enums.ResourceListingStatus;
import com.marketplace.backend.entity.enums.ListingType;
import com.marketplace.backend.repository.CommentRepository;
import com.marketplace.backend.repository.FavoriteRepository;
import com.marketplace.backend.repository.GroupPurchaseRepository;
import com.marketplace.backend.repository.PostAttachmentRepository;
import com.marketplace.backend.repository.ProductRepository;
import com.marketplace.backend.repository.ResourceListingRepository;
import java.util.ArrayList;
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
  private final FavoriteRepository favoriteRepository;
  private final CommentRepository commentRepository;

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

    return toResponse(listing);
  }

  @Transactional(readOnly = true)
  public List<ListingResponse> findAll() {
    return listingRepository.findByStatus(ResourceListingStatus.ACTIVE).stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public ListingResponse getById(Long id) {
    ResourceListing listing =
        listingRepository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Listing not found"));
    return toResponse(listing);
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

    return toResponse(copy);
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
                .participants(new ArrayList<>())
                .build();
      }
    }

    long favCount = favoriteRepository.countByListingId(listing.getId());
    long commentCount =
        commentRepository.findByListingIdOrderByCreatedAtAsc(listing.getId()).size();

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
        .createdAt(listing.getCreatedAt())
        .attachmentUrls(attachmentUrls)
        .groupPurchase(gpResponse)
        .favoriteCount(favCount)
        .commentCount(commentCount)
        .build();
  }
}
