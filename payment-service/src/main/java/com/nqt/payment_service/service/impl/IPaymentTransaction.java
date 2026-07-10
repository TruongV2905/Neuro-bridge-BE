package com.nqt.payment_service.service.impl;

import com.nqt.common_starter.dto.response.PageResponse;
import com.nqt.payment_service.constant.PaymentReferenceType;
import com.nqt.payment_service.constant.TransactionType;
import com.nqt.payment_service.dto.request.PaymentTransactionFilterRequest;
import com.nqt.payment_service.dto.request.PaymentTransactionRequest;
import com.nqt.payment_service.dto.response.PayEscrowResult;
import com.nqt.payment_service.dto.response.PaymentTransactionResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface IPaymentTransaction {

    // Base CRUD
    PaymentTransactionResponse create(PaymentTransactionRequest request);

    PaymentTransactionResponse getById(UUID id);

    PageResponse<PaymentTransactionResponse> getAll(PaymentTransactionFilterRequest request);

    PageResponse<PaymentTransactionResponse> getMyTransactions(PaymentTransactionFilterRequest request);

    // Status transitions
    PaymentTransactionResponse markSuccess(UUID id);

    PaymentTransactionResponse markFailed(UUID id);

    PaymentTransactionResponse cancel(UUID id);

    // ---------------------------------------------------------------------
    // CORE: unified payment handler
    // ---------------------------------------------------------------------
    // Quy tắc:
    //   fromUserCode != null && deductibleAmount >= 0  →  withdraw từ ví nguồn
    //   toUserCode   != null && receivedAmount  >= 0  →  deposit vào ví đích
    //   referenceType == PROJECT                        →  dùng EscrowService
    //   referenceType khác                              →  dùng WalletService
    //   transactionId == null                          →  tạo mới transaction
    //   transactionId != null                          →  update transaction hiện có
    //   Xử lý thành công                              →  set status = SUCCESS
    // ---------------------------------------------------------------------
    PaymentTransactionResponse payment(
            String fromUserCode,
            String toUserCode,
            TransactionType transactionType,
            String referenceId,
            PaymentReferenceType referenceType,
            BigDecimal deductibleAmount,
            BigDecimal receivedAmount,
            UUID transactionId
    );

    // ---------------------------------------------------------------------
    // 6.1 Mua AI Service bằng ví
    // ---------------------------------------------------------------------
    PaymentTransactionResponse transactionAvailableServiceByWallet(
            BigDecimal amount,
            String description,
            String expertCode,
            String aiServiceId
    );

    // ---------------------------------------------------------------------
    // 6.2 Mua Proposal bằng ví
    // ---------------------------------------------------------------------
    PaymentTransactionResponse transactionProposalUnlockByWallet(
            BigDecimal amount,
            String description,
            String expertCode,
            String proposalId
    );

    // ---------------------------------------------------------------------
    // 7.1 Nạp tiền vào Escrow bằng ví
    // ---------------------------------------------------------------------
    PaymentTransactionResponse transactionProjectByWallet(
            BigDecimal amount,
            String description,
            String escrowId,
            String projectId
    );

    // ---------------------------------------------------------------------
    // SEPAY / VNPay flows — tạo PENDING trước, xử lý trong callback
    // ---------------------------------------------------------------------

    // 8.1 Mua AI Service bằng VNPay
    PaymentTransactionResponse transactionAvailableServiceByVNPay(
            BigDecimal amount,
            String description,
            String expertCode,
            String aiServiceId
    );

    // 8.2 Mua Proposal bằng VNPay
    PaymentTransactionResponse transactionProposalUnlockByVNPay(
            BigDecimal amount,
            String description,
            String expertCode,
            String proposalId
    );

    // 8.4 Nạp tiền vào Escrow bằng VNPay
    PaymentTransactionResponse transactionProjectByVNPay(
            BigDecimal amount,
            String description,
            String escrowId,
            String projectId
    );

    PayEscrowResult payOrInitProjectEscrow(UUID jobPostId, String clientCode,
                                           String expertCode, BigDecimal amount);
}
