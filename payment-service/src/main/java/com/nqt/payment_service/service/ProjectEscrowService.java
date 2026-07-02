package com.nqt.payment_service.service;

import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.exception.GlobalException;
import com.nqt.payment_service.constant.EscrowStatus;
import com.nqt.payment_service.dto.request.ProjectEscrowRequest;
import com.nqt.payment_service.dto.response.ProjectEscrowResponse;
import com.nqt.payment_service.entity.ProjectEscrow;
import com.nqt.payment_service.httpclient.IdentityServiceClient;
import com.nqt.payment_service.mapper.ProjectEscrowMapper;
import com.nqt.payment_service.repository.ProjectEscrowRepo;
import com.nqt.payment_service.service.impl.IProjectEscrow;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProjectEscrowService implements IProjectEscrow {

    ProjectEscrowRepo projectEscrowRepo;
    ProjectEscrowMapper projectEscrowMapper;
    IdentityServiceClient identityServiceClient;
    VNPayService vNPayService;
    PaymentTransactionService paymentTransactionService;

    // ---------------------------------------------------------------------
    // Base CRUD
    // ---------------------------------------------------------------------

    @Override
    @Transactional
    public ProjectEscrow create(ProjectEscrowRequest projectEscrowRequest) {
        validateRequest(projectEscrowRequest);

        ProjectEscrow escrow = projectEscrowMapper.toEntity(projectEscrowRequest);

        if (escrow.getHeldAmount() == null) {
            escrow.setHeldAmount(escrow.getAmount());
        }
        if (escrow.getPlatformFee() == null) {
            escrow.setPlatformFee(BigDecimal.ZERO);
        }
        if (escrow.getStatus() == null) {
            escrow.setStatus(EscrowStatus.FUNDED);
        }

        ProjectEscrow saved = projectEscrowRepo.save(escrow);
        log.info(
                "Created project escrow id={} projectId={} clientCode={} expertCode={} amount={}",
                saved.getId(),
                saved.getProjectId(),
                saved.getClientCode(),
                saved.getExpertCode(),
                saved.getAmount()
        );

        return saved;
    }

    @Override
    public ProjectEscrowResponse getById(UUID id) {
        ProjectEscrow escrow = projectEscrowRepo.findById(id)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "ProjectEscrow not found with id: " + id
                ));
        return projectEscrowMapper.toResponse(escrow);
    }

    // ---------------------------------------------------------------------
    // deposit — cộng tiền vào escrow, tự động chia theo tỉ lệ phí hệ thống
    //   amount = tổng tiền client nạp vào escrow
    //   heldAmount += amount * 0.90  (sẽ được release cho expert)
    //   platformFee += amount * 0.10  (phí hệ thống)
    // ---------------------------------------------------------------------

    @Transactional
    public void deposit(String escrowId, BigDecimal amount) {
        ProjectEscrow escrow = projectEscrowRepo.findById(UUID.fromString(escrowId))
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "ProjectEscrow not found with id: " + escrowId
                ));

        if (escrow.getStatus() != EscrowStatus.FUNDED
                && escrow.getStatus() != EscrowStatus.HELD) {
            throw new GlobalException(
                    ErrorCode.DATA_INVALID.getCode(),
                    "Cannot deposit into escrow with status: " + escrow.getStatus()
            );
        }

        BigDecimal platformFee = amount
                .multiply(new BigDecimal("0.10"))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal heldPortion = amount.subtract(platformFee);

        escrow.setAmount(escrow.getAmount().add(amount));
        escrow.setHeldAmount(escrow.getHeldAmount().add(heldPortion));
        escrow.setPlatformFee(escrow.getPlatformFee().add(platformFee));
        projectEscrowRepo.save(escrow);

        log.info(
                "Escrow deposit done: escrowId={} amount={} heldPortion={} platformFee={}",
                escrowId, amount, heldPortion, platformFee
        );
    }



    // ---------------------------------------------------------------------
    // release — giải phóng toàn bộ heldAmount cho expert (PROJECT_RELEASE)
    // ---------------------------------------------------------------------

    @Transactional
    public ProjectEscrowResponse release(String escrowId) {
        ProjectEscrow escrow = projectEscrowRepo.findById(UUID.fromString(escrowId))
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "ProjectEscrow not found with id: " + escrowId
                ));

        escrow.setStatus(EscrowStatus.RELEASED);
        escrow.setHeldAmount(BigDecimal.ZERO);
        ProjectEscrow saved = projectEscrowRepo.save(escrow);

        log.info("Escrow released: escrowId={}", escrowId);
        return projectEscrowMapper.toResponse(saved);
    }

    // ---------------------------------------------------------------------
    // refund — hoàn toàn bộ tiền cho client (PROJECT_REFUND)
    // ---------------------------------------------------------------------

    @Transactional
    public ProjectEscrowResponse refund(String escrowId) {
        ProjectEscrow escrow = projectEscrowRepo.findById(UUID.fromString(escrowId))
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "ProjectEscrow not found with id: " + escrowId
                ));

        escrow.setStatus(EscrowStatus.REFUNDED);
        escrow.setAmount(BigDecimal.ZERO);
        escrow.setHeldAmount(BigDecimal.ZERO);
        ProjectEscrow saved = projectEscrowRepo.save(escrow);

        log.info("Escrow refunded: escrowId={}", escrowId);
        return projectEscrowMapper.toResponse(saved);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private void validateRequest(ProjectEscrowRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new GlobalException(
                    ErrorCode.DATA_INVALID.getCode(),
                    "Escrow amount must be greater than 0"
            );
        }

        boolean hasClient = request.getClientCode() != null && !request.getClientCode().isBlank();
        boolean hasExpert = request.getExpertCode() != null && !request.getExpertCode().isBlank();
        if (!hasClient && !hasExpert) {
            throw new GlobalException(
                    ErrorCode.DATA_INVALID.getCode(),
                    "At least one of clientCode / expertCode must be provided"
            );
        }
    }
}
