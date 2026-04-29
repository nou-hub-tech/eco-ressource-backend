package com.marketplace.backend.controller;

import com.marketplace.backend.dto.AdminUserDto;
import com.marketplace.backend.dto.UserCreateRequest;
import com.marketplace.backend.dto.UserStatusRequest;
import com.marketplace.backend.dto.UserUpdateRequest;
import com.marketplace.backend.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<AdminUserDto>> list() {
    return ResponseEntity.ok(userService.listNonAdminUsers());
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AdminUserDto> get(@PathVariable Long id) {
    try {
      return ResponseEntity.ok(userService.getForAdmin(id));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AdminUserDto> create(@Valid @RequestBody UserCreateRequest req) {
    try {
      return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AdminUserDto> update(
      @PathVariable Long id, @Valid @RequestBody UserUpdateRequest req) {
    try {
      return ResponseEntity.ok(userService.update(id, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.badRequest().build();
    }
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<AdminUserDto> userStatus(
      @PathVariable Long id, @Valid @RequestBody UserStatusRequest req) {
    try {
      return ResponseEntity.ok(userService.updateStatus(id, req));
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    try {

      userService.deleteUser(id);

      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }
}
