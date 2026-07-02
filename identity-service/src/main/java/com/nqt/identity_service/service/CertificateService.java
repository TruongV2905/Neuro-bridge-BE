package com.nqt.identity_service.service;

import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.exception.GlobalException;
import com.nqt.identity_service.constant.Role;
import com.nqt.identity_service.dto.request.CertificateRequest;
import com.nqt.identity_service.dto.response.CertificateResponse;
import com.nqt.identity_service.entity.Certificate;
import com.nqt.identity_service.entity.ExpertProfile;
import com.nqt.identity_service.entity.User;
import com.nqt.identity_service.mapper.CertificateMapper;
import com.nqt.identity_service.repository.CertificateRepository;
import com.nqt.identity_service.repository.ExpertProfileRepository;
import com.nqt.identity_service.repository.UserRepository;
import com.nqt.identity_service.service.impl.ICertificate;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CertificateService implements ICertificate {
    CertificateRepository certificateRepository;
    CertificateMapper certificateMapper;
    UserRepository userRepository;

    @Override
    public CertificateResponse  createCertificate(CertificateRequest request) {
        Certificate certificate = certificateMapper.toEntity(request);
        ExpertProfile expertProfile = userRepository.findByCode(request.getExpertCode())
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        ErrorCode.NOT_FOUND.getMessage()
                )).getExpertProfile();
        certificate.setExpertProfile(expertProfile);
        return certificateMapper.toResponse(certificateRepository.save(certificate));
    }

    @Override
    public List<CertificateResponse> getAllCertificates() {
        return certificateRepository.findAll()
                .stream()
                .filter(skill -> !skill.isDeleted())
                .map(certificateMapper::toResponse)
                .toList();
    }

    @Override
    public CertificateResponse getCertificateById(UUID id) {
        Certificate certificate = certificateRepository.findById(id)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        ErrorCode.NOT_FOUND.getMessage()
                ));

        return certificateMapper.toResponse(certificate);
    }

    @Override
    public CertificateResponse updateCertificate(UUID id, CertificateRequest request) {
        Certificate certificate = certificateRepository.findById(id)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        ErrorCode.NOT_FOUND.getMessage()
                ));

        certificateMapper.toUpdate(certificate, request);

        return certificateMapper.toResponse(certificateRepository.save(certificate));
    }

    @Override
    public void deleteCertificate(UUID id) {
        Certificate certificate = certificateRepository.findById(id)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        ErrorCode.NOT_FOUND.getMessage()
                ));

        certificate.setDeleted(true);
        certificateRepository.save(certificate);
    }

    @Override
    public List<CertificateResponse> getAllCertificatesByExpertCode(String expertCode) {
        User expert = userRepository.findByCode(expertCode)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        ErrorCode.NOT_FOUND.getMessage()
                ));
        if(expert.getRole() != Role.EXPERT) {
            throw new GlobalException(
                    ErrorCode.UNAUTHORIZED.getCode(),
                    ErrorCode.UNAUTHORIZED.getMessage()
            );
        }

        return expert.getExpertProfile().getCertificates()
                .stream()
                .filter(certificate -> !certificate.isDeleted())
                .map(certificateMapper::toResponse)
                .toList();
    }
}
