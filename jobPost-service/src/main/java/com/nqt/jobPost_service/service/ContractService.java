package com.nqt.jobPost_service.service;

import com.identity.grpc.UserResponse;
import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.dto.event.NotificationEvent;
import com.nqt.common_starter.exception.GlobalException;
import com.nqt.common_starter.kafka.NotificationEventPublisher;
import com.nqt.jobPost_service.constant.ContractStatus;
import com.nqt.jobPost_service.entity.Contract;
import com.nqt.jobPost_service.httpclient.IdentityServiceClient;
import com.nqt.jobPost_service.repository.ContractRepository;
import com.nqt.jobPost_service.service.impl.IContractService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractService implements IContractService {

    private final ContractRepository contractRepository;
    private final IdentityServiceClient identityServiceClient;
    private final NotificationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Contract createContract(UUID jobId, UUID proposalUnlockId,
                                   String clientId, String expertId,
                                   String strategyDetail, BigDecimal unlockCost,
                                   String projectName, LocalDateTime deadline) {

        contractRepository.findByProposalUnlockId(proposalUnlockId).ifPresent(c -> {
            throw new GlobalException(
                    ErrorCode.INVALID_CODE.getCode(),
                    "Contract already exists for this proposal unlock"
            );
        });

        Contract contract = Contract.builder()
                .jobId(jobId)
                .proposalUnlockId(proposalUnlockId)
                .clientId(clientId)
                .expertId(expertId)
                .strategyDetail(strategyDetail)
                .unlockCost(unlockCost)
                .projectName(projectName)
                .deadline(deadline)
                .status(ContractStatus.PENDING)
                .build();

        return contractRepository.save(contract);
    }

    @Override
    @Transactional
    public Contract sign(UUID contractId, String currentUserId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "Contract " + ErrorCode.NOT_FOUND.getMessage()
                ));

        if (contract.getStatus() == ContractStatus.FULLY_SIGNED) {
            throw new GlobalException(
                    ErrorCode.INVALID_CODE.getCode(),
                    "Contract already fully signed"
            );
        }

        String clientRealId = resolveUserId(contract.getClientId());
        String expertRealId = resolveUserId(contract.getExpertId());

        boolean isClient = clientRealId.equals(currentUserId);
        boolean isExpert = expertRealId.equals(currentUserId);

        if (!isClient && !isExpert) {
            throw new GlobalException(
                    ErrorCode.UNAUTHORIZED.getCode(),
                    "You are not a party of this contract"
            );
        }

        // Chặn ký lại nếu đã ký rồi — tránh spam noti
        if (isClient && contract.isClientSigned()) {
            throw new GlobalException(
                    ErrorCode.INVALID_CODE.getCode(),
                    "You have already signed this contract"
            );
        }
        if (isExpert && contract.isExpertSigned()) {
            throw new GlobalException(
                    ErrorCode.INVALID_CODE.getCode(),
                    "You have already signed this contract"
            );
        }

        if (isClient) {
            contract.setClientSigned(true);
            contract.setClientSignedAt(LocalDateTime.now());
        } else {
            contract.setExpertSigned(true);
            contract.setExpertSignedAt(LocalDateTime.now());
        }

        if (contract.isClientSigned() && contract.isExpertSigned()) {
            contract.setStatus(ContractStatus.FULLY_SIGNED);
            notifyContractFullySigned(contract);
        } else if (contract.isClientSigned()) {
            contract.setStatus(ContractStatus.CLIENT_SIGNED);
            notifyContractSignRequired(contract, isClient);
        } else {
            contract.setStatus(ContractStatus.EXPERT_SIGNED);
            notifyContractSignRequired(contract, isClient);
        }

        return contractRepository.save(contract);
    }

    private void notifyContractFullySigned(Contract contract) {
        try {
            UserResponse clientInfo = identityServiceClient.getUserByCode(contract.getClientId());
            UserResponse expertInfo = identityServiceClient.getUserByCode(contract.getExpertId());

            eventPublisher.publish(
                    NotificationEventPublisher.CONTRACT_FULLY_SIGNED,
                    NotificationEvent.builder()
                            .eventType("CONTRACT_FULLY_SIGNED")
                            .domain("contract")
                            .recipientUserId(UUID.fromString(clientInfo.getId())) // client là người thanh toán
                            .senderName(expertInfo.getFullName())
                            .resourceId(contract.getId())
                            .occurredAt(LocalDateTime.now())
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to send CONTRACT_FULLY_SIGNED notification for contract {}: {}",
                    contract.getId(), e.getMessage());
        }
    }

    private String resolveUserId(String code) {
        try {
            UserResponse user = identityServiceClient.getUserByCode(code);
            return user.getId();
        } catch (Exception e) {
            throw new GlobalException(
                    ErrorCode.NOT_FOUND.getCode(),
                    "Cannot resolve user from code: " + code
            );
        }
    }

    private void notifyContractSignRequired(Contract contract, boolean isClientJustSigned) {
        String recipientCode = isClientJustSigned ? contract.getExpertId() : contract.getClientId();
        String senderCode = isClientJustSigned ? contract.getClientId() : contract.getExpertId();

        try {
            UserResponse recipientInfo = identityServiceClient.getUserByCode(recipientCode);
            UserResponse senderInfo = identityServiceClient.getUserByCode(senderCode);

            eventPublisher.publish(
                    NotificationEventPublisher.CONTRACT_SIGN_REQUIRED,
                    NotificationEvent.builder()
                            .eventType("CONTRACT_SIGN_REQUIRED")
                            .domain("contract")
                            .recipientUserId(UUID.fromString(recipientInfo.getId()))
                            .senderName(senderInfo.getFullName())
                            .resourceId(contract.getId())
                            .occurredAt(LocalDateTime.now())
                            .build()
            );
        } catch (Exception e) {
            // Không rollback việc ký hợp đồng nếu bắn noti lỗi — chỉ log lại
            log.error("Failed to send CONTRACT_SIGN_REQUIRED notification for contract {}: {}",
                    contract.getId(), e.getMessage());
        }
    }

    @Override
    public Contract getById(UUID contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "Contract " + ErrorCode.NOT_FOUND.getMessage()
                ));
    }

    @Override
    public Optional<Contract> findByProposalUnlockId(UUID proposalUnlockId) {
        return contractRepository.findByProposalUnlockId(proposalUnlockId);
    }

    @Override
    @Transactional
    public Contract markWorkspaceCreated(UUID contractId, UUID workspaceId) {
        Contract contract = getById(contractId);
        contract.setWorkspaceId(workspaceId);
        return contractRepository.save(contract);
    }
}
