package com.nqt.payment_service.controller;

import com.nqt.payment_service.dto.request.UpdateBankInfoRequest;
import com.nqt.payment_service.dto.response.UserWalletResponse;
import com.nqt.payment_service.service.impl.IUserWallet;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class UserWalletController {

    private final IUserWallet userWalletService;

    @PutMapping("/me/bank-info")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cập nhật thông tin tài khoản ngân hàng của ví hiện tại")
    public ResponseEntity<UserWalletResponse> updateBankInfo(
            @Valid @RequestBody UpdateBankInfoRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UserWalletResponse response = userWalletService.updateCurrentUserBankInfo(request);
        return ResponseEntity.ok(response);
    }
}
