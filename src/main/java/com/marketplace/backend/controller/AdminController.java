package com.marketplace.backend.controller;

import com.marketplace.backend.dto.AdminUserDto;
import com.marketplace.backend.dto.EventDto;
import com.marketplace.backend.dto.ReservationDto;
import com.marketplace.backend.dto.SolidarityDto;
import com.marketplace.backend.dto.UserStatusRequest;
import com.marketplace.backend.dto.WalletTransactionDto;
import com.marketplace.backend.service.ListingService;
import com.marketplace.backend.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AdminController {




  
  private final UserService userService;
  private final ListingService listingService;

  @GetMapping("/api/users")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<AdminUserDto>> users() {
    return ResponseEntity.ok(userService.listNonAdminUsers());
  }

  @PatchMapping("/api/users/{id}/status")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AdminUserDto> userStatus(
      @PathVariable Long id, @Valid @RequestBody UserStatusRequest req) {
    try {
      return ResponseEntity.ok(userService.updateStatus(id, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/api/users/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    try {
      userService.deleteUser(id);
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

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


  @GetMapping("/api/admin/dashboard/stats")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<java.util.Map<String, Object>> dashboardStats() {
    long users = userService.listNonAdminUsers().size();
    return ResponseEntity.ok(
        java.util.Map.of(
            "totalUsers", users,
            "activeListings", 0,
            "pendingApprovals", 0));
  }
}
