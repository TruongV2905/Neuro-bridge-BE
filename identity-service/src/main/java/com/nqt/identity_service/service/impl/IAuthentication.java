package com.nqt.identity_service.service.impl;

import com.nqt.identity_service.dto.request.LoginRequest;
import com.nqt.identity_service.dto.request.register.ClientRegisterRequest;
import com.nqt.identity_service.dto.request.register.ExpertRegisterRequest;
import com.nqt.identity_service.dto.response.UserResponse;

public interface IAuthentication {
    void registerClient(ClientRegisterRequest registerRequest);

    void registerExpert(ExpertRegisterRequest registerRequest);

    UserResponse login(LoginRequest loginRequest);
}
