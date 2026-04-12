package com.marketplace.backend.service.finance;

import com.marketplace.backend.entity.finance.EscrowStatus;
import com.marketplace.backend.entity.finance.escrow;
import com.marketplace.backend.repository.finance.EscrowRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EscrowServiceImpl implements IEscrowService {
    private final EscrowRepository escrowRepository;

    @Override
    public List<escrow> retrieveAllEscrow() {
        return escrowRepository.findAll();
    }

    @Override
    public escrow addEscrow(escrow e) {

        if(e.getStatus() == EscrowStatus.RELEASED){
            e.setReleaseDate(LocalDate.now().toString());
        }

        return escrowRepository.save(e);
    }

    @Override
    public void removeEscrow(Long id) {
        escrowRepository.deleteById(id);
    }

    @Override
    public escrow modifyEscrow(escrow e) {

        if(e.getStatus() == EscrowStatus.RELEASED){
            e.setReleaseDate(LocalDate.now().toString());
        }

        return escrowRepository.save(e);
    }

    @Override
    public escrow releaseEscrow(Long id) {
        escrow e = escrowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Escrow introuvable : " + id));
        e.setStatus(EscrowStatus.RELEASED);
        e.setReleaseDate(LocalDate.now().toString());
        return escrowRepository.save(e);
    }
}
