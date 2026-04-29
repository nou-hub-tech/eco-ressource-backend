package com.marketplace.backend.controller;

import com.marketplace.backend.dto.CancelOrderRequest;
import com.marketplace.backend.dto.EcoOrderRequest;
import com.marketplace.backend.entity.EcoOrder;
import com.marketplace.backend.service.EcoOrderService;
import jakarta.validation.Valid;
import java.util.List;
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
  public ResponseEntity<List<EcoOrder>> list(
      Authentication auth,
      @RequestParam(name = "includeDeleted", required = false, defaultValue = "false")
          boolean includeDeleted) {
    return ResponseEntity.ok(orderService.findAll(auth, includeDeleted));
  }

  @GetMapping("/{id}")
  public ResponseEntity<EcoOrder> get(@PathVariable Long id, Authentication auth) {
    return ResponseEntity.ok(orderService.getById(id, auth));
  }

  @PostMapping
  public ResponseEntity<EcoOrder> create(
      Authentication auth, @Valid @RequestBody EcoOrderRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(auth, req));
  }

  @PutMapping("/{id}")
  public ResponseEntity<EcoOrder> update(
      @PathVariable Long id,
      Authentication auth,
      @Valid @RequestBody EcoOrderRequest req) {
    return ResponseEntity.ok(orderService.update(id, auth, req));
  }

  /** Status workflow advance: draft → confirmed → shipped → delivered. */
  @PostMapping("/{id}/advance")
  public ResponseEntity<EcoOrder> advance(@PathVariable Long id, Authentication auth) {
    return ResponseEntity.ok(orderService.advanceStatus(id, auth));
  }

  /** Soft delete with reason. */
  @PostMapping("/{id}/cancel")
  public ResponseEntity<EcoOrder> cancel(
      @PathVariable Long id,
      Authentication auth,
      @Valid @RequestBody(required = false) CancelOrderRequest req) {
    String reason = req == null ? null : req.getReason();
    return ResponseEntity.ok(orderService.softDelete(id, auth, reason));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
    orderService.delete(id, auth);
    return ResponseEntity.noContent().build();
  }
}
