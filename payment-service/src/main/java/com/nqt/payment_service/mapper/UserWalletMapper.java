package com.nqt.payment_service.mapper;

import com.nqt.payment_service.dto.response.UserWalletResponse;
import com.nqt.payment_service.entity.UserWallet;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserWalletMapper {
    UserWalletResponse toResponse(UserWallet userWallet);
}
