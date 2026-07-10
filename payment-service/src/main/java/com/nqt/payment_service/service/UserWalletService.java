package com.nqt.payment_service.service;

import com.identity.grpc.UserResponse;
import com.nqt.common_starter.dto.response.PageResponse;
import com.nqt.common_starter.exception.GlobalException;
import com.nqt.payment_service.constant.PaymentReferenceType;
import com.nqt.payment_service.constant.TransactionType;
import com.nqt.payment_service.constant.WalletStatus;
import com.nqt.payment_service.dto.request.PaymentTransactionRequest;
import com.nqt.payment_service.dto.request.UpdateBankInfoRequest;
import com.nqt.payment_service.dto.response.PaymentTransactionResponse;
import com.nqt.payment_service.dto.response.UserWalletResponse;
import com.nqt.payment_service.dto.response.WithdrawRequestResponse;
import com.nqt.payment_service.entity.UserWallet;
import com.nqt.payment_service.httpclient.IdentityServiceClient;
import com.nqt.payment_service.mapper.UserWalletMapper;
import com.nqt.payment_service.mapper.WithdrawRequestMapper;
import com.nqt.payment_service.repository.UserWalletRepo;
import com.nqt.payment_service.service.impl.IUserWallet;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
//@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserWalletService implements IUserWallet {
    UserWalletRepo userWalletRepository;
    UserWalletMapper userWalletMapper;
    WithdrawRequestMapper withdrawRequestMapper;
    IdentityServiceClient  identityServiceClient;
    VNPayService vNPayService;
    UserWalletRepo userWalletRepo;

