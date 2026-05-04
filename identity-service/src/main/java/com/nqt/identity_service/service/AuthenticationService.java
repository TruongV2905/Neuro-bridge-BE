package com.nqt.identity_service.service;

import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.common_starter.exception.GlobalException;
import com.nqt.identity_service.constant.UserStatus;
import com.nqt.identity_service.dto.request.LoginRequest;
import com.nqt.identity_service.dto.request.RegisterRequest;
import com.nqt.identity_service.dto.response.UserResponse;
import com.nqt.identity_service.entity.User;
import com.nqt.identity_service.mapper.UserMapper;
import com.nqt.identity_service.repository.RoleRepository;
import com.nqt.identity_service.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthenticationService {
    UserRepository userRepository;
    UserMapper userMapper;
    RoleRepository roleRepository;
    TokenService tokenService;

    public APIResponse<UserResponse> login(LoginRequest loginRequest) {
      try {
              //validation
            PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
            User user = userRepository
                  .findByEmail(loginRequest.getEmail())
                  .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_EXISTED));
          boolean authenticated = passwordEncoder.matches(loginRequest.getPassword(), user.getPassword());
          //logic
          if (!authenticated) throw new GlobalException(ErrorCode.UNAUTHENTICATED);

          String token = tokenService.generateToken(user);

              //-map-to-response
            UserResponse userResponse = userMapper.toResponse(user);
            userResponse.setToken(token);
              //return
          return APIResponse.success(userResponse);
      }catch (Exception e){
                  log.info("login: {}", e.getMessage());
                  throw new GlobalException(ErrorCode.OTHER);
      }

    }

    public APIResponse<String> register(RegisterRequest registerRequest) {
        try{
            PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        //validation
        if (userRepository.existsByPhone((registerRequest.getPhone()))) {
            throw new GlobalException(ErrorCode.PHONE_EXISTED);
        }
        if(userRepository.existsByEmail(registerRequest.getEmail())){
            throw new GlobalException(ErrorCode.EMAIL_EXISTED);
        }
        //logic
//            Set<Role> roles = new HashSet<>();
//            roles.add(roleRepository.findByName("USER")
//                    .orElseThrow(() -> new GlobalException(ErrorCode.NOT_FOUND)));
        //-map-to-entity
            System.out.println("Password: " + registerRequest.getPassword());

            User newUser = userMapper.toEntity(registerRequest);
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        newUser.setStatus(UserStatus.ACTIVE);
//        newUser.setRoles(roles);
        newUser.setCreatedAt(LocalDateTime.now());
        //-save
        userRepository.save(newUser);
        //-map-to-response
        userMapper.toResponse(newUser);
        //return
            return APIResponse.success("Register successfully");
        }catch (Exception e){
            log.info("Register failed: {}", e.getMessage());
            throw new GlobalException(ErrorCode.OTHER);
        }
    }
}
