package com.nqt.payment_service.repository;

import com.nqt.payment_service.entity.UserWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
@Repository
public interface UserWalletRepo extends JpaRepository<UserWallet, UUID> {
    boolean existsByUserCode(String userCode);
    Optional<UserWallet> findByUserCode(String userCode);
    Optional<UserWallet> findFirstByUserCodeStartingWith(String prefix);

}
