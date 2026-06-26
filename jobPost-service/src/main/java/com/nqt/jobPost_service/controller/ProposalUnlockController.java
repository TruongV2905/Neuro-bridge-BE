package com.nqt.jobPost_service.controller;

import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.jobPost_service.dto.request.ProposalUnlockRequest;
import com.nqt.jobPost_service.dto.response.ProposalUnlockResponse;
import com.nqt.jobPost_service.service.ProposalUnlockService;
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
@RequestMapping("/proposal-unlock")
public class ProposalUnlockController {
    ProposalUnlockService proposalUnlockService;

    @GetMapping("/{proposalId}")
    public ResponseEntity<APIResponse<ProposalUnlockResponse>> getProposalUnlockById(
            @PathVariable UUID proposalId
    ) {
        return ResponseEntity.ok(
                APIResponse.response(
                        HttpStatus.OK.value(),
                        "Get proposal unlock successfully",
                        proposalUnlockService.getByProposalId(proposalId)
                )
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<APIResponse<ProposalUnlockResponse>> updateProposalUnlock(
            @PathVariable UUID id,
            @RequestBody ProposalUnlockRequest request
    ) {
        return ResponseEntity.ok(
                APIResponse.response(
                        HttpStatus.OK.value(),
                        "Update proposal unlock successfully",
                        proposalUnlockService.update(id, request)
                )
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<Void>> deleteProposalUnlock(
            @PathVariable UUID id
    ) {
        proposalUnlockService.delete(id);

        return ResponseEntity.ok(
                APIResponse.response(
                        HttpStatus.OK.value(),
                        "Delete proposal unlock successfully",
                        null
                )
        );
    }
}
