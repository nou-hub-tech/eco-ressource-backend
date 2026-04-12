package com.marketplace.backend.controller.finance;

import com.marketplace.backend.entity.finance.EscrowStatus;
import com.marketplace.backend.entity.finance.escrow;
import com.marketplace.backend.service.finance.IEscrowService;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/escrow")
@RequiredArgsConstructor
public class EscrowController {
    private final IEscrowService service;

    @PostMapping("/add")
    public escrow add(@RequestBody escrow e){
        return service.addEscrow(e);
    }

    @GetMapping("/all")
    public List<escrow> getAll(){
        return service.retrieveAllEscrow();
    }


    @PutMapping("/update")
    public escrow update(@RequestBody escrow e){
        return service.modifyEscrow(e);
    }

    @DeleteMapping("/delete/{id}")
    public void delete(@PathVariable Long id){
        service.removeEscrow(id);
    }

    //  Escrow libéré
    @GetMapping("/released")
    public List<escrow> getReleased(){
        return service.retrieveAllEscrow()
                .stream()
                .filter(e -> e.getStatus() == EscrowStatus.RELEASED)
                .toList();
    }
    @PostMapping("/release/{id}")
    public escrow release(@PathVariable Long id) {
        return service.releaseEscrow(id);
    }
}
