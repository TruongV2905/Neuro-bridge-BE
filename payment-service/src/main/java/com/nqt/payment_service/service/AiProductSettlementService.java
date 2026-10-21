package com.nqt.payment_service.service;

import com.nqt.payment_service.constant.AiProductSettlementState;
import com.nqt.payment_service.constant.PaymentReferenceType;
import com.nqt.payment_service.constant.TransactionStatus;
import com.nqt.payment_service.constant.TransactionType;
import com.nqt.payment_service.constant.WalletStatus;
import com.nqt.payment_service.entity.AiProductSettlement;
import com.nqt.payment_service.entity.PaymentTransaction;
import com.nqt.payment_service.entity.UserWallet;
import com.nqt.payment_service.repository.AiProductSettlementRepo;
import com.nqt.payment_service.repository.PaymentTransactionRepo;
import com.nqt.payment_service.repository.UserWalletRepo;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Immediate, Payment-owned AI Product settlement. This service deliberately
 * does not call the generic payment(...) helper because that helper loads
 * wallets without locks and has no durable correlation outcome.
 */
@Service
@RequiredArgsConstructor
public class AiProductSettlementService {

    private static final String PLATFORM_WALLET_PREFIX = "ADM";
    private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.10");

    private final AiProductSettlementRepo settlementRepo;
    private final UserWalletRepo userWalletRepo;
    private final PaymentTransactionRepo paymentTransactionRepo;
    private final EntityManager entityManager;

    /**
     * Serializes all same-Purchase requests before the unique-row lookup and
     * locks every financial wallet in canonical UUID order before mutation.
     */
    @Transactional
    public SettlementDecision settle(AiProductSettlementCommand command) {
        acquireCorrelationLock(command.productPurchaseId());

        var existing = settlementRepo.findByProductPurchaseIdForUpdate(command.productPurchaseId());
        if (existing.isPresent()) {
            var settlement = existing.get();
            if (settlement.getState() == AiProductSettlementState.SUCCESS
                    || settlement.getState() == AiProductSettlementState.IN_PROGRESS) {
                return new SettlementDecision(settlement, !hasSameMaterialInput(settlement, command));
            }

            // Every persisted non-success state in this model is explicitly
            // confirmed non-money-moving, so current Product input may retry.
            applyMaterialInput(settlement, command);
            settlement.setState(AiProductSettlementState.IN_PROGRESS);
            settlement.setOutcomeCode(null);
            settlement.setShortfallAmountVnd(null);
            settlement.setSettledAtUtc(null);
            settlement.setMainPaymentTransactionId(null);
            settlement.setFeePaymentTransactionId(null);
            return settleWithLockedWallets(settlement);
        }

        var settlement = new AiProductSettlement();
        settlement.setProductPurchaseId(command.productPurchaseId());
        applyMaterialInput(settlement, command);
        settlement.setState(AiProductSettlementState.IN_PROGRESS);
        settlementRepo.save(settlement);
        return settleWithLockedWallets(settlement);
    }

    @Transactional(readOnly = true)
    public AiProductSettlement findByProductPurchaseId(UUID productPurchaseId) {
        return settlementRepo.findByProductPurchaseId(productPurchaseId).orElse(null);
    }

    private SettlementDecision settleWithLockedWallets(AiProductSettlement settlement) {
        List<UserWallet> candidates = userWalletRepo.findSettlementWalletCandidatesForUpdate(
                List.of(settlement.getClientCode(), settlement.getExpertCode()),
                PLATFORM_WALLET_PREFIX);

        var clientWallet = uniqueWalletForCode(candidates, settlement.getClientCode());
        var expertWallet = uniqueWalletForCode(candidates, settlement.getExpertCode());
        var platformWallet = uniquePlatformWallet(candidates);

        if (clientWallet == null || expertWallet == null || platformWallet == null
                || clientWallet.getStatus() != WalletStatus.ACTIVE
                || expertWallet.getStatus() != WalletStatus.ACTIVE
                || platformWallet.getStatus() != WalletStatus.ACTIVE) {
            settlement.setState(AiProductSettlementState.WALLET_NOT_ACTIVE);
            settlement.setOutcomeCode("REQUIRED_WALLET_UNAVAILABLE");
            settlementRepo.save(settlement);
            return new SettlementDecision(settlement, false);
        }

        if (clientWallet.getWalletBalance().compareTo(settlement.getGrossAmountVnd()) < 0) {
            settlement.setState(AiProductSettlementState.INSUFFICIENT_BALANCE);
            settlement.setShortfallAmountVnd(
                    settlement.getGrossAmountVnd().subtract(clientWallet.getWalletBalance()));
            settlement.setOutcomeCode("INSUFFICIENT_BALANCE");
            settlementRepo.save(settlement);
            return new SettlementDecision(settlement, false);
        }

        // All three rows are locked and fully validated before the first balance change.
        clientWallet.setWalletBalance(clientWallet.getWalletBalance().subtract(settlement.getGrossAmountVnd()));
        clientWallet.setTotalWithdrawn(clientWallet.getTotalWithdrawn().add(settlement.getGrossAmountVnd()));
        expertWallet.setWalletBalance(expertWallet.getWalletBalance().add(settlement.getExpertNetVnd()));
        platformWallet.setWalletBalance(platformWallet.getWalletBalance().add(settlement.getPlatformFeeVnd()));

        var correlationId = settlement.getProductPurchaseId().toString();
        var mainTransaction = paymentTransactionRepo.save(PaymentTransaction.builder()
                .fromUserCode(settlement.getClientCode())
                .toUserCode(settlement.getExpertCode())
                .amount(settlement.getExpertNetVnd())
                .type(TransactionType.AI_PRODUCT_PAYMENT)
                .status(TransactionStatus.SUCCESS)
                .referenceId(correlationId)
                .referenceType(PaymentReferenceType.AI_PRODUCT)
                .description("AI Place Product settlement")
                .build());
        var feeTransaction = paymentTransactionRepo.save(PaymentTransaction.builder()
                .fromUserCode(settlement.getClientCode())
                .toUserCode(platformWallet.getUserCode())
                .amount(settlement.getPlatformFeeVnd())
                .type(TransactionType.SYSTEM_FEE)
                .status(TransactionStatus.SUCCESS)
                .referenceId(correlationId)
                .referenceType(PaymentReferenceType.AI_PRODUCT)
                .description("AI Place Product platform fee")
                .build());

        settlement.setMainPaymentTransactionId(mainTransaction.getId());
        settlement.setFeePaymentTransactionId(feeTransaction.getId());
        settlement.setShortfallAmountVnd(null);
        settlement.setOutcomeCode(null);
        settlement.setSettledAtUtc(Instant.now());
        settlement.setState(AiProductSettlementState.SUCCESS);
        settlementRepo.save(settlement);
        return new SettlementDecision(settlement, false);
    }

