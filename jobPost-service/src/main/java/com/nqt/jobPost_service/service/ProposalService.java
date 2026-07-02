package com.nqt.jobPost_service.service;

import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.dto.request.PageRequest;
import com.nqt.common_starter.dto.response.PageResponse;
import com.nqt.common_starter.exception.GlobalException;
import com.nqt.common_starter.util.PageUtil;
import com.nqt.jobPost_service.dto.request.ProposalRequest;
import com.nqt.jobPost_service.dto.response.ProposalResponse;
import com.nqt.jobPost_service.entity.ExpertApply;
import com.nqt.jobPost_service.entity.Proposal;
import com.nqt.jobPost_service.mapper.ProposalMapper;
import com.nqt.jobPost_service.repository.ExpertApplyRepo;
import com.nqt.jobPost_service.repository.ProposalRepo;
import com.nqt.jobPost_service.service.impl.IProposal;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProposalService implements IProposal {

    ProposalRepo proposalRepository;
    ExpertApplyRepo expertApplyRepository;
    ProposalMapper proposalMapper;

    @Override
    @Transactional
    public ProposalResponse createForExpertApply(UUID expertApplyId, ProposalRequest request) {
        ExpertApply expertApply = expertApplyRepository.findById(expertApplyId)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "Expert Apply " + ErrorCode.NOT_FOUND.getMessage()
                ));

        if (expertApply.getProposal() != null) {
            throw new GlobalException(
                    ErrorCode.INVALID_CODE.getCode(),
                    "Expert Apply already has proposal"
            );
        }

        Proposal proposal = proposalMapper.toEntity(request);

        proposal.setExpertApply(expertApply);
        expertApply.setProposal(proposal);

        ExpertApply savedExpertApply = expertApplyRepository.save(expertApply);

        return proposalMapper.toResponse(savedExpertApply.getProposal());
    }

    @Override
    @Transactional
    public ProposalResponse update(UUID proposalId, ProposalRequest request) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "Proposal " + ErrorCode.NOT_FOUND.getMessage()
                ));

        proposalMapper.toUpdate(request, proposal);

        Proposal updatedProposal = proposalRepository.save(proposal);

        return proposalMapper.toResponse(updatedProposal);
    }

    @Override
    public ProposalResponse getById(UUID proposalId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "Proposal " + ErrorCode.NOT_FOUND.getMessage()
                ));

        return proposalMapper.toResponse(proposal);
    }

    @Override
    @Transactional
    public void delete(UUID proposalId) {
        Proposal proposal = proposalRepository.findById(proposalId)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "Proposal " + ErrorCode.NOT_FOUND.getMessage()
                ));

        ExpertApply expertApply = proposal.getExpertApply();

        if (expertApply != null) {
            expertApply.setProposal(null);
            proposal.setExpertApply(null);
            expertApplyRepository.save(expertApply);
        } else {
            proposalRepository.delete(proposal);
        }
    }

    @Override
    public PageResponse<ProposalResponse> getAllByExpertCode(String expertCode, PageRequest pageRequest) {
        Pageable pageable = PageUtil.createPageable(pageRequest);

        Page<ProposalResponse> page = expertApplyRepository
                .findByExpertCode(expertCode, pageable)
                .map(ExpertApply::getProposal)
                .map(proposalMapper::toResponse);

        return PageResponse.fromPage(page);
    }
}