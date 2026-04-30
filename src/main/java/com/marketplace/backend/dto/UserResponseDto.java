package com.marketplace.backend.dto;

import com.marketplace.backend.entity.Enterprise;
import com.marketplace.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {

  private String id;
  private String name;
  private String email;
  /** Angular route role: admin | enterprise | transporter */
  private String role;
  private String company;
  private Long enterpriseId;
  private EnterpriseRef enterprise;
  private String avatar;

  public static UserResponseDto from(User u) {
    String routeRole =
        switch (u.getRole()) {
          case ROLE_ADMIN -> "admin";
          case ROLE_ENTERPRISE -> "enterprise";
          case ROLE_TRANSPORTER -> "transporter";
        };
    String company = null;
    Long enterpriseId = null;
    EnterpriseRef enterprise = null;
    if (u.getEnterprise() != null) {
      Enterprise e = u.getEnterprise();
      company = e.getCompanyName();
      enterpriseId = e.getId();
      enterprise = new EnterpriseRef(e.getId(), e.getCompanyName());
    } else if (u.getTransporter() != null) {
      company = u.getTransporter().getCompanyName();
    }
    return UserResponseDto.builder()
        .id(String.valueOf(u.getId()))
        .name(u.getFullName())
        .email(u.getEmail())
        .role(routeRole)
        .company(company)
        .enterpriseId(enterpriseId)
        .enterprise(enterprise)
        .avatar(initials(u.getFullName()))
        .build();
  }

  private static String initials(String fullName) {
    if (fullName == null || fullName.isBlank()) {
      return "?";
    }
    String[] p = fullName.trim().split("\\s+");
    if (p.length == 1) {
      return p[0].substring(0, Math.min(2, p[0].length())).toUpperCase();
    }
    return ("" + p[0].charAt(0) + p[p.length - 1].charAt(0)).toUpperCase();
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class EnterpriseRef {
    private Long id;
    private String companyName;
  }
}
