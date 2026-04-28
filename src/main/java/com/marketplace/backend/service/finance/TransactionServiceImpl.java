package com.marketplace.backend.service.finance;

import com.marketplace.backend.entity.finance.transaction;
import com.marketplace.backend.repository.finance.TransactionRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements ITransactionService {

    private final TransactionRepository transactionRepository;

    @Override
    public List<transaction> retrieveAllTransactions() {
        return transactionRepository.findAll();
    }

    @Override
    public transaction retrieveTransaction(Long id) {
        return transactionRepository.findById(id).orElse(null);
    }

    @Override
    public transaction addTransaction(transaction t) {

        double commission = t.getAmount() * 0.02;
        t.setAmount(t.getAmount() - commission);

        return transactionRepository.save(t);
    }

    @Override
    public void removeTransaction(Long id) {
        transactionRepository.deleteById(id);
    }

    @Override
    public transaction modifyTransaction(transaction t) {
        return transactionRepository.save(t);
    }
}
