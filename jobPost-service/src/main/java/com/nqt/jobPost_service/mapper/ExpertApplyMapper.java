package com.nqt.jobPost_service.mapper;

import com.nqt.jobPost_service.dto.request.ExpertApplyRequest;
import com.nqt.jobPost_service.dto.request.jobPost.JobPostRequest;
import com.nqt.jobPost_service.dto.response.ExpertApplyResponse;
import com.nqt.jobPost_service.dto.response.JobPostResponse;
import com.nqt.jobPost_service.entity.ExpertApply;
import com.nqt.jobPost_service.entity.JobPost;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ExpertApplyMapper {

    @Mapping(target = "jobPost", ignore = true)
    @Mapping(target = "proposal", ignore = true)
    ExpertApply toEntity(ExpertApplyRequest request);

    ExpertApplyResponse toResponse(ExpertApply entity);


    @Mapping(target = "jobPost", ignore = true)
    @Mapping(target = "proposal", ignore = true)
    void toUpdate(ExpertApplyRequest request, @MappingTarget ExpertApply entity);
}
