package com.nqt.identity_service.mapper;

import com.nqt.identity_service.dto.request.CertificateRequest;
import com.nqt.identity_service.dto.response.CertificateResponse;
import com.nqt.identity_service.entity.Certificate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CertificateMapper {

    @Mapping(target = "expertProfile", ignore = true)
    Certificate toEntity(CertificateRequest request);

    CertificateResponse toResponse(Certificate certificate);

    void toUpdate(@MappingTarget Certificate certificate, CertificateRequest request);

}
