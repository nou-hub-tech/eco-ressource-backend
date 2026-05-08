package com.marketplace.backend.entity.workspace;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.ReservationRole;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.OrderStatus;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.PaymentStatus;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "workspace_orders")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkspaceOrder {

  @Id
  @Column(length = 20)
  private String id;

  @Column(nullable = false, unique = true, length = 20)
  private String code;

  @Column(nullable = false, unique = true, length = 20)
  private String invoiceNumber;

  @Column(nullable = false)
  private String customer;

  @Column(nullable = false, length = 20)
  private String reservationId;

  @Column(nullable = false, length = 20)
  private String slotId;

  @Enumerated(EnumType.STRING)
  @Column(length = 40)
  private ReservationRole role;

  private String city;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal tax;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal total;

  @Column(nullable = false)
  private LocalDate createdAt;

  @Column(nullable = false)
  private LocalDate dueDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PaymentStatus paymentStatus;

  @Convert(converter = WorkspaceJsonConverters.OrderLineItemListJsonConverter.class)
  @Column(name = "items_json", nullable = false, columnDefinition = "LONGTEXT")
  @Builder.Default
  private List<WorkspacePayloads.OrderLineItem> items = new ArrayList<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "enterprise_id", nullable = false)
  @JsonIgnore
  private Enterprise enterprise;

  @Column(nullable = false)
  private LocalDateTime createdTimestamp;

  @PrePersist
  public void prePersist() {
    if (createdTimestamp == null) {
      createdTimestamp = LocalDateTime.now();
    }
  }
}
