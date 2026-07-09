package com.nqt.payment_service.grpc;

import com.nqt.payment_service.dto.response.UserWalletResponse;
import com.nqt.payment_service.service.impl.IUserWallet;
import com.payment.grpc.CreateWalletRequest;
import com.payment.grpc.WalletGrpcServiceGrpc;
import com.payment.grpc.WalletResponse;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.security.access.prepost.PreAuthorize;

@GrpcService
@RequiredArgsConstructor
public class WalletGrpcServiceImpl extends WalletGrpcServiceGrpc.WalletGrpcServiceImplBase {

    private final IUserWallet userWalletService;

    @Override
    @PreAuthorize("hasRole('SERVICE')")
    public void createWallet(CreateWalletRequest request, StreamObserver<WalletResponse> responseObserver) {
        UserWalletResponse wallet = userWalletService.create(request.getUserCode());

        WalletResponse response = WalletResponse.newBuilder()
                .setId(wallet.getId().toString())
                .setUserCode(wallet.getUserCode())
                .setStatus(wallet.getStatus() != null ? wallet.getStatus().name() : "")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
