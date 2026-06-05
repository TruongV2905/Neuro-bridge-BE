package com.nqt.workspace_service.services.implement;

import com.nqt.common_starter.exception.GlobalException;
import com.nqt.workspace_service.dto.response.WorkspaceMemberRespond;
import com.nqt.workspace_service.entities.WorkspaceMember;
import com.nqt.workspace_service.repositories.WorkspaceMemberRepository;
import com.nqt.workspace_service.services.interfaces.IWorkspaceMemberService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class WorkspaceMemberService implements IWorkspaceMemberService {

    WorkspaceMemberRepository memberRepository;

    /**
     * + getWorkspaceMember(userId: UUID): WorkspaceMemberRespond
     * Quyền: Hệ thống ADMIN được xem HOẶC chính người dùng đó tự xem thông tin của mình.
     */
    @Override
    @PreAuthorize("hasRole('ADMIN') or authentication.principal == #userId.toString()")
    public WorkspaceMemberRespond getWorkspaceMember(UUID userId) {
        log.info("Fetching workspace member details for userId: {}", userId);

        return memberRepository.findById(userId)
                .map(this::mapToRespond)
                .orElseThrow(() -> new GlobalException("Member not found with User ID: " + userId));
    }

    /**
     * + getMembersByWorkspace(workspaceId: UUID): List<WorkspaceMemberRespond>
     * Quyền: Hệ thống ADMIN được xem HOẶC người dùng đang gọi API phải là THÀNH VIÊN hợp lệ trong Workspace đó.
     */
    @Override
    @PreAuthorize("hasRole('ADMIN') or @workspaceSecurityExpression.isMemberOf(#workspaceId)")
    public List<WorkspaceMemberRespond> getMembersByWorkspace(UUID workspaceId) {
        log.info("Fetching all members for workspaceId: {}", workspaceId);

        return memberRepository.findByWorkspaceId(workspaceId).stream()
                .map(this::mapToRespond)
                .collect(Collectors.toList());
    }

    // Hàm phụ trợ ánh xạ dữ liệu sang DTO phản hồi đúng cấu trúc sơ đồ lớp
    private WorkspaceMemberRespond mapToRespond(WorkspaceMember member) {
        return WorkspaceMemberRespond.builder()
                .userId(member.getUserId())
                .name(member.getName())
                .avatarUrl(member.getAvatarUrl())
                .role(member.getRole())
                .build();
    }
}