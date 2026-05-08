package com.marketplace.backend.entity.workspace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.SlotPortfolio;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.SlotStatus;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.SlotType;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "workspace_reservation_slots")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceReservationSlot {

  @Id
  @Column(length = 20)
  private String id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String zone;

  private String city;

  private String ownerCompany;

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private SlotPortfolio portfolio;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SlotType type;

  @Convert(converter = WorkspaceJsonConverters.DoubleListJsonConverter.class)
  @Column(name = "coordinates_json", nullable = false, columnDefinition = "TEXT")
  @Builder.Default
  private List<Double> coordinates = new ArrayList<>();

  @Column(nullable = false)
  private Integer capacity;

  @Column(nullable = false)
  private Integer occupied;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SlotStatus status;

  @Convert(converter = WorkspaceJsonConverters.StringListJsonConverter.class)
  @Column(name = "equipment_json", nullable = false, columnDefinition = "TEXT")
  @Builder.Default
  private List<String> equipment = new ArrayList<>();

  @Convert(converter = WorkspaceJsonConverters.IntegerMatrixJsonConverter.class)
  @Column(name = "heatmap_json", nullable = false, columnDefinition = "LONGTEXT")
  @Builder.Default
  private List<List<Integer>> heatmap = new ArrayList<>();

  @Convert(converter = WorkspaceJsonConverters.IntegerListJsonConverter.class)
  @Column(name = "forecast_json", nullable = false, columnDefinition = "TEXT")
  @Builder.Default
  private List<Integer> forecast = new ArrayList<>();

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
