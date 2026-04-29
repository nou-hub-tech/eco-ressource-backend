package com.marketplace.backend.service.finance;

import com.marketplace.backend.entity.finance.FinancingRequest;
import com.marketplace.backend.repository.finance.FinancingRequestRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FinancingServiceImpl implements IFinancingService {

    private final FinancingRequestRepository repo;

    @Override
    public List<FinancingRequest> getAll() {
        return repo.findAll();
    }

    @Override
    public FinancingRequest getById(Long id) {
        return repo.findById(id).orElse(null);
    }

    @Override
    public FinancingRequest add(FinancingRequest f) {

        // 💰 calcul automatique intérêt
        double interest = calculateInterest(
                f.getAmountRequested(),
                f.getInterestRate(),
                f.getDurationMonths()
        );

        f.setAmountApproved(f.getAmountRequested() + interest);

        return repo.save(f);
    }

    @Override
    public FinancingRequest update(FinancingRequest f) {
        return repo.save(f);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }

    // 🏦 formule intérêt
    @Override
    public double calculateInterest(double principal, double rate, int months) {
        return principal * (rate / 100) * (months / 12.0);
    }
}