package com.marketplace.backend.controller;

import com.marketplace.backend.entity.SolidarityAssociation;
import com.marketplace.backend.service.SolidarityAssociationService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/solidarity-associations")
@RequiredArgsConstructor
public class SolidarityAssociationController {

  private final SolidarityAssociationService solidarityAssociationService;

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<List<SolidarityAssociation>> list() {
    return ResponseEntity.ok(solidarityAssociationService.findAll());
  }
}
