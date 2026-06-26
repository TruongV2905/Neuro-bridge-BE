package com.nqt.jobPost_service.mapper;

import com.nqt.jobPost_service.dto.request.ProposalRequest;
import com.nqt.jobPost_service.dto.request.jobPost.JobPostRequest;
import com.nqt.jobPost_service.dto.response.JobPostResponse;
import com.nqt.jobPost_service.dto.response.ProposalResponse;
import com.nqt.jobPost_service.entity.JobPost;
import com.nqt.jobPost_service.entity.Proposal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProposalMapper {

    @Mapping(target = "expertApply", ignore = true)
    @Mapping(target = "proposalUnlock", ignore = true)
    Proposal toEntity(ProposalRequest request);


    ProposalResponse toResponse(Proposal entity);

    @Mapping(target = "expertApply", ignore = true)
    @Mapping(target = "proposalUnlock", ignore = true)
    void toUpdate(ProposalRequest request, @MappingTarget Proposal entity);
}
