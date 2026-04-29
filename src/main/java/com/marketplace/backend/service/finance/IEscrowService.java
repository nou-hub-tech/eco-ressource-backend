package com.marketplace.backend.service.finance;


import com.marketplace.backend.entity.finance.escrow;

import java.util.List;

public interface IEscrowService {

    List<escrow> retrieveAllEscrow();

    escrow retrieveEscrow(Long id);

    escrow addEscrow(escrow e);

    void removeEscrow(Long id);

    escrow modifyEscrow(escrow e);

    escrow releaseEscrow(Long id);
}
