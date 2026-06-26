package com.nqt.jobPost_service.mapper;

import com.nqt.jobPost_service.dto.request.jobPost.JobPostRequest;
import com.nqt.jobPost_service.dto.response.JobPostResponse;
import com.nqt.jobPost_service.entity.ExpertApply;
import com.nqt.jobPost_service.entity.JobPost;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface JobPostMapper {
    @Mapping(target = "expertApplies", ignore = true)
    JobPost toEntity(JobPostRequest request);

    @Mapping(target = "expertApplyCodes", source = " expertApplies")
    JobPostResponse toResponse(JobPost entity);


    default Set<String> mapExpertAppliesToCodes(Set<ExpertApply> expertApplies) {
        if (expertApplies == null) {
            return null;
        }
        return expertApplies.stream()
                .map(ExpertApply::getExpertCode)
                .collect(Collectors.toSet());
    }
}
