package com.nqt.payment_service.repository;

import com.nqt.payment_service.entity.UserWallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserWalletRepo extends JpaRepository<UserWallet, UUID> {
    boolean existsByUserCode(String userCode);

    Optional<UserWallet> findByUserCode(String userCode);

    Optional<UserWallet> findFirstByUserCodeStartingWith(String prefix);

    /**
     * Locks all possible financial participants in UUID order. The single query
     * provides a canonical lock order for Client, Expert, and ADM* candidates.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select wallet from UserWallet wallet
            where wallet.userCode in :userCodes
               or wallet.userCode like concat(:platformPrefix, '%')
            order by wallet.id asc
            """)
    List<UserWallet> findSettlementWalletCandidatesForUpdate(
            @Param("userCodes") Collection<String> userCodes,
            @Param("platformPrefix") String platformPrefix);

    Page<UserWallet> findAllByRequestWithdrawalTrue(Pageable pageable);
}
