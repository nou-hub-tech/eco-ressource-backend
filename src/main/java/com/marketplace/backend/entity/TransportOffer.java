package com.marketplace.backend.entity;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import com.marketplace.backend.entity.enums.TransportOfferStatus;
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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "transport_offers")

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportOffer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "transporter_id", nullable = false)
  private Transporter transporter;

  @Column(nullable = false)
  private String fromLocation;

  @Column(nullable = false)
  private String toLocation;

  @Column(nullable = false)
  private String cargoDescription;

  @Column(nullable = false)
  private String weightLabel;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal proposedEarn;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private TransportOfferStatus status;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @PrePersist
  public void prePersist() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }

}

