package com.nqt.identity_service.service.impl;

import com.nqt.identity_service.dto.request.CertificateRequest;
import com.nqt.identity_service.dto.request.SkillRequest;
import com.nqt.identity_service.dto.response.CertificateResponse;
import com.nqt.identity_service.dto.response.SkillResponse;

import java.util.List;
import java.util.UUID;

public interface ICertificate {
    CertificateResponse createCertificate(CertificateRequest request);

    List<CertificateResponse> getAllCertificates();

    CertificateResponse getCertificateById(UUID id);

    CertificateResponse updateCertificate(UUID id, CertificateRequest request);

    void deleteCertificate(UUID id);
    List<CertificateResponse> getAllCertificatesByExpertCode(String expertCode);
}
