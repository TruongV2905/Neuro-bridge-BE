package com.nqt.payment_service.service.impl;

import com.nqt.common_starter.dto.response.PageResponse;
import com.nqt.payment_service.dto.request.UpdateBankInfoRequest;
import com.nqt.payment_service.dto.response.UserWalletResponse;
import com.nqt.payment_service.dto.response.WithdrawRequestResponse;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.UUID;

public interface IUserWallet {
    UserWalletResponse create(String userCode);

    UserWalletResponse getUserWalletOfMine();

    void deposit(String userCode, BigDecimal depositAmount);
    void withdraw(String userCode, BigDecimal withdrawAmount);
    String depositByVnPay(BigDecimal depositAmount,String description, String clientIp);
    UserWalletResponse getWalletAdmin();
    UserWalletResponse withdrawToBank(String userCode);
    void createRequest(BigDecimal amountRequestWithdrawal);
    UserWalletResponse updateCurrentUserBankInfo(UpdateBankInfoRequest request);
    PageResponse<WithdrawRequestResponse> getWithdrawalRequests(int page, int size);

    void depositSuccessfully(BigDecimal depositAmount, UUID transactionId);

}
