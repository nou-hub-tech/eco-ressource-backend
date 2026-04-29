package com.marketplace.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.marketplace.backend.entity.enums.SlotStatus;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
  name = "reservation_slots",
  indexes = {
    @Index(name = "idx_machine_date", columnList = "machine, date")
  }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationSlot {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "machine", nullable = false)
  private String machine;

  @Column(name = "date", nullable = false)
  private LocalDate date;

  @Column(name = "start_hour", nullable = false)
  private Integer startHour;

  @Column(name = "end_hour", nullable = false)
  private Integer endHour;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private SlotStatus status;

  @Column(name = "solar", nullable = false)
  private Boolean solar;

  @Column(name = "discount_pct", nullable = false)
  private Integer discountPct;

  // 🔥 FIX JSON LOOP (VERY IMPORTANT)
  @OneToOne(mappedBy = "slot")
  @JsonIgnore // avoid Reservation <-> Slot recursion + lazy serialization issues
  private Reservation reservation;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enterprise_id")
  @JsonIgnore // frontend doesn't need enterprise object for slot listing
  private Enterprise enterprise;

  @Column(name = "deleted")
  @Builder.Default
  private Boolean deleted = false;

  @Column(name = "cancel_reason")
  private String cancelReason;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @PrePersist
  public void prePersist() {
    if (createdAt == null) createdAt = LocalDateTime.now();
    if (deleted == null) deleted = false;
    if (discountPct == null) discountPct = 0;
    if (solar == null) solar = false;
  }
}
