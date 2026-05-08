package com.marketplace.backend.service.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.marketplace.backend.entity.workspace.WorkspaceEnums.NotificationChannel;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.OrderStatus;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.PaymentStatus;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.ReservationCategory;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.ReservationRole;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.ReservationStatus;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.SlotPortfolio;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.SlotStatus;
import com.marketplace.backend.entity.workspace.WorkspaceEnums.SlotType;
import com.marketplace.backend.entity.workspace.WorkspaceOrder;
import com.marketplace.backend.entity.workspace.WorkspacePayloads;
import com.marketplace.backend.entity.workspace.WorkspaceReservation;
import com.marketplace.backend.entity.workspace.WorkspaceReservationSlot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorkspaceMlServiceTest {

  private final WorkspaceMlService service = new WorkspaceMlService();

  @Test
  void analyzesAllWorkspaceEntitiesWithRealModelOutputs() {
    List<WorkspaceReservationSlot> slots =
        List.of(
            WorkspaceReservationSlot.builder()
                .id("SLOT-1")
                .name("Charguia Dock")
                .zone("Charguia")
                .city("Tunis")
                .ownerCompany("Eco Ressource")
                .portfolio(SlotPortfolio.owned)
                .type(SlotType.Dock)
                .coordinates(List.of(10.21, 36.84))
                .capacity(14)
                .occupied(10)
                .status(SlotStatus.peak)
                .equipment(List.of("Dock leveller", "Forklift"))
                .forecast(List.of(72, 76, 79, 81, 84, 82, 78))
                .build(),
            WorkspaceReservationSlot.builder()
                .id("SLOT-2")
                .name("Sfax Storage Bay")
                .zone("Poudriere")
                .city("Sfax")
                .ownerCompany("Eco Ressource")
                .portfolio(SlotPortfolio.partner)
                .type(SlotType.Storage)
                .coordinates(List.of(10.75, 34.74))
                .capacity(26)
                .occupied(8)
                .status(SlotStatus.available)
                .equipment(List.of("Racks"))
                .forecast(List.of(38, 35, 34, 32, 31, 29, 28))
                .build());

    List<WorkspaceReservation> reservations =
        List.of(
            WorkspaceReservation.builder()
                .id("RES-1")
                .code("RSV-1")
                .title("Forklift support")
                .customer("Green Factory")
                .resource("Forklift line")
                .slotId("SLOT-1")
                .slotName("Charguia Dock")
                .role(ReservationRole.provider)
                .city("Tunis")
                .category(ReservationCategory.Machine)
                .startAt(LocalDateTime.now().plusDays(4))
                .endAt(LocalDateTime.now().plusDays(4).plusHours(3))
                .headcount(6)
                .amount(new BigDecimal("1800"))
                .status(ReservationStatus.confirmed)
                .notes("Operator ready")
                .contactName("Nour")
                .contactEmail("nour@example.com")
                .notificationChannel(NotificationChannel.email)
                .build(),
            WorkspaceReservation.builder()
                .id("RES-2")
                .code("RSV-2")
                .title("Overflow storage")
                .customer("Circular Trade")
                .resource("Storage aisles")
                .slotId("SLOT-2")
                .slotName("Sfax Storage Bay")
                .role(ReservationRole.consumer)
                .city("Sfax")
                .category(ReservationCategory.Storage)
                .startAt(LocalDateTime.now().plusHours(18))
                .endAt(LocalDateTime.now().plusHours(28))
                .headcount(12)
                .amount(new BigDecimal("3200"))
                .status(ReservationStatus.risk)
                .notes("")
                .contactName("Amel")
                .contactPhone("+21620000000")
                .notificationChannel(NotificationChannel.sms)
                .build(),
            WorkspaceReservation.builder()
                .id("RES-3")
                .code("RSV-3")
                .title("Partner review")
                .customer("North Hub")
                .resource("Meeting bay")
                .slotId("SLOT-2")
                .slotName("Sfax Storage Bay")
                .role(ReservationRole.provider)
                .city("Sfax")
                .category(ReservationCategory.Space)
                .startAt(LocalDateTime.now().plusDays(6))
                .endAt(LocalDateTime.now().plusDays(6).plusHours(2))
                .headcount(4)
                .amount(new BigDecimal("900"))
                .status(ReservationStatus.pending)
                .notes("Waiting for final access list")
                .contactName("Salma")
                .contactEmail("salma@example.com")
                .notificationChannel(NotificationChannel.both)
                .build());

    List<WorkspaceOrder> orders =
        List.of(
            WorkspaceOrder.builder()
                .id("ORD-1")
                .code("ORD-1")
                .invoiceNumber("INV-1")
                .customer("Green Factory")
                .reservationId("RES-1")
                .slotId("SLOT-1")
                .role(ReservationRole.provider)
                .city("Tunis")
                .amount(new BigDecimal("1800"))
                .tax(new BigDecimal("342"))
                .total(new BigDecimal("2142"))
                .createdAt(LocalDate.now().minusDays(2))
                .dueDate(LocalDate.now().plusDays(9))
                .status(OrderStatus.processing)
                .paymentStatus(PaymentStatus.pending)
                .items(
                    List.of(
                        new WorkspacePayloads.OrderLineItem("Forklift slot", 1, new BigDecimal("1800"))))
                .build(),
            WorkspaceOrder.builder()
                .id("ORD-2")
                .code("ORD-2")
                .invoiceNumber("INV-2")
                .customer("Circular Trade")
                .reservationId("RES-2")
                .slotId("SLOT-2")
                .role(ReservationRole.consumer)
                .city("Sfax")
                .amount(new BigDecimal("3200"))
                .tax(new BigDecimal("608"))
                .total(new BigDecimal("3808"))
                .createdAt(LocalDate.now().minusDays(5))
                .dueDate(LocalDate.now().plusDays(2))
                .status(OrderStatus.flagged)
                .paymentStatus(PaymentStatus.review)
                .items(
                    List.of(
                        new WorkspacePayloads.OrderLineItem("Storage block", 2, new BigDecimal("1600"))))
                .build());

    WorkspaceMlService.WorkspaceMlAnalysis analysis = service.analyze(slots, reservations, orders);

    assertEquals(2, analysis.slotProfiles().size());
    assertEquals(3, analysis.reservationProfiles().size());
    assertEquals(2, analysis.orderProfiles().size());

    WorkspaceMlService.ReservationMlProfile riskyReservation = analysis.reservationProfiles().get("RES-2");
    WorkspaceMlService.OrderMlProfile flaggedOrder = analysis.orderProfiles().get("ORD-2");
    WorkspaceMlService.SlotMlProfile availableSlot = analysis.slotProfiles().get("SLOT-2");

    assertTrue(riskyReservation.cancellationRisk() >= 50);
    assertTrue(flaggedOrder.fraudRisk() >= 50);
    assertTrue(availableSlot.predictedAvailability() >= 20);
    assertFalse(riskyReservation.topDrivers().isEmpty());
    assertFalse(flaggedOrder.topDrivers().isEmpty());
  }
}
