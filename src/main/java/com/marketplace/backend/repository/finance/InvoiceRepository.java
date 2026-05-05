package com.marketplace.backend.repository.finance;

import com.marketplace.backend.entity.finance.Invoice;
import com.marketplace.backend.entity.finance.InvoiceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /** Toutes les factures de l'entreprise (acheteur OU vendeur) */
    @Query("SELECT i FROM Invoice i WHERE " +
           "LOWER(i.sellerName) = LOWER(:companyName) OR " +
           "LOWER(i.clientName) = LOWER(:companyName)")
    List<Invoice> findByEnterpriseCompanyName(@Param("companyName") String companyName);

    /** 📤 Factures de VENTE : l'entreprise est vendeur */
    @Query("SELECT i FROM Invoice i WHERE LOWER(i.sellerName) = LOWER(:companyName)")
    List<Invoice> findSalesInvoices(@Param("companyName") String companyName);

    /** 📥 Factures d'ACHAT : l'entreprise est acheteur */
    @Query("SELECT i FROM Invoice i WHERE LOWER(i.clientName) = LOWER(:companyName)")
    List<Invoice> findPurchaseInvoices(@Param("companyName") String companyName);

    /** Par type */
    List<Invoice> findByInvoiceType(InvoiceType invoiceType);

    /**
     * 🔢 Nombre de factures d'un type pour une année donnée.
     * Utilisé pour générer le prochain numéro automatique.
     * ex: VTE-2026-001 / ACH-2026-001
     */
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.invoiceType = :type AND i.issueDate LIKE CONCAT(:year, '%')")
    long countByTypeAndYear(@Param("type") InvoiceType type, @Param("year") String year);

    /** 🔍 Vérifie si un numéro de facture existe déjà (pour garantir l'unicité) */
    boolean existsByInvoiceNumber(String invoiceNumber);

    /** 🚚 Factures UNPAID liées à une livraison (pour le polling automatique) */
    @Query("SELECT i FROM Invoice i WHERE i.status = 'UNPAID' AND i.deliveryOrderId IS NOT NULL")
    List<Invoice> findUnpaidWithDelivery();

    /** 🎯 Factures liées à une commande de livraison spécifique */
    List<Invoice> findByDeliveryOrderId(Long deliveryOrderId);

    /** 🔍 Fallback : factures d'un client avec un statut donné */
    @Query("SELECT i FROM Invoice i WHERE LOWER(i.clientName) = LOWER(:clientName) AND i.status = :status")
    List<Invoice> findByClientNameAndStatus(@Param("clientName") String clientName, @Param("status") String status);
}