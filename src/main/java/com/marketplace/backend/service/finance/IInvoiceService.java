package com.marketplace.backend.service.finance;


import com.marketplace.backend.entity.finance.Invoice;

import java.util.List;

public interface IInvoiceService {
    List<Invoice> retrieveAllInvoices();

    Invoice retrieveInvoice(Long id);

    Invoice addInvoice(Invoice invoice);

    Invoice updateInvoice(Invoice invoice);

    void deleteInvoice(Long id);
   // Confirmation livraison → PAID
    Invoice markAsPaid(Long id);
}