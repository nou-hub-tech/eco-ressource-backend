package com.marketplace.backend.controller.finance;

import com.marketplace.backend.service.finance.FinancialAnalyticsService;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class FinancialAnalyticsController {

    private final FinancialAnalyticsService analyticsService;


    @GetMapping("/financial-health")
    public FinancialAnalyticsService.FinancialHealthReport getFinancialHealth() {
        return analyticsService.generateReport();
    }
}
