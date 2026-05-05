package com.marketplace.backend.controller.finance;

import com.marketplace.backend.service.finance.IEscrowService;
import com.marketplace.backend.service.finance.ITransactionService;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final ITransactionService transactionService;
    private final IEscrowService escrowService;
    @GetMapping("/treasury-summary")
    public Map<String, Object> getTreasurySummary() {

        var transactions = transactionService.retrieveAllTransactions();
        var escrows = escrowService.retrieveAllEscrow();

        double cash = transactions.stream()
                .mapToDouble(t -> t.getAmount())
                .sum();

        double receivables = transactions.stream()
                .filter(t -> t.getAmount() > 0 && "PENDING".equals(t.getStatus()))
                .mapToDouble(t -> t.getAmount())
                .sum();

        double payables = transactions.stream()
                .filter(t -> t.getAmount() < 0)
                .mapToDouble(t -> Math.abs(t.getAmount()))
                .sum();

        double escrowLocked = escrows.stream()
                .filter(e -> e.getStatus().name().equals("LOCKED"))
                .mapToDouble(e -> e.getAmount())
                .sum();

        double net = receivables - payables;

        Map<String, Object> summary = new HashMap<>();
        summary.put("cashOnHand", cash);
        summary.put("receivables", receivables);
        summary.put("payables", payables);
        summary.put("netBalance", net);
        summary.put("escrowBalance", escrowLocked);
        summary.put("daysCashOnHand", 30); // exemple

        return summary;
    }}