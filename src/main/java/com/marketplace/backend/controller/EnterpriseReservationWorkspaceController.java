package com.marketplace.backend.controller;

import com.marketplace.backend.dto.workspace.ReservationWorkspaceDtos.ActivityCenterItemDto;
import com.marketplace.backend.dto.workspace.ReservationWorkspaceDtos.InsightCardDto;
import com.marketplace.backend.dto.workspace.ReservationWorkspaceDtos.ReservationConflictDto;
import com.marketplace.backend.dto.workspace.ReservationWorkspaceDtos.ReservationNotificationEventDto;
import com.marketplace.backend.dto.workspace.ReservationWorkspaceDtos.WorkspaceBootstrapResponse;
import com.marketplace.backend.dto.workspace.ReservationWorkspaceDtos.WorkspaceOrderRequest;
import com.marketplace.backend.dto.workspace.ReservationWorkspaceDtos.WorkspaceOrderResponse;
import com.marketplace.backend.dto.workspace.ReservationWorkspaceDtos.WorkspaceReservationRequest;
import com.marketplace.backend.dto.workspace.ReservationWorkspaceDtos.WorkspaceReservationResponse;
import com.marketplace.backend.dto.workspace.ReservationWorkspaceDtos.WorkspaceSlotRequest;
import com.marketplace.backend.dto.workspace.ReservationWorkspaceDtos.WorkspaceSlotResponse;
import com.marketplace.backend.dto.workspace.ReservationWorkspaceDtos.WorkspaceSummaryDto;
import com.marketplace.backend.service.workspace.EnterpriseReservationWorkspaceService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enterprise/reservation-workspace")
@RequiredArgsConstructor
public class EnterpriseReservationWorkspaceController {

  private final EnterpriseReservationWorkspaceService workspaceService;

  @GetMapping("/bootstrap")
  public WorkspaceBootstrapResponse bootstrap(
      Authentication auth, @RequestParam(required = false) Long enterpriseId) {
    return workspaceService.bootstrap(auth, enterpriseId);
  }

  @GetMapping("/summary")
  public WorkspaceSummaryDto summary(
      Authentication auth, @RequestParam(required = false) Long enterpriseId) {
    return workspaceService.summary(auth, enterpriseId);
  }

  @GetMapping("/reservations")
  public List<WorkspaceReservationResponse> reservations(
      Authentication auth, @RequestParam(required = false) Long enterpriseId) {
    return workspaceService.listReservations(auth, enterpriseId);
  }

  @GetMapping("/reservations/{id}")
  public WorkspaceReservationResponse reservation(@PathVariable String id, Authentication auth) {
    return workspaceService.getReservation(id, auth);
  }

  @PostMapping("/reservations")
  @ResponseStatus(HttpStatus.CREATED)
  public WorkspaceReservationResponse createReservation(
      Authentication auth, @Valid @RequestBody WorkspaceReservationRequest request) {
    return workspaceService.createReservation(auth, request);
  }

  @PutMapping("/reservations/{id}")
  public WorkspaceReservationResponse updateReservation(
      @PathVariable String id,
      Authentication auth,
      @Valid @RequestBody WorkspaceReservationRequest request) {
    return workspaceService.updateReservation(id, auth, request);
  }

  @PatchMapping("/reservations/{id}/send-confirmation")
  public WorkspaceReservationResponse sendReservationConfirmation(
      @PathVariable String id, Authentication auth) {
    return workspaceService.sendReservationConfirmation(id, auth);
  }

  @DeleteMapping("/reservations/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteReservation(@PathVariable String id, Authentication auth) {
    workspaceService.deleteReservation(id, auth);
  }

  @GetMapping("/reservations/conflicts")
  public List<ReservationConflictDto> reservationConflicts(
      Authentication auth, @RequestParam(required = false) Long enterpriseId) {
    return workspaceService.reservationConflicts(auth, enterpriseId);
  }

  @GetMapping("/reservations/insights")
  public List<InsightCardDto> reservationInsights(
      Authentication auth, @RequestParam(required = false) Long enterpriseId) {
    return workspaceService.reservationInsights(auth, enterpriseId);
  }

  @GetMapping("/reservation-notifications")
  public List<ReservationNotificationEventDto> reservationNotifications(
      Authentication auth, @RequestParam(required = false) Long enterpriseId) {
    return workspaceService.reservationNotifications(auth, enterpriseId);
  }

  @GetMapping("/activity")
  public List<ActivityCenterItemDto> activityFeed(
      Authentication auth, @RequestParam(required = false) Long enterpriseId) {
    return workspaceService.activityFeed(auth, enterpriseId);
  }

  @GetMapping("/slots")
  public List<WorkspaceSlotResponse> slots(
      Authentication auth, @RequestParam(required = false) Long enterpriseId) {
    return workspaceService.listSlots(auth, enterpriseId);
  }

  @GetMapping("/slots/{id}")
  public WorkspaceSlotResponse slot(@PathVariable String id, Authentication auth) {
    return workspaceService.getSlot(id, auth);
  }

  @PostMapping("/slots")
  @ResponseStatus(HttpStatus.CREATED)
  public WorkspaceSlotResponse createSlot(
      Authentication auth, @Valid @RequestBody WorkspaceSlotRequest request) {
    return workspaceService.createSlot(auth, request);
  }

  @PutMapping("/slots/{id}")
  public WorkspaceSlotResponse updateSlot(
      @PathVariable String id, Authentication auth, @Valid @RequestBody WorkspaceSlotRequest request) {
    return workspaceService.updateSlot(id, auth, request);
  }

  @DeleteMapping("/slots/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteSlot(@PathVariable String id, Authentication auth) {
    workspaceService.deleteSlot(id, auth);
  }

  @GetMapping("/slots/insights")
  public List<InsightCardDto> slotInsights(
      Authentication auth, @RequestParam(required = false) Long enterpriseId) {
    return workspaceService.slotInsights(auth, enterpriseId);
  }

  @GetMapping("/orders")
  public List<WorkspaceOrderResponse> orders(
      Authentication auth, @RequestParam(required = false) Long enterpriseId) {
    return workspaceService.listOrders(auth, enterpriseId);
  }

  @GetMapping("/orders/{id}")
  public WorkspaceOrderResponse order(@PathVariable String id, Authentication auth) {
    return workspaceService.getOrder(id, auth);
  }

  @PostMapping("/orders")
  @ResponseStatus(HttpStatus.CREATED)
  public WorkspaceOrderResponse createOrder(
      Authentication auth, @Valid @RequestBody WorkspaceOrderRequest request) {
    return workspaceService.createOrder(auth, request);
  }

  @PutMapping("/orders/{id}")
  public WorkspaceOrderResponse updateOrder(
      @PathVariable String id,
      Authentication auth,
      @Valid @RequestBody WorkspaceOrderRequest request) {
    return workspaceService.updateOrder(id, auth, request);
  }

  @PatchMapping("/orders/{id}/advance-status")
  public WorkspaceOrderResponse advanceOrder(@PathVariable String id, Authentication auth) {
    return workspaceService.advanceOrderStatus(id, auth);
  }

  @DeleteMapping("/orders/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteOrder(@PathVariable String id, Authentication auth) {
    workspaceService.deleteOrder(id, auth);
  }

  @GetMapping("/orders/insights")
  public List<InsightCardDto> orderInsights(
      Authentication auth, @RequestParam(required = false) Long enterpriseId) {
    return workspaceService.orderInsights(auth, enterpriseId);
  }
}
