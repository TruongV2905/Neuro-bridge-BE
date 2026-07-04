package com.nqt.payment_service.mapper;

import com.nqt.payment_service.constant.TransactionStatus;
import com.nqt.payment_service.dto.request.PaymentTransactionRequest;
import com.nqt.payment_service.dto.response.PaymentTransactionResponse;
import com.nqt.payment_service.entity.PaymentTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentTransactionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", expression = "java(resolveStatus(request))")
    PaymentTransaction toEntity(PaymentTransactionRequest request);

    PaymentTransactionResponse toResponse(PaymentTransaction paymentTransaction);

    default TransactionStatus resolveStatus(PaymentTransactionRequest request) {
        return request.getStatus() != null ? request.getStatus() : TransactionStatus.PENDING;
    }
}