package com.marketplace.backend.controller;

import com.marketplace.backend.dto.DonationRequest;
import com.marketplace.backend.dto.DonationResponseDto;
import com.marketplace.backend.service.DonationService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

  @PostMapping
  public ResponseEntity<DonationResponseDto> createDonation(
      @Valid @RequestBody DonationRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(donationService.createDonation(request));
  }

  @GetMapping("/association/{associationId}")
  public ResponseEntity<List<DonationResponseDto>> getDonationsByAssociation(
      @PathVariable Long associationId) {
    return ResponseEntity.ok(donationService.getDonationsByAssociation(associationId));
  }
}
