package com.marketplace.backend.event;

/**
 * Événement Spring publié après la libération d'un escrow (statut → RELEASED).
 * Transporté de manière asynchrone vers EscrowEmailListener pour envoi du mail.
 */
public record EscrowReleasedEvent(
        Long escrowId,
        String projectName,
        Double amount,
        String releasedAt,
        String recipientEmail,
        String recipientName
) {}
