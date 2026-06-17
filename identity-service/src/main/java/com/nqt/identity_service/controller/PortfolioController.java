package com.nqt.identity_service.controller;

import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.identity_service.dto.request.PortfolioRequest;
import com.nqt.identity_service.dto.response.PortfolioResponse;
import com.nqt.identity_service.service.PortfolioService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/portfolio")
public class PortfolioController {
    PortfolioService portfolioService;

    @PostMapping
    public ResponseEntity<APIResponse<PortfolioResponse>> createPortfolio(
            @Valid @RequestBody PortfolioRequest request
    ) {
        return ResponseEntity.status(201).body(
                APIResponse.response(201, "Create portfolio successfully", portfolioService.createPortfolio(request))
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<APIResponse<List<PortfolioResponse>>> getAllPortfolios() {
        return ResponseEntity.ok(
                APIResponse.response(200, "Get portfolios successfully", portfolioService.getAllPortfolios())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<PortfolioResponse>> getPortfolioById(@PathVariable UUID id) {
        return ResponseEntity.ok(
                APIResponse.response(200, "Get portfolio successfully", portfolioService.getPortfolioById(id))
        );
    }

    @GetMapping("/expert/{code}")
    public ResponseEntity<APIResponse<List<PortfolioResponse>>> getPortfoliosByExpertCode(
            @PathVariable String code
    ) {
        return ResponseEntity.ok(
                APIResponse.response(200, "Get portfolios successfully", portfolioService.getAllPortfoliosByExpertCode(code))
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<APIResponse<PortfolioResponse>> updatePortfolio(
            @PathVariable UUID id,
            @Valid @RequestBody PortfolioRequest request
    ) {
        return ResponseEntity.ok(
                APIResponse.response(200, "Update portfolio successfully", portfolioService.updatePortfolio(id, request))
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('EXPERT')")
    public ResponseEntity<APIResponse<String>> deletePortfolio(@PathVariable UUID id) {
        portfolioService.deletePortfolio(id);

        return ResponseEntity.ok(
                APIResponse.response(200, "Delete portfolio successfully", null)
        );
    }
}
