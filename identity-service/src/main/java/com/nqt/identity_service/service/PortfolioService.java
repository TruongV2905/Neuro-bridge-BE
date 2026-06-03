package com.nqt.identity_service.service;

import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.exception.GlobalException;
import com.nqt.identity_service.constant.Role;
import com.nqt.identity_service.dto.request.PortfolioRequest;
import com.nqt.identity_service.dto.response.PortfolioResponse;
import com.nqt.identity_service.entity.ExpertProfile;
import com.nqt.identity_service.entity.Portfolio;
import com.nqt.identity_service.entity.User;
import com.nqt.identity_service.mapper.PortfolioMapper;
import com.nqt.identity_service.repository.ExpertProfileRepository;
import com.nqt.identity_service.repository.PortfolioRepository;
import com.nqt.identity_service.repository.UserRepository;
import com.nqt.identity_service.service.impl.IPortfolio;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PortfolioService implements IPortfolio {
    PortfolioRepository portfolioRepository;
    PortfolioMapper portfolioMapper;
    ExpertProfileRepository expertProfileRepo;
    UserRepository userRepository;

    @Override
    public PortfolioResponse createPortfolio(PortfolioRequest request) {
        Portfolio portfolio = portfolioMapper.toEntity(request);

        ExpertProfile expertProfile = userRepository.findByCode(request.getExpertCode())
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        ErrorCode.NOT_FOUND.getMessage()
                )).getExpertProfile();

        portfolio.setExpertProfile(expertProfile);

        return portfolioMapper.toResponse(portfolioRepository.save(portfolio));
    }

    @Override
    public List<PortfolioResponse> getAllPortfolios() {
        return portfolioRepository.findAll()
                .stream()
                .filter(portfolio -> !portfolio.isDeleted())
                .map(portfolioMapper::toResponse)
                .toList();
    }

    @Override
    public PortfolioResponse getPortfolioById(UUID id) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        ErrorCode.NOT_FOUND.getMessage()
                ));

        return portfolioMapper.toResponse(portfolio);
    }

    @Override
    public PortfolioResponse updatePortfolio(UUID id, PortfolioRequest request) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        ErrorCode.NOT_FOUND.getMessage()
                ));

        portfolioMapper.toUpdate(portfolio, request);

        return portfolioMapper.toResponse(portfolioRepository.save(portfolio));
    }

    @Override
    public void deletePortfolio(UUID id) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        ErrorCode.NOT_FOUND.getMessage()
                ));

        portfolio.setDeleted(true);
        portfolioRepository.save(portfolio);
    }

    @Override
    public List<PortfolioResponse> getAllPortfoliosByExpertCode(String expertCode) {
        User expert = userRepository.findByCode(expertCode)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        ErrorCode.NOT_FOUND.getMessage()
                ));

        if (expert.getRole() != Role.EXPERT) {
            throw new GlobalException(
                    ErrorCode.UNAUTHORIZED.getCode(),
                    ErrorCode.UNAUTHORIZED.getMessage()
            );
        }

        return expert.getExpertProfile().getPortfolios()
                .stream()
                .filter(portfolio -> !portfolio.isDeleted())
                .map(portfolioMapper::toResponse)
                .toList();
    }
}
