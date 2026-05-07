package com.nqt.identity_service.service.impl;

import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.identity_service.dto.request.LoginRequest;
import com.nqt.identity_service.dto.request.RegisterRequest;
import com.nqt.identity_service.dto.response.UserResponse;
import com.nqt.identity_service.entity.User;

public interface IAuthentication {
    APIResponse<String> register(RegisterRequest registerRequest);
    APIResponse<UserResponse> login(LoginRequest loginRequest);

    APIResponse<UserResponse> getCurrentUser();
}
