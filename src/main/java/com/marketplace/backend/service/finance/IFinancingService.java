package com.marketplace.backend.service.finance;



import com.marketplace.backend.entity.finance.FinancingRequest;

import java.util.List;

public interface IFinancingService {
    List<FinancingRequest> getAll();

    FinancingRequest getById(Long id);

    FinancingRequest add(FinancingRequest f);

    FinancingRequest update(FinancingRequest f);

    void delete(Long id);

    double calculateInterest(double principal, double rate, int months);
}
