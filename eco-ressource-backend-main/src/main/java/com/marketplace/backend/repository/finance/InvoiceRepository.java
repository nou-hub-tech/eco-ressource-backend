package com.marketplace.backend.repository.finance;

import com.marketplace.backend.entity.finance.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /**
     * Retourne toutes les factures où l'entreprise est VENDEUR (sellerName)
     * OU ACHETEUR (clientName) — comparaison insensible à la casse.
     */
    @Query("SELECT i FROM Invoice i WHERE " +
           "LOWER(i.sellerName) = LOWER(:companyName) OR " +
           "LOWER(i.clientName) = LOWER(:companyName)")
    List<Invoice> findByEnterpriseCompanyName(@Param("companyName") String companyName);
}