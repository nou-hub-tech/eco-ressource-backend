package com.marketplace.backend.controller;

import com.marketplace.backend.dto.EventDto;
import com.marketplace.backend.dto.ReservationDto;
import com.marketplace.backend.dto.SolidarityDto;
import com.marketplace.backend.dto.WalletTransactionDto;
import com.marketplace.backend.entity.StockItem;
import com.marketplace.backend.service.IStockItemService;
import com.marketplace.backend.service.UserService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminController {

  private final UserService userService;
  private final IStockItemService stockItemService;

  @GetMapping("/api/admin/events")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<EventDto>> events() {
    return ResponseEntity.ok(userService.allEvents());
  }

  @GetMapping("/api/admin/reservations")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<ReservationDto>> reservations() {
    return ResponseEntity.ok(userService.allReservations());
  }

  @GetMapping("/api/admin/solidarity")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<SolidarityDto>> solidarity() {
    return ResponseEntity.ok(userService.allSolidarity());
  }

  @GetMapping("/api/admin/treasury/transactions")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<WalletTransactionDto>> treasury() {
    return ResponseEntity.ok(userService.allWalletTransactions());
  }

  @GetMapping("/api/admin/stock/items")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<Map<String, Object>>> adminStockItems() {
    List<StockItem> items = stockItemService.retrieveAllStockItems();
    List<Map<String, Object>> result =
        items.stream()
            .map(
                item -> {
                  Map<String, Object> dto = new HashMap<>();
                  dto.put("id", item.getIdStock());
                  dto.put("name", item.getProduct() != null ? item.getProduct().getName() : "");
                  dto.put("category", item.getProduct() != null ? item.getProduct().getCategory() : "");
                  dto.put("qty", item.getQuantity());
                  dto.put("unit", item.getUnit());
                  dto.put("condition", item.getCondition());
                  dto.put("status", item.getStatus());
                  dto.put("location", item.getLocation());
                  dto.put("expirationDate", item.getExpirationDate());
                  dto.put("unitPrice", item.getUnitPrice());
                  dto.put("image", item.getImage() != null ? item.getImage() : "");
                  return dto;
                })
            .collect(Collectors.toList());
    return ResponseEntity.ok(result);
  }
}
