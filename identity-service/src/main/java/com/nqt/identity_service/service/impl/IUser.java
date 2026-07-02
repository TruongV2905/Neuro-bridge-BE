package com.nqt.identity_service.service.impl;

import com.nqt.common_starter.dto.response.PageResponse;
import com.nqt.identity_service.constant.UserStatus;
import com.nqt.identity_service.dto.request.user.ClientRequest;
import com.nqt.identity_service.dto.request.user.ExpertRequest;
import com.nqt.identity_service.dto.request.user.UserFilterRequest;
import com.nqt.identity_service.dto.response.UserResponse;

import java.util.UUID;

public interface IUser {
    UserResponse getCurrentUser();

    UserResponse getUserByCode(String code);

    UserResponse getUserById(UUID id);

    PageResponse<UserResponse> getAllUsers(UserFilterRequest request);

    UserResponse updateClient(ClientRequest request);

    UserResponse updateExpert(ExpertRequest request);

    void changeStatus(String code, UserStatus status);
}
