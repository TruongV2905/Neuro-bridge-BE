package com.nqt.jobPost_service.mapper;

import com.nqt.jobPost_service.dto.request.ProposalUnlockRequest;
import com.nqt.jobPost_service.dto.response.ProposalUnlockResponse;
import com.nqt.jobPost_service.entity.ProposalUnlock;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProposalUnlockMapper {

    @Mapping(target = "proposal", ignore = true)
    ProposalUnlock toEntity(ProposalUnlockRequest request);

    ProposalUnlockResponse toResponse(ProposalUnlock entity);

    @Mapping(target = "proposal", ignore = true)
    void toUpdate(ProposalUnlockRequest request, @MappingTarget ProposalUnlock entity);
}
