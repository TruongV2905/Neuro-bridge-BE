package com.nqt.identity_service.service;

import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.common_starter.exception.GlobalException;
import com.nqt.identity_service.constant.Role;
import com.nqt.identity_service.constant.UserStatus;
import com.nqt.identity_service.dto.request.LoginRequest;
import com.nqt.identity_service.dto.request.register.BaseRegisterRequest;
import com.nqt.identity_service.dto.request.register.ClientRegisterRequest;
import com.nqt.identity_service.dto.request.register.ExpertRegisterRequest;
import com.nqt.identity_service.dto.response.UserResponse;
import com.nqt.identity_service.entity.*;
import com.nqt.identity_service.mapper.ClientProfileMapper;
import com.nqt.identity_service.mapper.ExpertProfileMapper;
import com.nqt.identity_service.mapper.UserMapper;
import com.nqt.identity_service.repository.ExpertProfileRepository;
import com.nqt.identity_service.repository.SkillRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

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
    ClientProfileMapper clientProfileMapper;
    ExpertProfileMapper expertProfileMapper;
    SkillRepository skillRepo;



    @Override
    @Transactional
    public String registerClient(ClientRegisterRequest registerRequest) {

        // validation
        validationExists(registerRequest);

        // Map ClientRegisterRequest -> User
        User user = userMapper.toEntity(registerRequest);
        // Mã hóa mật khẩu
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        // Set mặc định
        user.setRole(Role.CLIENT);
        ClientProfile clientProfile = clientProfileMapper.toEntity(registerRequest);
        user.setClientProfile(clientProfile);
        // Lưu user
        userRepository.save(user);

        return "register client success";
    }

    @Override
    @Transactional
    public String registerExpert(ExpertRegisterRequest registerRequest) {

        // validation
        validationExists(registerRequest);
        User user = userMapper.toEntity(registerRequest);

        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        ExpertProfile expertProfile = expertProfileMapper.toEntity(registerRequest);
        Set<Skill> skills = new HashSet<>(
                skillRepo.findAllById(registerRequest.getSkillIds())
        );
        expertProfile.setSkills(skills);
        expertProfile.setUser(user);
        user.setExpertProfile(expertProfile);
        user.setRole(Role.EXPERT);
        userRepository.save(user);

        return "register expert success";
    }

    @Override
    public UserResponse login(LoginRequest loginRequest) {
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

          // 3. Generate Access Token (JWT)
          String accessToken = tokenService.generateToken(user);

          // 4. Generate Refresh Token (random UUID)
          RefreshToken refreshTokenObj = tokenService.createRefreshToken(user);
          String refreshToken = refreshTokenObj.getToken();

          // 5. Response
          UserResponse response = userMapper.toResponse(user);
          response.setAccessToken(accessToken);
          response.setRefreshToken(refreshToken);




          return response;
      } catch (LockedException e) {
          throw new GlobalException(ErrorCode.ACCOUNT_BANNED.getMessage());
      } catch (DisabledException e) {
          throw new GlobalException(ErrorCode.ACCOUNT_INACTIVE.getMessage());
      } catch (BadCredentialsException e) {
          throw new GlobalException("Invalid password");
      } catch (Exception e) {
          log.info("login: {}", e.getMessage());
          throw new GlobalException(e.getMessage());
      }
    }


    private void validationExists(BaseRegisterRequest registerRequest) {

        if (userRepository.existsByPhone(registerRequest.getPhone())) {
            throw new GlobalException(ErrorCode.PHONE_EXISTED.getMessage());
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new GlobalException(ErrorCode.EMAIL_EXISTED.getMessage());
        }
    }

}
