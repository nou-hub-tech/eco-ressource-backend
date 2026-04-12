package com.marketplace.backend.controller.finance;

import lombok.RequiredArgsConstructor;
import com.marketplace.backend.entity.finance.transaction;
import com.marketplace.backend.service.finance.ITransactionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final ITransactionService service;


    @PostMapping("/add")
    public transaction add(@RequestBody transaction t){
        return service.addTransaction(t);
    }

    @GetMapping("/all")
    public List<transaction> getAll(){
        return service.retrieveAllTransactions();
    }


    @GetMapping("/{id}")
    public transaction getById(@PathVariable Long id){
        return service.retrieveTransaction(id);
    }

    @PutMapping("/update")
    public transaction update(@RequestBody transaction t){
        return service.modifyTransaction(t);
    }


    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable Long id){
        service.removeTransaction(id);
    }


    @GetMapping("/positive")
    public List<transaction> getPositiveTransactions(){
        return service.retrieveAllTransactions()
                .stream()
                .filter(t -> t.getAmount() > 0)
                .toList();
    }
}
