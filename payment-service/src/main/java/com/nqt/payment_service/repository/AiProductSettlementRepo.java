package com.nqt.payment_service.repository;

import com.nqt.payment_service.entity.AiProductSettlement;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiProductSettlementRepo extends JpaRepository<AiProductSettlement, UUID> {

    Optional<AiProductSettlement> findByProductPurchaseId(UUID productPurchaseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select settlement from AiProductSettlement settlement where settlement.productPurchaseId = :productPurchaseId")
    Optional<AiProductSettlement> findByProductPurchaseIdForUpdate(
            @Param("productPurchaseId") UUID productPurchaseId);
}
