package com.nqt.payment_service.mapper;

import com.nqt.payment_service.dto.request.ProjectEscrowRequest;
import com.nqt.payment_service.dto.response.ProjectEscrowResponse;
import com.nqt.payment_service.entity.ProjectEscrow;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectEscrowMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "heldAmount", ignore = true)
    @Mapping(target = "platformFee", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    ProjectEscrow toEntity(ProjectEscrowRequest projectEscrowRequest);

    ProjectEscrowResponse toResponse(ProjectEscrow projectEscrow);

}
