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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
        String userId = getCurrentUserId();

        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new GlobalException(400,"User" + ErrorCode.NOT_FOUND));
        return userMapper.toResponse(user);
    }
    private User getCurrentUserInfo() {
        String userId = getCurrentUserId();

        return userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new GlobalException(400,"User" + ErrorCode.NOT_FOUND));
    }

    /**
     * Resolves the current authenticated user's id from the Spring Security
     * context. Throws a clean {@link GlobalException} (HTTP 401) when there
     * is no authentication instead of NPE — this happens on:
     * <ul>
     *   <li>background tasks / Kafka listeners / @Scheduled jobs,</li>
     *   <li>gRPC calls that did not carry a valid JWT (the net.devh
     *       interceptor will have failed the call before reaching us, but
     *       we still defend against misconfiguration),</li>
     *   <li>internal service-to-service invocations where the JWT was not
     *       propagated.</li>
     * </ul>
     */
    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            log.warn("No Authentication found in SecurityContext — throwing UNAUTHENTICATED");
            throw new GlobalException(
                    ErrorCode.UNAUTHENTICATED.getCode(),
                    ErrorCode.UNAUTHENTICATED.getMessage());
        }
        log.info("Authentication in context: class={}, name='{}', principal={}, isAuthenticated={}",
                authentication.getClass().getSimpleName(),
                authentication.getName(),
                authentication.getPrincipal(),
                authentication.isAuthenticated());
        if (authentication.getName() == null || authentication.getName().isBlank()) {
            log.warn("Authentication.getName() is null/blank — throwing UNAUTHENTICATED");
            throw new GlobalException(
                    ErrorCode.UNAUTHENTICATED.getCode(),
                    ErrorCode.UNAUTHENTICATED.getMessage());
        }
        return authentication.getName();
    }

    @Override
    public PageResponse<UserResponse> getAllUsers(UserFilterRequest request) {

        //vaidlation
        User current = getCurrentUserInfo();
        Pageable pageable = PageUtil.createPageable(request);

        Specification<User> spec = UserSpecification.filter(request);


        Page<UserResponse> page = userRepository.findAll(spec, pageable)
                .map(user -> {
                    UserResponse response = userMapper.toResponse(user);

                    if (current.getRole() == Role.ADMIN) {
                        response.setId(user.getId());
                        response.setEmail(user.getEmail());
                        response.setPhone(user.getPhone());
                        response.setAddress(user.getAddress());
                        response.setDateOfBirth(user.getDateOfBirth());
                    }
                    return response;
                });

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
    public UserResponse updateClient(ClientRequest request) {

        User currentUser = getCurrentUserInfo();
        if (currentUser.getRole() != Role.CLIENT) {
            throw new GlobalException(ErrorCode.INVALID_KEY.getCode(),ErrorCode.INVALID_KEY.getMessage() );
        }
        userMapper.updateUserFromClientRequest(request, currentUser);
        return userMapper.toResponse(userRepository.save(currentUser));
    }

    @Override
    public UserResponse updateExpert(ExpertRequest request) {

        User currentUser = getCurrentUserInfo();
        if (currentUser.getRole() != Role.EXPERT) {
            throw new GlobalException(ErrorCode.INVALID_KEY.getCode(),ErrorCode.INVALID_KEY.getMessage() );
        }
        userMapper.updateUserFromExpertRequest(request, currentUser);
        if (request.getSkillIds() != null) {
            Set<Skill> skills = new HashSet<>(
                    skillRepo.findAllById(request.getSkillIds())
            );
            currentUser.getExpertProfile().setSkills(skills);
        }
        return userMapper.toResponse(userRepository.save(currentUser));
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
