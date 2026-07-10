package com.nqt.payment_service.mapper;

import com.nqt.payment_service.dto.response.WithdrawRequestResponse;
import com.nqt.payment_service.entity.UserWallet;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WithdrawRequestMapper {
    WithdrawRequestResponse toResponse(UserWallet wallet);
}