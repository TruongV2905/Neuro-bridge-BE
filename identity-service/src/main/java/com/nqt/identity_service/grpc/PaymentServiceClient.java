package com.nqt.identity_service.grpc;

import com.nqt.common_starter.config.GrpcAuthContext;
import com.nqt.common_starter.security.JwtTokenVerifier;
import com.payment.grpc.CreateWalletRequest;
import com.payment.grpc.WalletGrpcServiceGrpc;
import com.payment.grpc.WalletResponse;
import io.grpc.Context;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceClient {

    @GrpcClient("payment-service")
    private WalletGrpcServiceGrpc.WalletGrpcServiceBlockingStub walletServiceBlockingStub;

    private final JwtTokenVerifier jwtTokenVerifier;

    public WalletResponse createWallet(String userCode) {
        String serviceToken = jwtTokenVerifier.issueServiceToken("identity-service");
        log.info("DEBUG - issued service token: {}", serviceToken);
        Context ctx = Context.current()
                .withValue(GrpcAuthContext.AUTHORIZATION_CONTEXT_KEY, "Bearer " + serviceToken);

        try {
            return ctx.call(() ->
                    walletServiceBlockingStub.createWallet(
                            CreateWalletRequest.newBuilder()
                                    .setUserCode(userCode)
                                    .build()
                    )
            );
        } catch (Exception e) {
            if (e instanceof RuntimeException re) {
                throw re;
            }
            throw new RuntimeException(e);
        }
    }
}
