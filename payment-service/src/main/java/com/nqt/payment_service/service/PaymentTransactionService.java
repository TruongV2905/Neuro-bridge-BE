package com.nqt.payment_service.service;

import com.identity.grpc.UserResponse;
import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.dto.response.PageResponse;
import com.nqt.common_starter.exception.GlobalException;
import com.nqt.common_starter.util.PageUtil;
import com.nqt.payment_service.constant.PaymentReferenceType;
import com.nqt.payment_service.constant.TransactionStatus;
import com.nqt.payment_service.constant.TransactionType;
import com.nqt.payment_service.dto.request.PaymentTransactionFilterRequest;
import com.nqt.payment_service.dto.request.PaymentTransactionRequest;
import com.nqt.payment_service.dto.request.SubPaymentTransactionFilterRequest;
import com.nqt.payment_service.dto.response.PaymentTransactionResponse;
import com.nqt.payment_service.entity.PaymentTransaction;
import com.nqt.payment_service.httpclient.IdentityServiceClient;
import com.nqt.payment_service.mapper.PaymentTransactionMapper;
import com.nqt.payment_service.repository.PaymentTransactionRepo;
import com.nqt.payment_service.service.impl.IPaymentTransaction;
import com.nqt.payment_service.specification.PaymentTransactionSpecification;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PaymentTransactionService implements IPaymentTransaction {

    PaymentTransactionRepo paymentTransactionRepo;
    PaymentTransactionMapper paymentTransactionMapper;
    IdentityServiceClient identityServiceClient;
    UserWalletService userWalletService;
    ProjectEscrowService projectEscrowService;
    BigDecimal SYSTEM_FEE_RATE = new BigDecimal("0.10");

    // ---------------------------------------------------------------------
    // Base CRUD
    // ---------------------------------------------------------------------

    @Override
    @Transactional
    public PaymentTransactionResponse create(PaymentTransactionRequest request) {
        PaymentTransaction transaction = paymentTransactionMapper.toEntity(request);
        PaymentTransaction saved = paymentTransactionRepo.save(transaction);
        log.info("Created payment transaction id={} type={} amount={}",
                saved.getId(), saved.getType(), saved.getAmount());
        return paymentTransactionMapper.toResponse(saved);
    }

    @Override
    public PaymentTransactionResponse getById(UUID id) {
        PaymentTransaction transaction = paymentTransactionRepo.findById(id)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "PaymentTransaction not found with id: " + id));
        return paymentTransactionMapper.toResponse(transaction);
    }

    @Override
    public PageResponse<PaymentTransactionResponse> getAll(PaymentTransactionFilterRequest request) {
        Pageable pageable = PageUtil.createPageable(request);
        Specification<PaymentTransaction> spec = PaymentTransactionSpecification.filter(request);
        Page<PaymentTransactionResponse> page = paymentTransactionRepo.findAll(spec, pageable)
                .map(paymentTransactionMapper::toResponse);
        return PageResponse.fromPage(page);
    }

    @Override
    public PageResponse<PaymentTransactionResponse> getMyTransactions(
            PaymentTransactionFilterRequest request) {
        UserResponse user = identityServiceClient.getCurrentUser();
        String userCode = user.getCode();

        SubPaymentTransactionFilterRequest scopedFilter = SubPaymentTransactionFilterRequest.builder()
                .involvingUserCode(userCode)
                .build();

        PaymentTransactionFilterRequest scoped = PaymentTransactionFilterRequest.builder()
                .page(request.getPage())
                .size(request.getSize())
                .sortBy(request.getSortBy())
                .direction(request.getDirection())
                .search(request.getSearch())
                .filter(scopedFilter)
                .build();

        Pageable pageable = PageUtil.createPageable(scoped);
        Specification<PaymentTransaction> spec = PaymentTransactionSpecification.filter(scoped);
        Page<PaymentTransactionResponse> page = paymentTransactionRepo.findAll(spec, pageable)
                .map(paymentTransactionMapper::toResponse);
        return PageResponse.fromPage(page);
    }

    // ---------------------------------------------------------------------
    // Status transitions
    // ---------------------------------------------------------------------

    @Override
    @Transactional
    public PaymentTransactionResponse markSuccess(UUID id) {
        return updateStatus(id, TransactionStatus.SUCCESS);
    }

    @Override
    @Transactional
    public PaymentTransactionResponse markFailed(UUID id) {
        return updateStatus(id, TransactionStatus.FAILED);
    }

    @Override
    @Transactional
    public PaymentTransactionResponse cancel(UUID id) {
        return updateStatus(id, TransactionStatus.CANCELLED);
    }

    // ---------------------------------------------------------------------
    // CORE: payment — unified handler
    // ---------------------------------------------------------------------
    //
    // Quy tắc:
    //   fromId != null && deductibleAmount >= 0  →  withdraw từ ví nguồn
    //   toId   != null && receivedAmount  >= 0  →  deposit vào ví đích
    //   referenceType == PROJECT  →  dùng EscrowService
    //   referenceType khác         →  dùng WalletService
    //   transactionId == null      →  tạo mới transaction
    //   transactionId != null      →  update transaction hiện có
    //   Khi xử lý thành công       →  set status = SUCCESS
    // ---------------------------------------------------------------------

    @Override
    @Transactional
    public PaymentTransactionResponse payment(
            String fromUserCode,
            String toUserCode,
            TransactionType transactionType,
            String referenceId,
            PaymentReferenceType referenceType,
            BigDecimal deductibleAmount,
            BigDecimal receivedAmount,
            UUID transactionId
    ) {
        // --- 1. Thực hiện chuyển tiền trên ví/escrow ---------------------------
        if (fromUserCode != null && deductibleAmount != null
                && deductibleAmount.compareTo(BigDecimal.ZERO) >= 0) {
                userWalletService.withdraw(fromUserCode, deductibleAmount);
        }

        if (receivedAmount != null && receivedAmount.compareTo(BigDecimal.ZERO) > 0) {

            if (referenceType == PaymentReferenceType.PROJECT) {
                if (referenceId == null) {
                    throw new GlobalException(
                            ErrorCode.DATA_INVALID.getCode(),
                            "Project referenceId is required for escrow deposit"
                    );
                }

                projectEscrowService.deposit(referenceId, receivedAmount);

            } else {
                if (toUserCode == null) {
                    throw new GlobalException(
                            ErrorCode.DATA_INVALID.getCode(),
                            "Wallet referenceId is required for wallet deposit"
                    );
                }

                userWalletService.deposit(toUserCode, receivedAmount);
            }
        }

        // --- 2. Tạo hoặc cập nhật transaction ----------------------------------
        PaymentTransactionResponse response;
        if (transactionId == null) {
            // Tạo mới
            PaymentTransactionRequest request = PaymentTransactionRequest.builder()
                    .fromUserCode(fromUserCode)
                    .toUserCode(toUserCode)
                    .type(transactionType)
                    .referenceId(referenceId)
                    .referenceType(referenceType)
                    .amount(receivedAmount != null ? receivedAmount : deductibleAmount)
                    .status(TransactionStatus.SUCCESS)
                    .description(buildDescription(transactionType, fromUserCode, toUserCode))
                    .build();
            response = create(request);
        } else {
            // Update bản ghi hiện có
            PaymentTransaction existing = paymentTransactionRepo.findById(transactionId)
                    .orElseThrow(() -> new GlobalException(
                            ErrorCode.NOT_FOUND.getCode(),
                            "PaymentTransaction not found with id: " + transactionId));
            existing.setStatus(TransactionStatus.SUCCESS);
            PaymentTransaction saved = paymentTransactionRepo.save(existing);
            response = paymentTransactionMapper.toResponse(saved);
        }

        log.info(
                "Payment success from={} to={} type={} ref={}:{} txId={} deductible={} received={}",
                fromUserCode, toUserCode, transactionType,
                referenceType, referenceId, transactionId,
                deductibleAmount, receivedAmount
        );

        return response;
    }

    // ---------------------------------------------------------------------
    // CORE: tính phí hệ thống — KHÔNG gọi payment() bên trong
    // ---------------------------------------------------------------------
    //
    // Trả về mảng [feeAmount, balanceAmount]:
    //   feeAmount     = amount * 10%
    //   balanceAmount = amount - feeAmount
    //
    // Caller tự quyết định gọi payment() cho phí admin riêng.
    // ---------------------------------------------------------------------

    BigDecimal[] calculateFee(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new GlobalException(
                    ErrorCode.DATA_INVALID.getCode(),
                    "Amount must be greater than 0"
            );
        }

        BigDecimal feeAmount = amount
                .multiply(SYSTEM_FEE_RATE)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal balanceAmount = amount.subtract(feeAmount);

        log.info("Fee calculated: gross={} fee={} net={}", amount, feeAmount, balanceAmount);

        return new BigDecimal[]{feeAmount, balanceAmount};
    }

    // ---------------------------------------------------------------------
    // 6.2 Mua Proposal bằng ví
    // ---------------------------------------------------------------------

    @Override
    @Transactional
    public PaymentTransactionResponse transactionProposalUnlockByWallet(
            BigDecimal amount,
            String description,
            String expertCode,
            String proposalId
    ) {
        UserResponse client = identityServiceClient.getCurrentUser();
        String clientCode = client.getCode();

        BigDecimal[] split = calculateFee(amount);
        BigDecimal systemFee = split[0];
        BigDecimal expertReceived = split[1];

        // Thanh toán chính: client → expert
        PaymentTransactionResponse mainTx = payment(
                clientCode,
                expertCode,
                TransactionType.PROPOSAL_PAYMENT,
                proposalId,
                PaymentReferenceType.PROPOSAL,
                amount,
                expertReceived,
                null
        );

        // Thu phí hệ thống: client → admin
        String adminCode = userWalletService.getWalletAdmin().getUserCode();
        payment(
                clientCode,
                adminCode,
                TransactionType.SYSTEM_FEE,
                null,
                PaymentReferenceType.ADMIN_ACTION,
                BigDecimal.ZERO,
                systemFee,
                null
        );

        log.info("Proposal payment done: client={} expert={} proposalId={} amount={} fee={}",
                clientCode, expertCode, proposalId, amount, systemFee);

        return mainTx;
    }

    // ---------------------------------------------------------------------
    // 6.1 Mua AI Service bằng ví
    // ---------------------------------------------------------------------

    @Override
    @Transactional
    public PaymentTransactionResponse transactionAvailableServiceByWallet(
            BigDecimal amount,
            String description,
            String expertCode,
            String aiServiceId
    ) {
        UserResponse client = identityServiceClient.getCurrentUser();
        String clientCode = client.getCode();

        BigDecimal[] split = calculateFee(amount);
        BigDecimal systemFee = split[0];
        BigDecimal expertReceived = split[1];

        // Thanh toán chính: client → expert
        PaymentTransactionResponse mainTx = payment(
                clientCode,
                expertCode,
                TransactionType.AI_SERVICE_PAYMENT,
                aiServiceId,
                PaymentReferenceType.AI_SERVICE,
                amount,
                expertReceived,
                null
        );

        // Thu phí hệ thống: client → admin
        String adminCode = userWalletService.getWalletAdmin().getUserCode();
        payment(
                clientCode,
                adminCode,
                TransactionType.SYSTEM_FEE,
                null,
                PaymentReferenceType.ADMIN_ACTION,
                BigDecimal.ZERO,
                systemFee,
                null
        );

        log.info("AI Service payment done: client={} expert={} aiServiceId={} amount={} fee={}",
                clientCode, expertCode, aiServiceId, amount, systemFee);

        return mainTx;
    }

    // ---------------------------------------------------------------------
    // 7.1 Nạp tiền vào Escrow (bằng ví)
    // ---------------------------------------------------------------------

    @Override
    @Transactional
    public PaymentTransactionResponse transactionProjectByWallet(
            BigDecimal amount,
            String description,
            String escrowId,
            String projectId
    ) {
        UserResponse client = identityServiceClient.getCurrentUser();
        String clientCode = client.getCode();

        // Nạp tiền từ ví client vào escrow — không tính phí
        return payment(
                clientCode,
                escrowId,
                TransactionType.PROJECT_ESCROW_DEPOSIT,
                projectId,
                PaymentReferenceType.PROJECT,
                amount,
                amount,
                null
        );
    }

    // ---------------------------------------------------------------------
    // 8.1 Mua AI Service bằng VNPay (SEPAY)
    // ---------------------------------------------------------------------

    @Override
    public PaymentTransactionResponse transactionAvailableServiceByVNPay(
            BigDecimal amount,
            String description,
            String expertCode,
            String aiServiceId
    ) {
        throw new UnsupportedOperationException(
                "transactionAvailableServiceByVNPay not implemented yet — " +
                "wire up SEPAY callback: create PENDING, deposit on success"
        );
    }

    // ---------------------------------------------------------------------
    // 8.2 Mua Proposal bằng VNPay (SEPAY)
    // ---------------------------------------------------------------------

    @Override
    public PaymentTransactionResponse transactionProposalUnlockByVNPay(
            BigDecimal amount,
            String description,
            String expertCode,
            String proposalId
    ) {
        throw new UnsupportedOperationException(
                "transactionProposalUnlockByVNPay not implemented yet — " +
                "wire up SEPAY callback: create PENDING, deposit on success"
        );
    }

    // ---------------------------------------------------------------------
    // 8.4 Nạp tiền vào Escrow bằng VNPay (SEPAY)
    // ---------------------------------------------------------------------

    @Override
    public PaymentTransactionResponse transactionProjectByVNPay(
            BigDecimal amount,
            String description,
            String escrowId,
            String projectId
    ) {
        throw new UnsupportedOperationException(
                "transactionProjectByVNPay not implemented yet — " +
                "wire up SEPAY callback: create PENDING, escrow deposit on success"
        );
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private PaymentTransactionResponse updateStatus(UUID id, TransactionStatus newStatus) {
        PaymentTransaction transaction = paymentTransactionRepo.findById(id)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "PaymentTransaction not found with id: " + id));

        if (transaction.getStatus() == TransactionStatus.SUCCESS
                || transaction.getStatus() == TransactionStatus.CANCELLED) {
            throw new GlobalException(
                    ErrorCode.DATA_INVALID.getCode(),
                    "Cannot transition transaction from terminal status: " + transaction.getStatus());
        }

        transaction.setStatus(newStatus);
        PaymentTransaction saved = paymentTransactionRepo.save(transaction);
        log.info("Updated payment transaction id={} -> status={}", saved.getId(), newStatus);
        return paymentTransactionMapper.toResponse(saved);
    }

    private String buildDescription(TransactionType type, String from, String to) {
        return switch (type) {
            case WALLET_DEPOSIT -> "Nap tien vao vi";
            case WALLET_WITHDRAW -> "Rut tien khoi vi";
            case PROPOSAL_PAYMENT -> "Thanh toan mua proposal";
            case AI_SERVICE_PAYMENT -> "Thanh toan mua AI service";
            case PROJECT_ESCROW_DEPOSIT -> "Nap tien vao escrow du an";
            case PROJECT_RELEASE -> "Release tien tu escrow cho expert";
            case PROJECT_REFUND -> "Hoan tien tu escrow ve client";
            case SYSTEM_FEE -> "Phi he thong";
            case REFUND -> "Hoan tien giao dich";
            case ADMIN_ADD -> "Admin cong tien";
            case ADMIN_DEDUCT -> "Admin tru tien";
        };
    }
}
