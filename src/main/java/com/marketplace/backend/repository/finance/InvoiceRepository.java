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
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.invoiceType = :type AND i.issueDate LIKE :year%")
    long countByTypeAndYear(@Param("type") InvoiceType type, @Param("year") String year);
}