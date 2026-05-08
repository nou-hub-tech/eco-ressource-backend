package com.marketplace.backend.dto.workspace;

import com.marketplace.backend.entity.workspace.WorkspaceEnums.InsightTone;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.NotificationChannel;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.OrderStatus;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.PaymentStatus;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.ReservationCategory;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.ReservationRole;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.ReservationStatus;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.SlotPortfolio;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.SlotStatus;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.SlotType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class ReservationWorkspaceDtos {

  private ReservationWorkspaceDtos() {}

  public record OrderLineItemDto(
      @NotBlank String label,
      @NotNull @Min(1) Integer quantity,
      @NotNull @DecimalMin("0.0") BigDecimal unitPrice) {}

  public record TrackingStepDto(String label, String timestamp, boolean done) {}

  public record ReservationConflictDto(
      List<String> reservationIds,
      String slotId,
      String slotName,
      String overlapLabel,
      String severity) {}

  public record WorkspaceSummaryDto(
      int hostedReservations,
      int requestedReservations,
      BigDecimal receivableTotal,
      BigDecimal payableTotal,
      int conflictCount,
      int occupancySignal,
      int flaggedOrders,
      String bestWindow) {}

  public record InsightCardDto(
      String title,
      String description,
      String badge,
      InsightTone tone) {}

  public record ReservationConfirmationNoticeDto(
      NotificationChannel channel,
      String destination,
      LocalDateTime sentAt,
      String summary) {}

  public record ReservationNotificationEventDto(
      String reservationId,
      String reservationCode,
      String customer,
      NotificationChannel channel,
      String destination,
      LocalDateTime sentAt,
      String summary) {}

  public record ActivityCenterItemDto(
      String id,
      String audience,
      String entity,
      InsightTone tone,
      String badge,
      String title,
      String description,
      LocalDateTime occurredAt,
      String route,
      String actionLabel) {}

  public record WorkspaceReservationRequest(
      @NotBlank String title,
      @NotBlank String customer,
      @NotBlank String resource,
      @NotBlank String slotId,
      @NotNull ReservationRole role,
      @NotBlank String city,
      @NotNull ReservationCategory category,
      @NotNull LocalDateTime start,
      @NotNull LocalDateTime end,
      @NotNull @Min(1) Integer headcount,
      @NotNull @DecimalMin("0.0") BigDecimal amount,
      @NotNull ReservationStatus status,
      String notes,
      List<String> tags,
      @NotBlank String contactName,
      @Email String contactEmail,
      String contactPhone,
      @NotNull NotificationChannel notificationChannel,
      Long enterpriseId) {}

  public record WorkspaceSlotRequest(
      @NotBlank String name,
      @NotBlank String zone,
      @NotBlank String city,
      @NotBlank String ownerCompany,
      @NotNull SlotPortfolio portfolio,
      @NotNull SlotType type,
      @NotNull @Size(min = 2, max = 2) List<Double> coordinates,
      @NotNull @Min(1) Integer capacity,
      @NotNull @Min(0) Integer occupied,
      @NotNull SlotStatus status,
      List<String> equipment,
      Long enterpriseId) {}

  public record WorkspaceOrderRequest(
      @NotBlank String customer,
      @NotBlank String reservationId,
      @NotBlank String slotId,
      @NotNull ReservationRole role,
      @NotBlank String city,
      @NotNull @DecimalMin("0.0") BigDecimal amount,
      @NotNull @DecimalMin("0.0") BigDecimal tax,
      @NotNull LocalDate createdAt,
      @NotNull LocalDate dueDate,
      @NotNull OrderStatus status,
      @NotNull PaymentStatus paymentStatus,
      @NotEmpty List<@Valid OrderLineItemDto> items,
      Long enterpriseId) {}

  public record WorkspaceReservationResponse(
      String id,
      String code,
      String title,
      String customer,
      String resource,
      String slotId,
      String slotName,
      ReservationRole role,
      String city,
      ReservationCategory category,
      String providerCompany,
      String consumerCompany,
      LocalDateTime start,
      LocalDateTime end,
      Integer headcount,
      BigDecimal amount,
      ReservationStatus status,
      String notes,
      List<String> tags,
      String contactName,
      String contactEmail,
      String contactPhone,
      NotificationChannel notificationChannel,
      ReservationConfirmationNoticeDto confirmationNotice,
      String color,
      int cancellationRisk,
      String recommendedTime,
      String bookingSuggestion,
      Integer readinessScore,
      String serviceBrief,
      List<String> coordinationChecklist,
      String messageDraft,
      String ecoNote) {}

  public record WorkspaceSlotResponse(
      String id,
      String name,
      String zone,
      String city,
      String ownerCompany,
      SlotPortfolio portfolio,
      SlotType type,
      List<Double> coordinates,
      Integer capacity,
      Integer occupied,
      SlotStatus status,
      List<String> equipment,
      List<List<Integer>> heatmap,
      List<Integer> forecast,
      int utilizationRate,
      int predictedAvailability,
      int underusedScore,
      String recoveryAction,
      String quietWindow,
      String bestUseMode,
      String spotlightMessage,
      List<String> activationChecklist,
      String ecoFitNote) {}

  public record WorkspaceOrderResponse(
      String id,
      String code,
      String invoiceNumber,
      String customer,
      String reservationId,
      String slotId,
      ReservationRole role,
      String city,
      String buyerCompany,
      String sellerCompany,
      BigDecimal amount,
      BigDecimal tax,
      BigDecimal total,
      LocalDate createdAt,
      LocalDate dueDate,
      OrderStatus status,
      PaymentStatus paymentStatus,
      List<OrderLineItemDto> items,
      String qrValue,
      List<TrackingStepDto> tracking,
      int fraudRisk,
      String spendingCluster,
      String paymentInsight,
      String settlementPriority,
      String nextBestAction,
      String followUpDraft,
      List<String> financeChecklist,
      String relationshipTone) {}

  public record WorkspaceBootstrapResponse(
      WorkspaceSummaryDto summary,
      List<WorkspaceReservationResponse> reservations,
      List<WorkspaceSlotResponse> slots,
      List<WorkspaceOrderResponse> orders,
      List<ReservationConflictDto> conflicts,
      List<InsightCardDto> reservationInsights,
      List<InsightCardDto> slotInsights,
      List<InsightCardDto> orderInsights,
      List<ReservationNotificationEventDto> reservationNotifications,
      List<ActivityCenterItemDto> activityFeed) {}
}
