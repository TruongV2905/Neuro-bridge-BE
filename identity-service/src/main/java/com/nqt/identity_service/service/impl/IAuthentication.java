package com.nqt.identity_service.service.impl;

import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.identity_service.dto.request.LoginRequest;
import com.nqt.identity_service.dto.request.register.ClientRegisterRequest;
import com.nqt.identity_service.dto.request.register.ExpertRegisterRequest;
import com.nqt.identity_service.dto.response.UserResponse;

public interface IAuthentication {
    String registerClient(ClientRegisterRequest registerRequest);

    String registerExpert(ExpertRegisterRequest registerRequest);

    UserResponse login(LoginRequest loginRequest);

}
