package com.marketplace.backend.service.finance;



import com.marketplace.backend.entity.finance.transaction;

import java.util.List;

public interface ITransactionService {

    List<transaction> retrieveAllTransactions();

    transaction retrieveTransaction(Long id);

    transaction addTransaction(transaction t);

    void removeTransaction(Long id);

    transaction modifyTransaction(transaction t);
}