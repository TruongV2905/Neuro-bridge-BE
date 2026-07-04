package com.nqt.payment_service.service.impl;

import com.nqt.payment_service.dto.request.UpdateBankInfoRequest;
import com.nqt.payment_service.dto.response.UserWalletResponse;

import java.math.BigDecimal;

public interface IUserWallet {
    UserWalletResponse create(String userCode);

    UserWalletResponse getUserWalletOfMine();

    UserWalletResponse deposit(String userCode, BigDecimal depositAmount);
    public UserWalletResponse withdraw(String userCode, BigDecimal withdrawAmount);
    String depositByVnPay(BigDecimal depositAmount,String description, String clientIp);
    UserWalletResponse getWalletAdmin();
    UserWalletResponse withdrawToBank(String userCode);
    void createRequest(BigDecimal amountRequestWithdrawal);
    void updateCurrentUserBankInfo(UpdateBankInfoRequest request);
}
