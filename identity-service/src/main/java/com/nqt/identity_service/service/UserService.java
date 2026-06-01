package com.nqt.identity_service.service;

import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.dto.response.PageResponse;
import com.nqt.common_starter.exception.GlobalException;
import com.nqt.common_starter.util.PageUtil;
import com.nqt.identity_service.constant.Role;
import com.nqt.identity_service.constant.UserStatus;
import com.nqt.identity_service.dto.request.user.ClientRequest;
import com.nqt.identity_service.dto.request.user.ExpertRequest;
import com.nqt.identity_service.dto.request.user.UserFilterRequest;
import com.nqt.identity_service.dto.response.UserResponse;
import com.nqt.identity_service.entity.Skill;
import com.nqt.identity_service.entity.User;
import com.nqt.identity_service.mapper.UserMapper;
import com.nqt.identity_service.repository.SkillRepository;
import com.nqt.identity_service.repository.UserRepository;
import com.nqt.identity_service.service.impl.IUser;
import com.nqt.identity_service.specification.UserSpecification;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.SortDirection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService implements IUser {
    UserRepository userRepository;
    UserMapper userMapper;
    SkillRepository skillRepo;


    @Override
    public UserResponse getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findUserByEmail(email);
        return userMapper.toResponse(user);
    }
    private User getCurrentUserInfo() {
        return (User) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    @Override
    public PageResponse<UserResponse> getAllUsers(UserFilterRequest request) {
        Pageable pageable = PageUtil.createPageable(request);

        Specification<User> spec = UserSpecification.filter(request);

        Page<UserResponse> page = userRepository.findAll(spec, pageable)
                .map(userMapper::toResponse);

        return PageResponse.fromPage(page);
    }

    @Override
    public UserResponse getUserByCode(String code) {
        User user = userRepository.findByCode(code)
                .orElseThrow(() -> new GlobalException(400,"User" + ErrorCode.NOT_FOUND));
        return userMapper.toResponse(user);
    }



    @Override
    @Transactional
    public UserResponse updateClient(String code,ClientRequest request) {
        User user = userRepository.findByCode(code)
                .orElseThrow(() -> new GlobalException(400,"User" + ErrorCode.NOT_FOUND));
        User currentUser = getCurrentUserInfo();
        if(!user.getCode().equals(currentUser.getCode()) ) {
            throw new GlobalException(ErrorCode.UNAUTHORIZED.getCode(),ErrorCode.UNAUTHORIZED.getMessage());
        }
        if (user.getRole() != Role.CLIENT) {
            throw new GlobalException(ErrorCode.INVALID_KEY.getCode(),ErrorCode.INVALID_KEY.getMessage() );
        }
        userMapper.updateUserFromClientRequest(request, user);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse updateExpert(String code,ExpertRequest request) {

        User user = userRepository.findByCode(code)
                .orElseThrow(() -> new GlobalException(400,"User" + ErrorCode.NOT_FOUND));
        User currentUser = getCurrentUserInfo();
        if(!user.getCode().equals(currentUser.getCode()) ) {
            throw new GlobalException(ErrorCode.UNAUTHORIZED.getCode(),ErrorCode.UNAUTHORIZED.getMessage());
        }

        if (user.getRole() != Role.EXPERT) {
            throw new GlobalException(ErrorCode.INVALID_KEY.getCode(),ErrorCode.INVALID_KEY.getMessage() );
        }
        userMapper.updateUserFromExpertRequest(request, user);
        if (request.getSkillIds() != null) {
            Set<Skill> skills = new HashSet<>(
                    skillRepo.findAllById(request.getSkillIds())
            );
            user.getExpertProfile().setSkills(skills);
        }
        return userMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void changeStatus(String code, UserStatus status) {
        User user = userRepository.findByCode(code)
                .orElseThrow(() -> new GlobalException(400,"User" + ErrorCode.NOT_FOUND));

        user.setStatus(status);

        userRepository.save(user);
    }


}
