package com.marketplace.backend.controller.finance;

import com.marketplace.backend.entity.finance.FinancingRequest;
import com.marketplace.backend.service.finance.IFinancingService;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/financing")
@RequiredArgsConstructor
public class FinancingController {
    private final IFinancingService service;

    @PostMapping("/add")
    public FinancingRequest add(@RequestBody FinancingRequest f){
        return service.add(f);
    }

    @GetMapping("/all")
    public List<FinancingRequest> getAll(){
        return service.getAll();
    }

    @GetMapping("/{id}")
    public FinancingRequest getById(@PathVariable Long id){
        return service.getById(id);
    }

    @PutMapping("/update")
    public FinancingRequest update(@RequestBody FinancingRequest f){
        return service.update(f);
    }

    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable Long id){
        service.delete(id);
    }

    // 🔥 calcul intérêt seul
    @GetMapping("/interest")
    public double calculate(
            @RequestParam double amount,
            @RequestParam double rate,
            @RequestParam int months
    ){
        return service.calculateInterest(amount, rate, months);
    }

}
