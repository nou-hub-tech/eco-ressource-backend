package com.marketplace.backend.entity.workspace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.NotificationChannel;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.ReservationCategory;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.ReservationRole;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "workspace_reservations")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceReservation {

  @Id
  @Column(length = 20)
  private String id;

  @Column(nullable = false, unique = true, length = 20)
  private String code;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String customer;

  @Column(nullable = false)
  private String resource;

  @Column(nullable = false, length = 20)
  private String slotId;

  @Column(nullable = false)
  private String slotName;

  @Enumerated(EnumType.STRING)
  @Column(length = 40)
  private ReservationRole role;

  private String city;

  @Enumerated(EnumType.STRING)
  @Column(length = 40)
  private ReservationCategory category;

  @Column(nullable = false)
  private LocalDateTime startAt;

  @Column(nullable = false)
  private LocalDateTime endAt;

  @Column(nullable = false)
  private Integer headcount;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ReservationStatus status;

  @Column(columnDefinition = "TEXT")
  private String notes;

  @Column(columnDefinition = "LONGTEXT")
  private String voiceNoteTranscript;

  private String contactName;

  private String contactEmail;

  private String contactPhone;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private NotificationChannel notificationChannel;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private NotificationChannel confirmationChannel;

  private String confirmationDestination;

  private LocalDateTime confirmationSentAt;

  @Column(columnDefinition = "TEXT")
  private String confirmationSummary;

  @Convert(converter = WorkspaceJsonConverters.StringListJsonConverter.class)
  @Column(name = "tags_json", nullable = false, columnDefinition = "TEXT")
  @Builder.Default
  private List<String> tags = new ArrayList<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enterprise_id", nullable = false)
  @JsonIgnore
  private Enterprise enterprise;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @PrePersist
  public void prePersist() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }
}
