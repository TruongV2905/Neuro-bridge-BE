package com.nqt.payment_service.constant;

public enum TransactionType {

    // Nạp tiền vào ví
    WALLET_DEPOSIT,

    // Rút tiền khỏi ví
    WALLET_WITHDRAW,

    // Mua / mở khóa proposal chi tiết của expert
    PROPOSAL_PAYMENT,

    // Client nạp / khóa tiền vào escrow cho project
    PROJECT_ESCROW_DEPOSIT,

    // Release tiền từ escrow cho expert sau khi hoàn thành project/milestone
    PROJECT_RELEASE,

    // Hoàn tiền từ escrow về ví client
    PROJECT_REFUND,

    // Hoàn tiền giao dịch thông thường
    REFUND,

    // Trừ phí hệ thống
    SYSTEM_FEE,

    // Cộng tiền thủ công bởi admin
    ADMIN_ADD,

    // Trừ tiền thủ công bởi admin
    ADMIN_DEDUCT
}
