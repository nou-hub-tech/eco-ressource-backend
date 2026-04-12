package com.marketplace.backend.repository.finance;

import com.marketplace.backend.entity.finance.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {}