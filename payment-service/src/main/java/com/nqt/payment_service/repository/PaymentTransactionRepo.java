package com.nqt.payment_service.repository;

import com.nqt.payment_service.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface PaymentTransactionRepo
        extends JpaRepository<PaymentTransaction, UUID>,
                JpaSpecificationExecutor<PaymentTransaction> {
}