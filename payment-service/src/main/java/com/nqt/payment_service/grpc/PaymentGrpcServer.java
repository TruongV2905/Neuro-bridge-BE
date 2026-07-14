package com.nqt.payment_service.grpc;

import com.nqt.payment_service.service.PaymentTransactionService;
import com.payment.grpc.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.math.BigDecimal;
import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class PaymentGrpcServer extends PaymentServiceGrpc.PaymentServiceImplBase {

    private final PaymentTransactionService paymentTransactionService;

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
}
