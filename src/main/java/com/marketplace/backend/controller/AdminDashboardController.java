package com.marketplace.backend.controller;

import com.marketplace.backend.service.UserService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

  private final UserService userService;

  @GetMapping("/stats")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Map<String, Object>> dashboardStats() {
    long users = userService.countNonAdminUsers();
    return ResponseEntity.ok(
        Map.of(
            "totalUsers", users,
            "activeListings", 0,
            "pendingApprovals", 0));
  }
}
