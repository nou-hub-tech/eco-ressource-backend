package com.marketplace.backend.entity.enums;

/**
 * Lifecycle of a published machine slot.
 * <ul>
 *   <li>{@code open}    — bookable</li>
 *   <li>{@code booked}  — claimed by a peer</li>
 *   <li>{@code blocked} — withheld (maintenance, etc.)</li>
 * </ul>
 */
public enum SlotStatus {
  open,
  booked,
  blocked
}
