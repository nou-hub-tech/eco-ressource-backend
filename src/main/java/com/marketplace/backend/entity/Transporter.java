package com.marketplace.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transporters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transporter {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(nullable = false)
  private String companyName;

  private String sector;
  private String taxId;

  @Column(nullable = false)
  private Integer listingsCount;

  @Column(nullable = false)
  private Integer ordersCount;

  private String revenue;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @OneToMany(mappedBy = "transporter")
  @Builder.Default
  private List<Delivery> deliveries = new ArrayList<>();

  @OneToMany(mappedBy = "transporter")
  @Builder.Default
  private List<TransportOffer> transportOffers = new ArrayList<>();

  @PrePersist
  public void prePersist() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
    if (listingsCount == null) {
      listingsCount = 0;
    }
    if (ordersCount == null) {
      ordersCount = 0;
    }
  }
}