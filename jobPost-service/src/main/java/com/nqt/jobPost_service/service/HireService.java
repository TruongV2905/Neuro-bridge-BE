package com.nqt.jobPost_service.service;

import org.springframework.security.core.Authentication;
import com.google.protobuf.Timestamp;
import com.nqt.jobPost_service.constant.ApplyStatus;
import com.nqt.jobPost_service.constant.JobPostStatus;
import com.nqt.jobPost_service.dto.response.HireResponse;
import com.nqt.jobPost_service.entity.ExpertApply;
import com.nqt.jobPost_service.entity.JobPost;
import com.nqt.jobPost_service.repository.ExpertApplyRepo;
import com.nqt.jobPost_service.repository.JobPostRepo;
import com.nqt.jobPost_service.service.impl.IHireService;
import com.workspace.grpc.CreateWorkspaceRequest;
import com.workspace.grpc.CreateWorkspaceResponse;
import com.workspace.grpc.WorkspaceServiceGrpc;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HireService implements IHireService {

    private final JobPostRepo jobPostRepo;
    private final ExpertApplyRepo expertApplyRepo;

    @GrpcClient("workspace-service")
    private WorkspaceServiceGrpc.WorkspaceServiceBlockingStub workspaceStub;

    @Override
    @Transactional
    public HireResponse hireExpert(UUID jobPostId, UUID expertApplyId) {

        // ✅ 1. Lấy userId & role hiện tại trực tiếp từ SecurityContext (giống JwtAuthenticationSupport đã set)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Người dùng chưa đăng nhập.");
        }

        String currentUserId = auth.getName(); // userId dạng String

        String currentUserRole = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .map(r -> r.replace("ROLE_", ""))
                .orElseThrow(() -> new AccessDeniedException("Không xác định được vai trò người dùng."));

        // ✅ 2. Chỉ CLIENT mới có quyền thuê
        if (!"CLIENT".equalsIgnoreCase(currentUserRole)) {
            throw new AccessDeniedException("Chỉ Client mới có quyền thuê Expert.");
        }

        // ✅ 2. JobPost phải tồn tại
        JobPost jobPost = jobPostRepo.findById(jobPostId)
                .orElseThrow(() -> new EntityNotFoundException("Job không tồn tại: " + jobPostId));


        // ✅ 4. Job phải đang ở trạng thái có thể thuê (chưa ASSIGNED)
        if (jobPost.getStatus() == JobPostStatus.ASSIGNED) {
            throw new IllegalStateException("Công việc này đã được thuê Expert khác.");
        }

        // ✅ 5. ExpertApply phải tồn tại và thuộc đúng job này
        ExpertApply expertApply = expertApplyRepo.findById(expertApplyId)
                .orElseThrow(() -> new EntityNotFoundException("Đơn ứng tuyển không tồn tại: " + expertApplyId));

        if (expertApply.getStatus() != ApplyStatus.PENDING) {
            throw new IllegalStateException("Đơn ứng tuyển này đã được xử lý trước đó.");
        }

        // ✅ 6. Chấp nhận ExpertApply được chọn
        expertApply.setStatus(ApplyStatus.APPROVED);
        expertApplyRepo.save(expertApply);

        // ✅ 7. Từ chối các ExpertApply còn lại của job này
        List<ExpertApply> others = expertApplyRepo.findAllByJobPost_Id(jobPostId);
        for (ExpertApply other : others) {
            if (!other.getId().equals(expertApplyId) && other.getStatus() == ApplyStatus.PENDING) {
                other.setStatus(ApplyStatus.REJECTED);
                expertApplyRepo.save(other);
            }
        }

        // ✅ 8. Cập nhật trạng thái job
        jobPost.setStatus(JobPostStatus.ASSIGNED);
        jobPostRepo.save(jobPost);

        // ✅ 9. Tính deadline dựa trên estimatedDays của ExpertApply được chọn
        int days = expertApply.getEstimatedDays() != null
                ? expertApply.getEstimatedDays()
                : jobPost.getEstimatedDays(); // fallback nếu expert không đề xuất riêng
        LocalDateTime deadline = LocalDateTime.now().plusDays(days);

        // ✅ 10. Gọi gRPC sang workspace-service để khởi tạo workspace
        CreateWorkspaceRequest request = CreateWorkspaceRequest.newBuilder()
                .setJobId(jobPost.getId().toString())
                .setEscrowId("") // chưa có payment-service
                .setClientId(jobPost.getCreatorCode())
                .setExpertId(expertApply.getExpertCode())
                .setProjectName(jobPost.getTitle())
                .setDeadline(toTimestamp(deadline))
                .build();

        CreateWorkspaceResponse response;
        try {
            response = workspaceStub.createWorkspace(request);
        } catch (Exception e) {
            log.error("❌ Lỗi khi gọi gRPC tới workspace-service", e);
            throw new RuntimeException("Không thể khởi tạo workspace. Vui lòng thử lại.", e);
        }

        if (!response.getSuccess()) {
            log.error("❌ workspace-service từ chối tạo workspace: {}", response.getMessage());
            throw new RuntimeException("Tạo workspace thất bại: " + response.getMessage());
        }

        return HireResponse.builder()
                .workspaceId(UUID.fromString(response.getWorkspaceId()))
                .jobPostId(jobPost.getId())
                .expertCode(expertApply.getExpertCode())
                .message("Thuê Expert thành công, workspace đã được khởi tạo.")
                .build();
    }

    private Timestamp toTimestamp(LocalDateTime dateTime) {
        var instant = dateTime.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }
}