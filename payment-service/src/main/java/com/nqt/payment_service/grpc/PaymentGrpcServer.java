package com.nqt.payment_service.grpc;

import com.nqt.payment_service.constant.AiProductSettlementState;
import com.nqt.payment_service.entity.AiProductSettlement;
import com.nqt.payment_service.service.AiProductSettlementService;
import com.nqt.payment_service.service.PaymentTransactionService;
import com.payment.grpc.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.security.access.prepost.PreAuthorize;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class PaymentGrpcServer extends PaymentServiceGrpc.PaymentServiceImplBase {

    private final PaymentTransactionService paymentTransactionService;
    private final AiProductSettlementService aiProductSettlementService;

    @Override
    public void payProjectEscrow(PayProjectEscrowRequest request,
                                 StreamObserver<PayProjectEscrowResponse> responseObserver) {
        try {
            var result = paymentTransactionService.payOrInitProjectEscrow(
                    UUID.fromString(request.getJobPostId()),
                    request.getClientCode(),
                    request.getExpertCode(),
                    new BigDecimal(request.getAmount())
            );

            var builder = PayProjectEscrowResponse.newBuilder()
                    .setSuccess(true)
                    .setStatus(result.getStatus().name())
                    .setEscrowId(result.getEscrowId().toString())
                    .setMessage(result.getMessage());

            if (result.getShortfallAmount() != null) {
                builder.setShortfallAmount(result.getShortfallAmount().toPlainString());
            }

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("❌ Lỗi payProjectEscrow", e);
            responseObserver.onNext(PayProjectEscrowResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Lỗi thanh toán vào ví chung: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    @Override
    public void releaseEscrow(ReleaseEscrowRequest request,
                              StreamObserver<ReleaseEscrowResponse> responseObserver) {
        try {
            var result = paymentTransactionService.releaseProjectEscrow(request.getEscrowId());

            responseObserver.onNext(ReleaseEscrowResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Giải ngân cho Expert thành công")
                    .setTransactionId(result.getId().toString())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("❌ Lỗi releaseEscrow", e);
            responseObserver.onNext(ReleaseEscrowResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Lỗi giải ngân: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }

    /**
     * This endpoint accepts only a service JWT. It must never accept an end
     * user's browser identity as authorization to debit arbitrary wallets.
     */
    @Override
    @PreAuthorize("hasRole('SERVICE')")
    public void settleAiProductPurchase(AiProductSettlementRequest request,
                                        StreamObserver<AiProductSettlementResponse> responseObserver) {
        try {
            var decision = aiProductSettlementService.settle(toCommand(request));
            responseObserver.onNext(toResponse(decision.settlement(), decision.idempotencyConflict()));
            responseObserver.onCompleted();
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid AI Product settlement request.")
                    .asRuntimeException());
        } catch (Exception exception) {
            log.error("AI Product settlement failed safely", exception);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("AI Product settlement is temporarily unavailable.")
                    .asRuntimeException());
        }
    }

    @Override
    @PreAuthorize("hasRole('SERVICE')")
    public void getAiProductPurchaseSettlement(AiProductSettlementLookupRequest request,
                                                StreamObserver<AiProductSettlementResponse> responseObserver) {
        try {
            var productPurchaseId = parseUuid(request.getProductPurchaseId(), "product_purchase_id");
            var settlement = aiProductSettlementService.findByProductPurchaseId(productPurchaseId);
            if (settlement == null) {
                responseObserver.onNext(AiProductSettlementResponse.newBuilder()
                        .setStatus(AiProductSettlementStatus.AI_PRODUCT_SETTLEMENT_NOT_FOUND)
                        .setProductPurchaseId(productPurchaseId.toString())
                        .setMessage("Settlement was not found.")
                        .build());
            } else {
                responseObserver.onNext(toResponse(settlement, false));
            }
            responseObserver.onCompleted();
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid AI Product settlement lookup request.")
                    .asRuntimeException());
        } catch (Exception exception) {
            log.error("AI Product settlement lookup failed safely", exception);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("AI Product settlement lookup is temporarily unavailable.")
                    .asRuntimeException());
        }
    }

    private static AiProductSettlementService.AiProductSettlementCommand toCommand(
            AiProductSettlementRequest request) {
        var productPurchaseId = parseUuid(request.getProductPurchaseId(), "product_purchase_id");
        var productId = parseUuid(request.getProductId(), "product_id");
        var clientCode = requireText(request.getClientCode(), "client_code");
        var expertCode = requireText(request.getExpertCode(), "expert_code");
        if (clientCode.equals(expertCode)) {
            throw new IllegalArgumentException("client_code and expert_code must differ");
        }

        var gross = parseWholePositiveAmount(request.getGrossAmountVnd());
        var context = request.getSnapshotContext();
        var snapshot = new AiProductSettlementService.SnapshotContext(
                requireText(context.getProductName(), "snapshot_context.product_name"),
                parseUuid(context.getSellerExpertId(), "snapshot_context.seller_expert_id"),
                requireText(context.getSellerExpertCode(), "snapshot_context.seller_expert_code"),
                requireText(context.getSellerDisplayName(), "snapshot_context.seller_display_name"));

        return new AiProductSettlementService.AiProductSettlementCommand(
                productPurchaseId, productId, clientCode, expertCode, gross, snapshot);
    }

    private static AiProductSettlementResponse toResponse(
            AiProductSettlement settlement,
            boolean idempotencyConflict) {
        var builder = AiProductSettlementResponse.newBuilder()
                .setStatus(idempotencyConflict
                        ? AiProductSettlementStatus.AI_PRODUCT_SETTLEMENT_IDEMPOTENCY_CONFLICT
                        : toProtoStatus(settlement.getState()))
                .setProductPurchaseId(settlement.getProductPurchaseId().toString())
                .setProductId(settlement.getProductId().toString())
                .setGrossAmountVnd(toDecimalString(settlement.getGrossAmountVnd()))
                .setPlatformFeeAmountVnd(toDecimalString(settlement.getPlatformFeeVnd()))
                .setExpertNetAmountVnd(toDecimalString(settlement.getExpertNetVnd()))
                .setSnapshotContext(AiProductSnapshotContext.newBuilder()
                        .setProductName(settlement.getSnapshotProductName())
                        .setSellerExpertId(settlement.getSnapshotSellerExpertId().toString())
                        .setSellerExpertCode(settlement.getSnapshotSellerExpertCode())
                        .setSellerDisplayName(settlement.getSnapshotSellerDisplayName())
                        .build())
                .setMessage(messageFor(settlement.getState(), idempotencyConflict));

        if (settlement.getMainPaymentTransactionId() != null) {
            builder.setPaymentTransactionId(settlement.getMainPaymentTransactionId().toString());
        }
        if (settlement.getShortfallAmountVnd() != null) {
            builder.setShortfallAmountVnd(toDecimalString(settlement.getShortfallAmountVnd()));
        }
        if (settlement.getSettledAtUtc() != null) {
            builder.setSettledAtUtc(settlement.getSettledAtUtc().toString());
        }
        return builder.build();
    }

    private static AiProductSettlementStatus toProtoStatus(AiProductSettlementState state) {
        return switch (state) {
            case SUCCESS -> AiProductSettlementStatus.AI_PRODUCT_SETTLEMENT_SUCCESS;
            case IN_PROGRESS -> AiProductSettlementStatus.AI_PRODUCT_SETTLEMENT_IN_PROGRESS;
            case INSUFFICIENT_BALANCE -> AiProductSettlementStatus.AI_PRODUCT_SETTLEMENT_INSUFFICIENT_BALANCE;
            case WALLET_NOT_ACTIVE -> AiProductSettlementStatus.AI_PRODUCT_SETTLEMENT_WALLET_NOT_ACTIVE;
            case PAYMENT_FAILED -> AiProductSettlementStatus.AI_PRODUCT_SETTLEMENT_PAYMENT_FAILED;
        };
    }

    private static String messageFor(AiProductSettlementState state, boolean idempotencyConflict) {
        if (idempotencyConflict) {
            return "Settlement input conflicts with an authoritative settlement.";
        }
        return switch (state) {
            case SUCCESS -> "Settlement succeeded.";
            case IN_PROGRESS -> "Settlement is in progress.";
            case INSUFFICIENT_BALANCE -> "Wallet balance is insufficient.";
            case WALLET_NOT_ACTIVE -> "A required wallet is unavailable.";
            case PAYMENT_FAILED -> "Payment did not move money and may be retried.";
        };
    }

    private static UUID parseUuid(String value, String field) {
        try {
            return UUID.fromString(requireText(value, field));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(field + " must be a UUID");
        }
    }

    private static BigDecimal parseWholePositiveAmount(String value) {
        try {
            var amount = new BigDecimal(requireText(value, "gross_amount_vnd"));
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("gross_amount_vnd must be positive");
            }
            return amount.setScale(0, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException | NumberFormatException exception) {
            throw new IllegalArgumentException("gross_amount_vnd must be a positive whole-VND decimal");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static String toDecimalString(BigDecimal amount) {
        return amount == null ? "" : amount.toPlainString();
    }
}