//    @Lazy
    PaymentTransactionService paymentTransactionService;

    public UserWalletService(
            UserWalletRepo userWalletRepository,
            UserWalletMapper userWalletMapper,
            WithdrawRequestMapper withdrawRequestMapper,
            IdentityServiceClient identityServiceClient,
            VNPayService vNPayService,
            @org.springframework.context.annotation.Lazy PaymentTransactionService paymentTransactionService,
            UserWalletRepo userWalletRepo
    ) {
        this.userWalletRepository = userWalletRepository;
        this.userWalletMapper = userWalletMapper;
        this.withdrawRequestMapper = withdrawRequestMapper;
        this.identityServiceClient = identityServiceClient;
        this.vNPayService = vNPayService;
        this.paymentTransactionService = paymentTransactionService;
        this.userWalletRepo = userWalletRepo;
    }

    @Override
    public UserWalletResponse create(String userCode) {
        if (userWalletRepository.existsByUserCode(userCode)) {
            throw new GlobalException("Wallet already exists for userCode: " + userCode);
        }

        UserWallet wallet = UserWallet.builder()
                .userCode(userCode)
                .status(WalletStatus.ACTIVE)
                .walletBalance(BigDecimal.ZERO)
                .totalEarned(BigDecimal.ZERO)
                .totalWithdrawn(BigDecimal.ZERO)
                .build();

        wallet = userWalletRepository.save(wallet);

        return userWalletMapper.toResponse(wallet);
    }

    @Override
    public UserWalletResponse getUserWalletOfMine() {
        UserResponse user = identityServiceClient.getCurrentUser();
        String userCode = user.getCode();
        UserWallet wallet = userWalletRepository.findByUserCode(userCode)
                .orElseThrow(() -> new RuntimeException("Wallet not found for userCode: " + userCode));

        return userWalletMapper.toResponse(wallet);
    }




    @Override
    public void deposit(String userCode, BigDecimal depositAmount) {

        if (depositAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new GlobalException("Deposit amount must be greater than 0");
        }

        UserWallet wallet = userWalletRepository.findByUserCode(userCode)
                .orElseThrow(() -> new GlobalException("Wallet not found for userCode: " + userCode));

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new GlobalException("Wallet is not active");
        }


        wallet.setWalletBalance(wallet.getWalletBalance().add(depositAmount));
        userWalletRepository.save(wallet);

    }
    @Override
    public void withdraw(String userCode, BigDecimal withdrawAmount) {

        if (withdrawAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new GlobalException("Withdraw amount must be greater than 0");
        }

        UserWallet wallet = userWalletRepository.findByUserCode(userCode)
                .orElseThrow(() -> new GlobalException("Wallet not found for userCode: " + userCode));

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new GlobalException("Wallet is not active");
        }

        if (wallet.getWalletBalance().compareTo(withdrawAmount) < 0) {
            throw new GlobalException("Wallet balance is not enough");
        }

        wallet.setWalletBalance(wallet.getWalletBalance().subtract(withdrawAmount));
        wallet.setTotalWithdrawn(wallet.getTotalWithdrawn().add(withdrawAmount));
        userWalletRepository.save(wallet);
    }


    @Override
    public UserWalletResponse getWalletAdmin() {
        UserWallet adminWallet = userWalletRepository.findFirstByUserCodeStartingWith("ADM")
                .orElseThrow(() -> new GlobalException("Admin wallet not found"));
        return userWalletMapper.toResponse(adminWallet);
    }


    @Transactional
    @Override
    public String depositByVnPay(BigDecimal depositAmount,String description, String clientIp) {
        UserResponse user = identityServiceClient.getCurrentUser();
        String userCode = user.getCode();
        PaymentTransactionResponse paymentTransactionResponse = paymentTransactionService.payment(
                null,
                userCode,
                TransactionType.WALLET_DEPOSIT,
                "depositByVnPay",
                null,
                depositAmount,
                null,
                null
        );
        String paymentTransactionId = paymentTransactionResponse.getId().toString();
        try{
            return  vNPayService.createVnPayUrl(paymentTransactionId,depositAmount,clientIp);
        }catch (Exception e){
            throw new GlobalException("Failed to create VNPay URL: " + e.getMessage());
        }
    }


    @Override
    public void createRequest(BigDecimal amountRequestWithdrawal){
        UserResponse user = identityServiceClient.getCurrentUser();
        UserWallet currentWallet = userWalletRepository.findByUserCode(user.getCode())
                .orElseThrow(() -> new GlobalException("Wallet not found for userCode: " + user.getCode()));
        if (amountRequestWithdrawal == null ||
                amountRequestWithdrawal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new GlobalException("Withdrawal amount must be greater than zero");
        }
        if(currentWallet.getStatus() != WalletStatus.ACTIVE){
            throw new GlobalException("Wallet is not active");
        }
        if (amountRequestWithdrawal.compareTo(currentWallet.getWalletBalance()) >= 0) {
            throw new GlobalException(
                    "Withdrawal amount must be less than the current wallet balance"
            );
        }
        currentWallet.setWalletBalance(currentWallet.getWalletBalance().add(amountRequestWithdrawal));
        currentWallet.setRequestWithdrawal(true);
        userWalletRepository.save(currentWallet);
    }

    @Override
    @Transactional
    public UserWalletResponse updateCurrentUserBankInfo(UpdateBankInfoRequest request) {
        UserResponse currentUser = identityServiceClient.getCurrentUser();
        String userCode = currentUser.getCode(); // đổi theo tên getter thực tế trong proto

        UserWallet wallet = userWalletRepo.findByUserCode(userCode)
                .orElseThrow(() -> new GlobalException("Wallet not found for userCode=" + userCode));

        wallet.setAccountHolderName(request.getAccountHolderName());
        wallet.setAccountNumber(request.getAccountNumber());
        wallet.setBankName(request.getBankName());

        UserWallet savedWallet = userWalletRepo.save(wallet);

        return toResponse(savedWallet);
    }

    @Override
    public PageResponse<WithdrawRequestResponse> getWithdrawalRequests(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        return PageResponse.fromPage(
                userWalletRepo.findAllByRequestWithdrawalTrue(pageable)
                        .map(withdrawRequestMapper::toResponse)
        );
    }

    private UserWalletResponse toResponse(UserWallet wallet) {
        return UserWalletResponse.builder()
                .id(wallet.getId())
                .userCode(wallet.getUserCode())
                .accountHolderName(wallet.getAccountHolderName())
                .accountNumber(wallet.getAccountNumber())
                .bankName(wallet.getBankName())
                .walletBalance(wallet.getWalletBalance())
                .totalEarned(wallet.getTotalEarned())
                .totalWithdrawn(wallet.getTotalWithdrawn())
                .status(wallet.getStatus())
                .build();
    }


    @Override
    @Transactional
    public UserWalletResponse withdrawToBank(String userCode){
        UserWallet currentWallet = userWalletRepository.findByUserCode(userCode)
                .orElseThrow(() -> new GlobalException("Wallet not found for userCode: " + userCode));
        paymentTransactionService.payment(
                userCode,
                null,
                TransactionType.WALLET_WITHDRAW,
                null,
                null,
                currentWallet.getAmountRequestWithdrawal(),
                null,
                null
        );
        currentWallet.setRequestWithdrawal(false);
        currentWallet.setAmountRequestWithdrawal(BigDecimal.ZERO);
        UserWallet savedWallet = userWalletRepository.save(currentWallet);
        return userWalletMapper.toResponse(savedWallet);
    }
}
