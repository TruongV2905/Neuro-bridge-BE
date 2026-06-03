package com.nqt.identity_service.mapper;

import com.nqt.identity_service.dto.request.PortfolioRequest;
import com.nqt.identity_service.dto.response.PortfolioResponse;
import com.nqt.identity_service.entity.Portfolio;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
@Mapper(componentModel = "spring")
public interface PortfolioMapper {
    @Mapping(target = "expertProfile", ignore = true)
    Portfolio toEntity(PortfolioRequest request);

    PortfolioResponse toResponse(Portfolio portfolio);

    void toUpdate(@MappingTarget Portfolio portfolio, PortfolioRequest request);
}
