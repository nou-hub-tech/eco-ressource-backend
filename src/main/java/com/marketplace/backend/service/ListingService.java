package com.marketplace.backend.service;

import com.marketplace.backend.dto.MarketplaceListingRequest;
import com.marketplace.backend.dto.ListingDto;
import com.marketplace.backend.dto.ListingModerationRequest;
import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.Listing;
import com.marketplace.backend.entity.User;
import com.marketplace.backend.entity.enums.ListingStatus;
import com.marketplace.backend.entity.enums.Role;
import com.marketplace.backend.repository.EnterpriseRepository;
import com.marketplace.backend.repository.ListingRepository;
import com.marketplace.backend.security.SecurityUserHelper;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListingService {

  private final ListingRepository listingRepository;
  private final EnterpriseRepository enterpriseRepository;
  private final SecurityUserHelper securityUserHelper;

  @Transactional(readOnly = true)
  public List<ListingDto> findAll() {
    return listingRepository.findAll().stream()
        .map(ListingDto::from)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public ListingDto getById(Long id) {
    Listing l = listingRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    return ListingDto.from(l);
  }

  @Transactional(readOnly = true)
  public List<ListingDto> findMine(Authentication auth) {
    Enterprise e = securityUserHelper.requireEnterprise(auth);
    return listingRepository.findByEnterpriseId(e.getId()).stream()
        .map(ListingDto::from)
        .collect(Collectors.toList());
  }

  private Enterprise resolveEnterpriseForWrite(Authentication auth, Long enterpriseId) {
    User u = securityUserHelper.requireUser(auth);
    if (enterpriseId != null) {
      Enterprise target =
          enterpriseRepository
              .findById(enterpriseId)
              .orElseThrow(() -> new IllegalArgumentException("Enterprise not found"));
      if (u.getRole() != Role.ROLE_ADMIN) {
        if (u.getEnterprise() == null || !u.getEnterprise().getId().equals(target.getId())) {
          throw new IllegalArgumentException("Forbidden");
        }
      }
      return target;
    }
    return securityUserHelper.requireEnterprise(auth);
  }

  @Transactional
  public ListingDto create(Authentication auth, MarketplaceListingRequest req) {
    Enterprise e = resolveEnterpriseForWrite(auth, req.getEnterpriseId());
    Listing l =
        Listing.builder()
            .enterprise(e)
            .title(req.getTitle())
            .category(req.getCategory())
            .price(req.getPrice())
            .quantityLabel(req.getQuantityLabel())
            .status(ListingStatus.valueOf(req.getStatus().toLowerCase(Locale.ROOT)))
            .aiInsight(req.getAiInsight())
            .build();
    listingRepository.save(l);
    e.setListingsCount((e.getListingsCount() != null ? e.getListingsCount() : 0) + 1);
    enterpriseRepository.save(e);
    return ListingDto.from(l);
  }

  @Transactional
  public ListingDto update(Long id, Authentication auth, MarketplaceListingRequest req) {
    User u = securityUserHelper.requireUser(auth);
    Listing l = listingRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    if (req.getEnterpriseId() != null) {
      if (u.getRole() != Role.ROLE_ADMIN) {
        throw new IllegalArgumentException("Forbidden");
      }
      Enterprise e =
          enterpriseRepository
              .findById(req.getEnterpriseId())
              .orElseThrow(() -> new IllegalArgumentException("Enterprise not found"));
      l.setEnterprise(e);
    } else if (u.getRole() != Role.ROLE_ADMIN) {
      if (u.getEnterprise() == null
          || !u.getEnterprise().getId().equals(l.getEnterprise().getId())) {
        throw new IllegalArgumentException("Forbidden");
      }
    }
    l.setTitle(req.getTitle());
    l.setCategory(req.getCategory());
    l.setPrice(req.getPrice());
    l.setQuantityLabel(req.getQuantityLabel());
    l.setStatus(ListingStatus.valueOf(req.getStatus().toLowerCase(Locale.ROOT)));
    l.setAiInsight(req.getAiInsight());
    listingRepository.save(l);
    return ListingDto.from(l);
  }

  @Transactional
  public void delete(Long id, Authentication auth) {
    User u = securityUserHelper.requireUser(auth);
    Listing l = listingRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    Enterprise owner = l.getEnterprise();
    if (u.getRole() != Role.ROLE_ADMIN) {
      if (u.getEnterprise() == null || !u.getEnterprise().getId().equals(owner.getId())) {
        throw new IllegalArgumentException("Forbidden");
      }
    }
    listingRepository.delete(l);
    owner.setListingsCount(Math.max(0, (owner.getListingsCount() != null ? owner.getListingsCount() : 1) - 1));
    enterpriseRepository.save(owner);
  }

  @Transactional
  public ListingDto moderate(Long id, ListingModerationRequest req) {
    Listing l =
        listingRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
    l.setStatus(ListingStatus.valueOf(req.getStatus().toLowerCase(Locale.ROOT)));
    listingRepository.save(l);
    return ListingDto.from(l);
  }
}
