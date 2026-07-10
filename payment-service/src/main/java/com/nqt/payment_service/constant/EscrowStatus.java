package com.nqt.payment_service.constant;
public enum EscrowStatus {

    /**
     * Escrow vừa được tạo cho job post, chưa có tiền nạp vào.
     */
    PENDING,

    /**
     * Tiền đã được client nạp vào escrow và đang bị giữ.
     */
    FUNDED,

    /**
     * Tiền đang bị khóa trong escrow để chờ project hoàn thành.
     */
    HELD,

    /**
     * Tiền đã được release cho expert sau khi project hoàn thành.
     */
    RELEASED,

    /**
     * Tiền đã hoàn lại cho client.
     */
    REFUNDED,

    /**
     * Escrow đang có tranh chấp giữa client và expert.
     */
    DISPUTED,

    CANCELLED
}
