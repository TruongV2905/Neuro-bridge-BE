package com.nqt.payment_service.controller;

import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.common_starter.dto.response.PageResponse;
import com.nqt.payment_service.dto.request.UpdateBankInfoRequest;
import com.nqt.payment_service.dto.response.UserWalletResponse;
import com.nqt.payment_service.dto.response.WithdrawRequestResponse;
import com.nqt.payment_service.service.impl.IUserWallet;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import com.nqt.payment_service.dto.request.UserWalletRequest;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/api/wallet", "/wallet"})
@RequiredArgsConstructor
public class UserWalletController {

    private final IUserWallet userWalletService;

    @PutMapping("/me/bank-info")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cập nhật thông tin tài khoản ngân hàng của ví hiện tại")
    public ResponseEntity<APIResponse<UserWalletResponse>> updateBankInfo(
            @Valid @RequestBody UpdateBankInfoRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UserWalletResponse response = userWalletService.updateCurrentUserBankInfo(request);
        return ResponseEntity.ok(APIResponse.response(
                200,
                "Bank info updated successfully",
                response
        ));
    }

    @GetMapping("/mine")
    public ResponseEntity<APIResponse<UserWalletResponse>> getUserWalletOfMine() {
        return ResponseEntity.ok(
                APIResponse.response(
                        200,
                        "Get my wallet successfully",
                        userWalletService.getUserWalletOfMine()
                )
        );
    }

    @PatchMapping("/{userCode}/withdraw-to-bank")
    @Operation(summary = "rút tiền (cái này khi admin chấp nhận là sài)")
    public ResponseEntity<APIResponse<UserWalletResponse>> withdrawToBank(
            @PathVariable String userCode
    ) {
        return ResponseEntity.ok(
                APIResponse.response(
                        200,
                        "Withdraw request processed successfully",
                        userWalletService.withdrawToBank(userCode)
                )
        );
    }

    @PostMapping("/withdrawal-request")
    public ResponseEntity<APIResponse<Void>> createWithdrawalRequest(
            @RequestParam BigDecimal amountRequestWithdrawal
    ) {
        userWalletService.createRequest(amountRequestWithdrawal);

        return ResponseEntity.ok(
                APIResponse.response(
                        200,
                        "Create withdrawal request successfully",
                        null
                )
        );
    }

    @PostMapping("/deposit/vnpay")
    public ResponseEntity<APIResponse<String>> depositByVnPay(
            @RequestParam(required = false) BigDecimal depositAmount,
            @RequestParam(required = false) String description,
            @RequestBody(required = false) UserWalletRequest body,
            HttpServletRequest request
    ) {
        BigDecimal amount = depositAmount != null ? depositAmount : (body != null ? body.getDepositAmount() : null);
        String desc = description != null ? description : (body != null ? body.getDescription() : null);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(
                    APIResponse.response(400, "depositAmount is required and must be greater than 0", null)
            );
        }

        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }
        return ResponseEntity.ok(
                APIResponse.response(
                        200,
                        "Create VNPay payment URL successfully",
                        userWalletService.depositByVnPay(
                                amount,
                                desc,
                                clientIp
                        )
                )
        );
    }

    @PostMapping("/deposit/success")
    public ResponseEntity<APIResponse<Void>> depositSuccessfully(
            @RequestParam(required = false) BigDecimal depositAmount,
            @RequestParam(required = false) UUID transactionId,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        UUID txId = transactionId;
        BigDecimal amount = depositAmount;

        if (txId == null && body != null && body.get("transactionId") != null) {
            txId = UUID.fromString(body.get("transactionId").toString());
        }
        if (amount == null && body != null && body.get("depositAmount") != null) {
            amount = new BigDecimal(body.get("depositAmount").toString());
        }

        if (txId == null) {
            return ResponseEntity.badRequest().body(
                    APIResponse.response(400, "transactionId is required", null)
            );
        }

        userWalletService.depositSuccessfully(amount, txId);

        return ResponseEntity.ok(
                APIResponse.response(
                        200,
                        "Deposit successfully",
                        null
                )
        );
    }

    @GetMapping("/withdrawal-requests")
    @Operation(summary = "Danh sách các yêu cầu rút tiền đang chờ xử lý (admin)")
    public ResponseEntity<APIResponse<PageResponse<WithdrawRequestResponse>>> getWithdrawalRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                APIResponse.response(
                        200,
                        "Get withdrawal requests successfully",
                        userWalletService.getWithdrawalRequests(page, size)
                )
        );
    }
}
