package com.nqt.payment_service.constant;

public enum TransactionStatus {
    // Giao dịch mới tạo, chưa xử lý xong
    PENDING,

    // Giao dịch thành công
    SUCCESS,

    // Giao dịch thất bại
    FAILED,

    // Giao dịch bị hủy
    CANCELLED
}
