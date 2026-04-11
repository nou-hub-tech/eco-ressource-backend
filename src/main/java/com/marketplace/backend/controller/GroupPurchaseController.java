package com.marketplace.backend.controller;

import com.marketplace.backend.dto.GroupPurchaseResponse;
import com.marketplace.backend.dto.JoinGroupRequest;
import com.marketplace.backend.service.GroupPurchaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Tag(name = "Group Buying", description = "Gestion des achats groupes")
public class GroupPurchaseController {

  private final GroupPurchaseService groupPurchaseService;

  @GetMapping("/{id}")
  @Operation(summary = "Detail d'un groupe (quantites, deadline, status, participants)")
  public ResponseEntity<GroupPurchaseResponse> get(@PathVariable Long id) {
    return ResponseEntity.ok(groupPurchaseService.getById(id));
  }

  @PostMapping("/{id}/join")
  @Operation(summary = "Rejoindre un groupe avec une quantite")
  public ResponseEntity<GroupPurchaseResponse> join(
      @PathVariable Long id, @Valid @RequestBody JoinGroupRequest req) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(groupPurchaseService.join(id, req));
  }

  @DeleteMapping("/{id}/leave")
  @Operation(summary = "Quitter un groupe")
  public ResponseEntity<GroupPurchaseResponse> leave(
      @PathVariable Long id, @RequestParam Long companyId) {
    return ResponseEntity.ok(groupPurchaseService.leave(id, companyId));
  }

  @GetMapping("/{id}/participants")
  @Operation(summary = "Lister les participants d'un groupe")
  public ResponseEntity<List<GroupPurchaseResponse.ParticipantInfo>> participants(
      @PathVariable Long id) {
    return ResponseEntity.ok(groupPurchaseService.getParticipants(id));
  }
}
