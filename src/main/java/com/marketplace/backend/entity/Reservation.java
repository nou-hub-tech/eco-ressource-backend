package com.marketplace.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.marketplace.backend.entity.enums.ReservationStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // =========================================================
  // CORE FIELDS
  // =========================================================

  @Column(name = "company", nullable = false)
  private String company;

  @Column(name = "machine", nullable = false)
  private String machine;

  // MySQL column name is `date` (existing reservations table).
  @Column(name = "date", nullable = false)
  private LocalDate date;

  @Column(name = "start_hour", nullable = false)
  private Integer startHour;

  @Column(name = "hours", nullable = false)
  private Integer hours;

  @Column(name = "hours_count", nullable = false)
  @JsonIgnore
  private Integer hoursCountCompat;

  @Column(name = "solar", nullable = false)
  private Boolean solar;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ReservationStatus status;

  // =========================================================
  // ECO
  // =========================================================

  // MySQL column name is `co2_saved` (schema migration).
  @Column(name = "co2_saved", precision = 10, scale = 2)
  private BigDecimal co2Saved;

  // =========================================================
  // RELATIONS
  // =========================================================

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "slot_id")
  @JsonIgnore // prevent lazy-loading + recursion during JSON serialization
  private ReservationSlot slot;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enterprise_id")
  @JsonIgnore // prevent lazy-loading + recursion during JSON serialization
  private Enterprise enterprise;

  // =========================================================
  // SYSTEM
  // =========================================================

  @Column(name = "deleted")
  @Builder.Default
  private Boolean deleted = false;

  @Column(name = "cancel_reason")
  private String cancelReason;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  // =========================================================
  // HOOK
  // =========================================================

  @PrePersist
  public void prePersist() {
    syncHoursColumns();
    if (createdAt == null) createdAt = LocalDateTime.now();
    if (deleted == null) deleted = false;
  }

  @PreUpdate
  public void preUpdate() {
    syncHoursColumns();
  }

  @PostLoad
  public void postLoad() {
    if (hours == null) {
      hours = hoursCountCompat;
    }
  }

  private void syncHoursColumns() {
    if (hours == null) {
      hours = hoursCountCompat;
    }
    hoursCountCompat = hours;
  }

  // =========================================================
  // JSON contract expected by the Angular frontend
  // =========================================================

  @JsonProperty("companyName")
  public String getCompanyName() {
    return company;
  }

  @JsonProperty("fromDate")
  public LocalDate getFromDate() {
    return date;
  }

  @JsonProperty("toDate")
  public LocalDate getToDate() {
    return date;
  }
}
