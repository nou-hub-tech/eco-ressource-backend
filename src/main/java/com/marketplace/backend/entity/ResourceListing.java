package com.marketplace.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.marketplace.backend.entity.enums.ResourceListingStatus;
import com.marketplace.backend.entity.enums.ListingType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "resource_listings")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceListing {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false, length = 2000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ListingType type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ResourceListingStatus status;

  @Column(nullable = false)
  private Integer quantity;

  @Column(nullable = false)
  private String unit;

  @Column(precision = 14, scale = 2)
  private BigDecimal price;

  private String location;

  private Double latitude;

  private Double longitude;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  @JsonIgnore
  private List<PostAttachment> attachments = new ArrayList<>();

  @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  @JsonIgnore
  private List<Comment> comments = new ArrayList<>();

  @OneToOne(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
  @JsonIgnore
  private GroupPurchase groupPurchase;

  @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  @JsonIgnore
  private List<Favorite> favorites = new ArrayList<>();

  @PrePersist
  public void prePersist() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
    if (status == null) {
      status = ResourceListingStatus.ACTIVE;
    }
  }
}
