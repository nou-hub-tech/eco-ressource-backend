package com.marketplace.backend.controller;

import com.marketplace.backend.dto.SolidarityAssociationRequest;
import com.marketplace.backend.entity.SolidarityAssociation;
import com.marketplace.backend.service.DiscordBotService;
import com.marketplace.backend.service.SolidarityAssociationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.security.SecurityUserHelper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/solidarity-associations")
@RequiredArgsConstructor
public class SolidarityAssociationController {

  private final SolidarityAssociationService solidarityAssociationService;
  private final SecurityUserHelper securityUserHelper;
  private final DiscordBotService discordBotService;

  @GetMapping
  public ResponseEntity<List<SolidarityAssociation>> list() {
    return ResponseEntity.ok(solidarityAssociationService.findAll());
  }

  @GetMapping("/{id}")
  public ResponseEntity<SolidarityAssociation> get(@PathVariable Long id) {
    try {
      SolidarityAssociation sa = solidarityAssociationService.getById(id);
      return ResponseEntity.ok(sa);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    }
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'ENTERPRISE')")
  public ResponseEntity<SolidarityAssociation> create(
      @Valid @RequestBody SolidarityAssociationRequest req, Authentication auth) {
    User currentUser = securityUserHelper.requireUser(auth);
    SolidarityAssociation created = solidarityAssociationService.create(req, currentUser.getId());
    
    discordBotService.sendNotification(String.format(
        "🆕 **New Solidarity Association Created**\n" +
        "🏢 **Name**: %s\n" +
        "📜 **Mission**: %s\n" +
        "👥 **Members**: %d\n" +
        "🎯 **Goal**: %.2f TND\n" +
        "👤 **Created By**: User ID %d (%s)",
        created.getName(), created.getMission(), created.getMembers(), 
        created.getGoalAmount(), currentUser.getId(), currentUser.getFullName()
    ));
    
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'ENTERPRISE')")
  public ResponseEntity<SolidarityAssociation> update(
      @PathVariable Long id, @Valid @RequestBody SolidarityAssociationRequest req, Authentication auth) {
    try {
      User currentUser = securityUserHelper.requireUser(auth);
      SolidarityAssociation updated = solidarityAssociationService.update(id, req, currentUser);
      discordBotService.sendNotification(String.format(
          "🔄 **Solidarity Association Updated**\n" +
          "🆔 **ID**: %d\n" +
          "🏢 **Name**: %s\n" +
          "📊 **New Status**: %s\n" +
          "👤 **Updated By**: %s",
          id, updated.getName(), updated.getStatusLabel(), currentUser.getFullName()
      ));
      return ResponseEntity.ok(updated);
    } catch (IllegalArgumentException e) {
      return ResponseEntity.notFound().build();
    } catch (org.springframework.security.access.AccessDeniedException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'ENTERPRISE')")
  public ResponseEntity<?> delete(@PathVariable Long id, Authentication auth) {
    try {
      User currentUser = securityUserHelper.requireUser(auth);
      SolidarityAssociation sa = solidarityAssociationService.getById(id);
      String name = sa.getName();
      
      solidarityAssociationService.delete(id, currentUser);
      discordBotService.sendNotification(String.format(
          "❌ **Solidarity Association Deleted**\n" +
          "🏢 **Name**: %s (ID: %d)\n" +
          "👤 **Deleted By**: User ID %d (%s)",
          name, id, currentUser.getId(), currentUser.getFullName()
      ));
      return ResponseEntity.noContent().build();
    } catch (IllegalArgumentException e) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    } catch (org.springframework.security.access.AccessDeniedException e) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
    } catch (Exception e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
    }
  }

  @DeleteMapping("/all")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<Void> deleteAll() {
    solidarityAssociationService.deleteAllAssociations();
    discordBotService.sendNotification("🗑️ ALL Solidarity Associations (and donations) have been DELETED by Admin.");
    return ResponseEntity.noContent().build();
  }
}

