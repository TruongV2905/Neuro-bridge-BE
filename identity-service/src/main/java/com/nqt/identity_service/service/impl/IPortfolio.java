package com.nqt.identity_service.service.impl;

import com.nqt.identity_service.dto.request.PortfolioRequest;
import com.nqt.identity_service.dto.response.PortfolioResponse;

import java.util.List;
import java.util.UUID;

public interface IPortfolio {
    PortfolioResponse createPortfolio(PortfolioRequest request);

    List<PortfolioResponse> getAllPortfolios();

    PortfolioResponse getPortfolioById(UUID id);

    PortfolioResponse updatePortfolio(UUID id, PortfolioRequest request);

    void deletePortfolio(UUID id);

    List<PortfolioResponse> getAllPortfoliosByExpertCode(String expertCode);
}
