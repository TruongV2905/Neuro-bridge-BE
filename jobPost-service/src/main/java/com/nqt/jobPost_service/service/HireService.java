package com.nqt.jobPost_service.service;

import com.nqt.jobPost_service.constant.ContractStatus;
import com.nqt.jobPost_service.entity.Contract;
import com.nqt.jobPost_service.entity.ProposalUnlock;
import com.nqt.jobPost_service.service.impl.IContractService;
import com.payment.grpc.PaymentServiceGrpc;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HireService implements IHireService {

    private final JobPostRepo jobPostRepo;
    private final ExpertApplyRepo expertApplyRepo;
    private final IContractService contractService;

    @GrpcClient("workspace-service")
    private WorkspaceServiceGrpc.WorkspaceServiceBlockingStub workspaceStub;

    @GrpcClient("payment-service")
    private PaymentServiceGrpc.PaymentServiceBlockingStub paymentStub;

    @Override
    @Transactional
    public HireResponse hireExpert(UUID jobPostId, UUID expertApplyId) {

        // ✅ 1. Lấy userId & role hiện tại
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Người dùng chưa đăng nhập.");
        }

        String currentUserId = auth.getName();

        String currentUserRole = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .map(r -> r.replace("ROLE_", ""))
                .orElseThrow(() -> new AccessDeniedException("Không xác định được vai trò người dùng."));

        if (!"CLIENT".equalsIgnoreCase(currentUserRole)) {
            throw new AccessDeniedException("Chỉ Client mới có quyền thuê Expert.");
        }

        // ✅ 2. JobPost & ExpertApply phải tồn tại
        JobPost jobPost = jobPostRepo.findById(jobPostId)
                .orElseThrow(() -> new EntityNotFoundException("Job không tồn tại: " + jobPostId));

        ExpertApply expertApply = expertApplyRepo.findById(expertApplyId)
                .orElseThrow(() -> new EntityNotFoundException("Đơn ứng tuyển không tồn tại: " + expertApplyId));

        ProposalUnlock proposalUnlock = expertApply.getProposal() != null
                ? expertApply.getProposal().getProposalUnlock()
                : null;

        // ✅ 3. Kiểm tra Contract đã tồn tại cho proposalUnlock này chưa
        Optional<Contract> existingContractOpt = contractService.findByJobId(jobPost.getId());

        if (existingContractOpt.isEmpty()) {
            // === LẦN GỌI ĐẦU: tạo Contract, CHƯA tạo workspace ===
            return createContractStep(jobPost, expertApply, proposalUnlock);
        }

        Contract contract = existingContractOpt.get();

        if (contract.getStatus() != ContractStatus.FULLY_SIGNED) {
            throw new IllegalStateException("Hợp đồng chưa được ký đủ bởi cả 2 bên. Vui lòng ký hợp đồng trước.");
        }

        if (contract.getWorkspaceId() != null) {
            // Đã tạo workspace trước đó (gọi lại lần nữa) → trả về luôn, không gọi gRPC lại
            return HireResponse.builder()
                    .contractId(contract.getId())
                    .workspaceId(contract.getWorkspaceId())
                    .jobPostId(jobPost.getId())
                    .expertCode(expertApply.getExpertCode())
                    .message("Workspace đã được khởi tạo trước đó.")
                    .build();
        }

        return payEscrowThenCreateWorkspace(jobPost, expertApply, contract);
    }

    private HireResponse payEscrowThenCreateWorkspace(JobPost jobPost, ExpertApply expertApply, Contract contract) {

        com.payment.grpc.PayProjectEscrowRequest request = com.payment.grpc.PayProjectEscrowRequest.newBuilder()
                .setJobPostId(jobPost.getId().toString())
                .setClientCode(jobPost.getCreatorCode())
                .setExpertCode(expertApply.getExpertCode())
                .setAmount(contract.getProposedBudget().toPlainString())
                .build();

        com.payment.grpc.PayProjectEscrowResponse response;
        try {
            response = paymentStub.payProjectEscrow(request);
        } catch (Exception e) {
            log.error("❌ Lỗi khi gọi gRPC tới payment-service", e);
            throw new RuntimeException("Không thể thanh toán vào ví chung. Vui lòng thử lại.", e);
        }

        if (!response.getSuccess()) {
            throw new RuntimeException("Thanh toán ví chung thất bại: " + response.getMessage());
        }

        contractService.markEscrowId(contract.getId(), UUID.fromString(response.getEscrowId()));

        if ("PENDING".equals(response.getStatus())) {
            return HireResponse.builder()
                    .contractId(contract.getId())
                    .jobPostId(jobPost.getId())
                    .expertCode(expertApply.getExpertCode())
                    .shortfallAmount(new java.math.BigDecimal(response.getShortfallAmount()))
                    .message("Ví chung chưa đủ tiền. Vui lòng nạp thêm " + response.getShortfallAmount()
                            + " qua VNPay rồi bấm Thuê Expert lại.")
                    .build();
        }

        // status == FUNDED -> tạo workspace, tái sử dụng nguyên method createWorkspaceStep cũ
        return createWorkspaceStep(jobPost, expertApply, contract);
    }


    private HireResponse createContractStep(JobPost jobPost, ExpertApply expertApply, ProposalUnlock proposalUnlock) {

        if (jobPost.getStatus() == JobPostStatus.ASSIGNED) {
            throw new IllegalStateException("Công việc này đã được thuê Expert khác.");
        }

        if (expertApply.getStatus() != ApplyStatus.PENDING) {
            throw new IllegalStateException("Đơn ứng tuyển này đã được xử lý trước đó.");
        }

        // Chấp nhận ExpertApply được chọn
        expertApply.setStatus(ApplyStatus.APPROVED);
        expertApplyRepo.save(expertApply);

        // Từ chối các ExpertApply còn lại của job này
        List<ExpertApply> others = expertApplyRepo.findAllByJobPost_Id(jobPost.getId());
        for (ExpertApply other : others) {
            if (!other.getId().equals(expertApply.getId()) && other.getStatus() == ApplyStatus.PENDING) {
                other.setStatus(ApplyStatus.REJECTED);
                expertApplyRepo.save(other);
            }
        }

        // Cập nhật trạng thái job
        jobPost.setStatus(JobPostStatus.ASSIGNED);
        jobPostRepo.save(jobPost);

        // Tính deadline
        int days = expertApply.getEstimatedDays() != null
                ? expertApply.getEstimatedDays()
                : jobPost.getEstimatedDays();
        LocalDateTime deadline = LocalDateTime.now().plusDays(days);

        UUID proposalUnlockId = proposalUnlock != null ? proposalUnlock.getId() : null;
        String strategyDetail = proposalUnlock != null
                ? proposalUnlock.getStrategyDetail()
                : null;
        BigDecimal unlockCost = proposalUnlock != null
                ? proposalUnlock.getUnlockCost()
                : expertApply.getProposedBudget();

        BigDecimal proposedBudget = expertApply.getProposedBudget();

        Contract contract = contractService.createContract(
                jobPost.getId(),
                proposalUnlockId,
                expertApply.getId(),
                jobPost.getCreatorCode(),
                expertApply.getExpertCode(),
                strategyDetail,
                unlockCost,
                proposedBudget,
                jobPost.getTitle(),
                deadline
        );

        return HireResponse.builder()
                .contractId(contract.getId())
                .jobPostId(jobPost.getId())
                .expertCode(expertApply.getExpertCode())
                .message("Thuê Expert thành công. Vui lòng ký hợp đồng để tiếp tục khởi tạo workspace.")
                .build();
    }

    private HireResponse createWorkspaceStep(JobPost jobPost, ExpertApply expertApply, Contract contract) {

        CreateWorkspaceRequest request = CreateWorkspaceRequest.newBuilder()
                .setJobId(jobPost.getId().toString())
                .setEscrowId(contract.getEscrowId() != null ? contract.getEscrowId().toString() : "")
                .setClientId(jobPost.getCreatorCode())
                .setExpertId(expertApply.getExpertCode())
                .setProjectName(jobPost.getTitle())
                .setDeadline(toTimestamp(contract.getDeadline()))
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

        UUID workspaceId = UUID.fromString(response.getWorkspaceId());
        contractService.markWorkspaceCreated(contract.getId(), workspaceId);

        return HireResponse.builder()
                .contractId(contract.getId())
                .workspaceId(workspaceId)
                .jobPostId(jobPost.getId())
                .expertCode(expertApply.getExpertCode())
                .message("Hợp đồng đã ký đủ, workspace đã được khởi tạo.")
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