    private void acquireCorrelationLock(UUID productPurchaseId) {
        // PostgreSQL transaction-scoped advisory lock prevents two missing-row
        // requests from racing before the unique constraint can be observed.
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(hashtext(:correlationId))")
                .setParameter("correlationId", productPurchaseId.toString())
                .getSingleResult();
    }

    private static UserWallet uniqueWalletForCode(List<UserWallet> wallets, String userCode) {
        var matches = wallets.stream().filter(wallet -> userCode.equals(wallet.getUserCode())).toList();
        return matches.size() == 1 ? matches.getFirst() : null;
    }

    private static UserWallet uniquePlatformWallet(List<UserWallet> wallets) {
        // A matching inactive wallet is also ambiguous: Payment does not select
        // an arbitrary ADM* wallet merely because one candidate happens active.
        var matches = wallets.stream()
                .filter(wallet -> wallet.getUserCode() != null && wallet.getUserCode().startsWith(PLATFORM_WALLET_PREFIX))
                .toList();
        return matches.size() == 1 ? matches.getFirst() : null;
    }

    private static boolean hasSameMaterialInput(AiProductSettlement settlement, AiProductSettlementCommand command) {
        return settlement.getProductId().equals(command.productId())
                && settlement.getClientCode().equals(command.clientCode())
                && settlement.getExpertCode().equals(command.expertCode())
                && settlement.getGrossAmountVnd().compareTo(command.grossAmountVnd()) == 0
                && settlement.getSnapshotProductName().equals(command.snapshot().productName())
                && settlement.getSnapshotSellerExpertId().equals(command.snapshot().sellerExpertId())
                && settlement.getSnapshotSellerExpertCode().equals(command.snapshot().sellerExpertCode())
                && settlement.getSnapshotSellerDisplayName().equals(command.snapshot().sellerDisplayName());
    }

    private static void applyMaterialInput(AiProductSettlement settlement, AiProductSettlementCommand command) {
        var platformFee = command.grossAmountVnd()
                .multiply(PLATFORM_FEE_RATE)
                .setScale(0, RoundingMode.HALF_UP);
        settlement.setProductId(command.productId());
        settlement.setClientCode(command.clientCode());
        settlement.setExpertCode(command.expertCode());
        settlement.setGrossAmountVnd(command.grossAmountVnd());
        settlement.setPlatformFeeVnd(platformFee);
        settlement.setExpertNetVnd(command.grossAmountVnd().subtract(platformFee));
        settlement.setSnapshotProductName(command.snapshot().productName());
        settlement.setSnapshotSellerExpertId(command.snapshot().sellerExpertId());
        settlement.setSnapshotSellerExpertCode(command.snapshot().sellerExpertCode());
        settlement.setSnapshotSellerDisplayName(command.snapshot().sellerDisplayName());
    }

    public record SettlementDecision(AiProductSettlement settlement, boolean idempotencyConflict) { }

    public record AiProductSettlementCommand(
            UUID productPurchaseId,
            UUID productId,
            String clientCode,
            String expertCode,
            BigDecimal grossAmountVnd,
            SnapshotContext snapshot) { }

    public record SnapshotContext(
            String productName,
            UUID sellerExpertId,
            String sellerExpertCode,
            String sellerDisplayName) { }
}
