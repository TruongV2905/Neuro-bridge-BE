package com.nqt.identity_service.service;

import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.common_starter.exception.GlobalException;
import com.nqt.identity_service.constant.Role;
import com.nqt.identity_service.constant.UserStatus;
import com.nqt.identity_service.dto.request.LoginRequest;
import com.nqt.identity_service.dto.request.RegisterRequest;
import com.nqt.identity_service.dto.response.UserResponse;
import com.nqt.identity_service.entity.RefreshToken;
import com.nqt.identity_service.entity.User;
import com.nqt.identity_service.mapper.UserMapper;
import com.nqt.identity_service.repository.UserRepository;
import com.nqt.identity_service.service.impl.IAuthentication;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthenticationService implements IAuthentication {
    UserRepository userRepository;
    UserMapper userMapper;
    TokenService tokenService;
    AuthenticationManager authenticationManager;
    PasswordEncoder passwordEncoder;
    @Override
    public APIResponse<String> register(RegisterRequest request) {
        //validation
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new GlobalException(ErrorCode.PHONE_EXISTED);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new GlobalException(ErrorCode.EMAIL_EXISTED);
        }

        // Map RegisterRequest => User
        User entity = userMapper.toEntity(request);
        // Mã hóa mật khẩu trước khi lưu
        entity.setPassword(passwordEncoder.encode(request.getPassword()));
        entity.setStatus(UserStatus.ACTIVE);
        entity.setRole(Role.MEMBER);
        // Lưu tài khoản vào DB
        userRepository.save(entity);
        return APIResponse.success(null,"register success");
    }

    @Override
    public APIResponse<UserResponse> login(LoginRequest loginRequest) {
      try {
          // 1. Authenticate bằng Spring Security
          Authentication authentication =
                  authenticationManager.authenticate(
                          new UsernamePasswordAuthenticationToken(
                                  loginRequest.getEmail(),
                                  loginRequest.getPassword()
                          )
                  );

          User user = (User) authentication.getPrincipal();
          // 2. Check trạng thái account
          if (UserStatus.BANNED.equals(user.getStatus())) {
              throw new GlobalException(ErrorCode.ACCOUNT_BANNED);
          }

          if (UserStatus.INACTIVE.equals(user.getStatus())) {
              throw new GlobalException(ErrorCode.ACCOUNT_INACTIVE);
          }

          // 3. Generate Access Token (JWT)
          String accessToken = tokenService.generateToken(user);

//          // 4. Generate Refresh Token (random UUID)
          RefreshToken refreshTokenObj = tokenService.createRefreshToken(user);
          String refreshToken = refreshTokenObj.getToken();

          // 5. Response
          UserResponse response = userMapper.toResponse(user);
          response.setAccessToken(accessToken);
          response.setRefreshToken(refreshToken);




          return APIResponse.success(response, "Login successful");
      } catch (LockedException e) {
          throw new GlobalException(ErrorCode.ACCOUNT_BANNED);
      } catch (DisabledException e) {
          throw new GlobalException(ErrorCode.ACCOUNT_INACTIVE);
      } catch (BadCredentialsException e) {
          throw new GlobalException(ErrorCode.INVALID_PASSWORD);
      } catch (Exception e) {
          log.info("login: {}", e.getMessage());
          throw new GlobalException(ErrorCode.OTHER);
      }
    }


    @Override
    public APIResponse<UserResponse> getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findUserByEmail(email);
        UserResponse response = userMapper.toResponse(user);
        return APIResponse.success(response);
    }

}
