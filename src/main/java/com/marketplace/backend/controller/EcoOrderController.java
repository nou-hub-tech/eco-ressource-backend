package com.marketplace.backend.controller;

import com.marketplace.backend.dto.CancelOrderRequest;
import com.marketplace.backend.dto.EcoOrderRequest;
import com.marketplace.backend.entity.EcoOrder;
import com.marketplace.backend.service.EcoOrderService;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/eco-orders")
@RequiredArgsConstructor
public class EcoOrderController {

  private final EcoOrderService orderService;

  @GetMapping
  public ResponseEntity<List<Map<String, Object>>> list(
      Authentication auth,
      @RequestParam(name = "includeDeleted", required = false, defaultValue = "false")
          boolean includeDeleted) {
    return ResponseEntity.ok(orderService.findAll(auth, includeDeleted).stream().map(this::toResponse).toList());
  }

  @GetMapping("/{id}")
  public ResponseEntity<Map<String, Object>> get(@PathVariable Long id, Authentication auth) {
    return ResponseEntity.ok(toResponse(orderService.getById(id, auth)));
  }

  @PostMapping
  public ResponseEntity<Map<String, Object>> create(
      Authentication auth, @Valid @RequestBody EcoOrderRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(orderService.create(auth, req)));
  }

  @PutMapping("/{id}")
  public ResponseEntity<Map<String, Object>> update(
      @PathVariable Long id,
      Authentication auth,
      @Valid @RequestBody EcoOrderRequest req) {
    return ResponseEntity.ok(toResponse(orderService.update(id, auth, req)));
  }

  /** Status workflow advance: draft → confirmed → shipped → delivered. */
  @PostMapping("/{id}/advance")
  public ResponseEntity<Map<String, Object>> advance(@PathVariable Long id, Authentication auth) {
    return ResponseEntity.ok(toResponse(orderService.advanceStatus(id, auth)));
  }

  /** Soft delete with reason. */
  @PostMapping("/{id}/cancel")
  public ResponseEntity<Map<String, Object>> cancel(
      @PathVariable Long id,
      Authentication auth,
      @Valid @RequestBody(required = false) CancelOrderRequest req) {
    String reason = req == null ? null : req.getReason();
    return ResponseEntity.ok(toResponse(orderService.softDelete(id, auth, reason)));
  }

  @GetMapping("/total-co2")
  public ResponseEntity<Map<String, Object>> totalCo2(
      Authentication auth,
      @RequestParam(name = "includeDeleted", required = false, defaultValue = "false")
          boolean includeDeleted) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("totalCo2Saved", orderService.totalCo2Saved(auth, includeDeleted));
    return ResponseEntity.ok(body);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
    orderService.delete(id, auth);
    return ResponseEntity.noContent().build();
  }

  private Map<String, Object> toResponse(EcoOrder order) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("id", order.getId());
    body.put("ref", order.getRef());
    body.put("companyName", order.getCompanyName());
    body.put("material", order.getMaterial());
    body.put("qtyKg", order.getQtyKg());
    body.put("supplier", order.getSupplier());
    body.put("distanceKm", order.getDistanceKm());
    body.put("orderDate", order.getOrderDate());
    body.put("status", order.getStatus() == null ? null : order.getStatus().name());
    body.put("grade", order.getGrade() == null ? null : order.getGrade().name());
    body.put("co2Saved", order.getCo2Saved());
    body.put("waterSaved", order.getWaterSaved());
    body.put("wasteAvoided", order.getWasteAvoided());
    body.put("enterpriseId", order.getEnterprise() == null ? null : order.getEnterprise().getId());
    body.put(
        "enterprise",
        order.getEnterprise() == null
            ? null
            : Map.of(
                "id", order.getEnterprise().getId(),
                "companyName", order.getEnterprise().getCompanyName()));
    body.put("createdAt", order.getCreatedAt());
    body.put("deleted", order.getDeleted());
    body.put("cancelReason", order.getCancelReason());
    body.put("ecoScore", orderService.ecoScore(order));
    body.put("pricingImpact", orderService.pricingImpact(order));
    return body;
  }
}
