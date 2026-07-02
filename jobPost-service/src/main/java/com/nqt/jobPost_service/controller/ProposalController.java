package com.nqt.jobPost_service.controller;

import com.nqt.common_starter.dto.request.PageRequest;
import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.common_starter.dto.response.PageResponse;
import com.nqt.jobPost_service.dto.request.ProposalRequest;
import com.nqt.jobPost_service.dto.response.ProposalResponse;
import com.nqt.jobPost_service.service.ProposalService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/proposal")
public class ProposalController {
    ProposalService proposalService;

    @GetMapping()
    public ResponseEntity<APIResponse<PageResponse<ProposalResponse>>> getAllProposalsByExpertCode(
            @RequestParam String expertCode,
            @ModelAttribute PageRequest pageRequest
    ) {
        return ResponseEntity.ok(
                APIResponse.response(
                        HttpStatus.OK.value(),
                        "Get proposals by expert code successfully",
                        proposalService.getAllByExpertCode(expertCode, pageRequest)
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<ProposalResponse>> getProposalById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                APIResponse.response(
                        HttpStatus.OK.value(),
                        "Get proposal successfully",
                        proposalService.getById(id)
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<APIResponse<ProposalResponse>> updateProposal(
            @PathVariable UUID id,
            @RequestBody ProposalRequest request
    ) {
        return ResponseEntity.ok(
                APIResponse.response(
                        HttpStatus.OK.value(),
                        "Update proposal successfully",
                        proposalService.update(id, request)
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<Void>> deleteProposal(
            @PathVariable UUID id
    ) {
        proposalService.delete(id);

        return ResponseEntity.ok(
                APIResponse.response(
                        HttpStatus.OK.value(),
                        "Delete proposal successfully",
                        null
                )
        );
    }
}
