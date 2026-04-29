package com.marketplace.backend.entity.enums;

/**
 * Order workflow.
 * DRAFT → CONFIRMED → SHIPPED → DELIVERED, with CANCELLED as a terminal soft-delete state.
 */
public enum OrderStatus {
  draft,
  confirmed,
  shipped,
  delivered,
  cancelled
}
