package com.marketplace.backend.entity.finance;

/**
 * 🧾 Type de facture du point de vue de l'entreprise connectée :
 *  - VENTE : l'entreprise est vendeur (sellerName = companyName) → elle va encaisser
 *  - ACHAT : l'entreprise est acheteur (clientName = companyName) → elle doit payer
 */
public enum InvoiceType {
    VENTE,
    ACHAT
}
