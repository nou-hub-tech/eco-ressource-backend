package com.marketplace.backend.controller;

import com.marketplace.backend.dto.DonationRequest;
import com.marketplace.backend.dto.DonationResponseDto;
import com.marketplace.backend.service.DiscordBotService;
import com.marketplace.backend.service.DonationService;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.security.SecurityUserHelper;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/donations")
@RequiredArgsConstructor
public class DonationController {

  private final DonationService donationService;
  private final SecurityUserHelper securityUserHelper;
  private final DiscordBotService discordBotService;

  @PostMapping
  public ResponseEntity<DonationResponseDto> createDonation(
      @Valid @RequestBody DonationRequest request) {
    DonationResponseDto created = donationService.createDonation(request);
    
    discordBotService.sendNotification(String.format(
        "💰 **New Donation Received**\n" +
        "💵 **Amount**: %.2f TND\n" +
        "🏢 **For Association**: ID %d\n" +
        "👤 **Donor**: %s (User ID %d)\n" +
        "💬 **Message**: %s",
        created.getAmount(), created.getAssociationId(), 
        created.getUserName() != null ? created.getUserName() : "Anonymous",
        created.getUserId() != null ? created.getUserId() : 0,
        created.getMessage() != null ? created.getMessage() : "No message"
    ));
    
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @GetMapping("/association/{associationId}")
  public ResponseEntity<List<DonationResponseDto>> getDonationsByAssociation(
      @PathVariable Long associationId) {
    return ResponseEntity.ok(donationService.getDonationsByAssociation(associationId));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN', 'ENTERPRISE')")
  public ResponseEntity<?> delete(@PathVariable Long id, Authentication auth) {
    try {
      User currentUser = securityUserHelper.requireUser(auth);
      donationService.delete(id, currentUser);
      discordBotService.sendNotification(String.format(
          "❌ **Donation Deleted**\n" +
          "🆔 **Donation ID**: %d\n" +
          "👤 **Deleted By**: User ID %d (%s)",
          id, currentUser.getId(), currentUser.getFullName()
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
}
