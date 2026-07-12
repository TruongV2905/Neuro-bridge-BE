package com.nqt.payment_service.controller;

import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.common_starter.dto.response.PageResponse;
import com.nqt.payment_service.dto.request.PaymentTransactionFilterRequest;
import com.nqt.payment_service.dto.request.PaymentTransactionRequest;
import com.nqt.payment_service.dto.response.PaymentTransactionResponse;
import com.nqt.payment_service.service.PaymentTransactionService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/payment-transactions")
public class PaymentTransactionController {

    PaymentTransactionService paymentTransactionService;

    // ---------------------------------------------------------------------
    // Base CRUD
    // ---------------------------------------------------------------------

    @PostMapping
    public ResponseEntity<APIResponse<PaymentTransactionResponse>> create(
            @Valid @RequestBody PaymentTransactionRequest request) {
        return ResponseEntity.ok(APIResponse.response(
                201,
                "Create payment transaction successfully",
                paymentTransactionService.create(request)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<PaymentTransactionResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(APIResponse.response(
                200,
                "Get payment transaction successfully",
                paymentTransactionService.getById(id)
        ));
    }

    @PostMapping("/filter")
    public ResponseEntity<APIResponse<PageResponse<PaymentTransactionResponse>>> filter(
            @Valid @RequestBody PaymentTransactionFilterRequest request) {
        return ResponseEntity.ok(APIResponse.response(
                200,
                "Get payment transactions successfully",
                paymentTransactionService.getAll(request)
        ));
    }

    @GetMapping("/mine")
    public ResponseEntity<APIResponse<PageResponse<PaymentTransactionResponse>>> mine(
            PaymentTransactionFilterRequest request) {
        return ResponseEntity.ok(APIResponse.response(
                200,
                "Get my payment transactions successfully",
                paymentTransactionService.getMyTransactions(request)
        ));
    }

    // ---------------------------------------------------------------------
    // Status transitions
    // ---------------------------------------------------------------------

    @PatchMapping("/{id}/success")
    public ResponseEntity<APIResponse<PaymentTransactionResponse>> markSuccess(@PathVariable UUID id) {
        return ResponseEntity.ok(APIResponse.response(
                200,
                "Mark transaction as success",
                paymentTransactionService.markSuccess(id)
        ));
    }

    @PatchMapping("/{id}/fail")
    public ResponseEntity<APIResponse<PaymentTransactionResponse>> markFailed(@PathVariable UUID id) {
        return ResponseEntity.ok(APIResponse.response(
                200,
                "Mark transaction as failed",
                paymentTransactionService.markFailed(id)
        ));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<APIResponse<PaymentTransactionResponse>> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(APIResponse.response(
                200,
                "Cancel transaction",
                paymentTransactionService.cancel(id)
        ));
    }

    // ---------------------------------------------------------------------
    // Business operations
    // ---------------------------------------------------------------------

    /**
     * Mua / mở khóa proposal chi tiết của expert bằng ví của client hiện tại.
     */
    @PostMapping("/proposal/pay-by-wallet")
    public ResponseEntity<APIResponse<PaymentTransactionResponse>> payProposalByWallet(
            @RequestParam String proposalId) {
        return ResponseEntity.ok(APIResponse.response(
                200,
                "Pay proposal by wallet successfully",
                paymentTransactionService.transactionProposalUnlockByWallet(
                        proposalId
                )
        ));
    }

    /**
     * Nạp tiền vào Escrow của project bằng ví của client hiện tại.
     */
    @PostMapping("/project/deposit-by-wallet")
    public ResponseEntity<APIResponse<PaymentTransactionResponse>> depositProjectByWallet(
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String description,
            @RequestParam String escrowId,
            @RequestParam String projectId) {
        return ResponseEntity.ok(APIResponse.response(
                200,
                "Deposit to project escrow by wallet successfully",
                paymentTransactionService.transactionProjectByWallet(
                        amount, description, escrowId, projectId
                )
        ));
    }
}