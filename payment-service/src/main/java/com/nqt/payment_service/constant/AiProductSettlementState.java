package com.nqt.payment_service.constant;

/**
 * Payment-owned state for the permanent ProductPurchase correlation key.
 * NOT_FOUND and IDEMPOTENCY_CONFLICT are RPC results, not persisted states.
 */
public enum AiProductSettlementState {
    IN_PROGRESS,
    SUCCESS,
    INSUFFICIENT_BALANCE,
    WALLET_NOT_ACTIVE,
    PAYMENT_FAILED
}
