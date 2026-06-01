package com.nqt.identity_service.controller;

import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.identity_service.dto.request.SkillRequest;
import com.nqt.identity_service.dto.response.SkillResponse;
import com.nqt.identity_service.service.SkillService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/skill")
@PreAuthorize("hasRole('ADMIN')")
public class SkillController {
    SkillService skillService;

    @PostMapping
    public ResponseEntity<APIResponse<SkillResponse>> createSkill(
            @Valid @RequestBody SkillRequest request
    ) {
        return ResponseEntity.status(201).body(
                APIResponse.response(201,"Create skill successfully", skillService.createSkill(request))
        );
    }

    @GetMapping
    public ResponseEntity<APIResponse<List<SkillResponse>>> getAllSkills() {
        return ResponseEntity.ok(
                APIResponse.response(200,"Get skills successfully",skillService.getAllSkills())
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<SkillResponse>> getSkillById(@PathVariable long id) {
        return ResponseEntity.ok(
                APIResponse.response(200,"Get skill successfully",skillService.getSkillById(id))
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<APIResponse<SkillResponse>> updateSkill(
            @PathVariable long id,
            @Valid @RequestBody SkillRequest request
    ) {
        return ResponseEntity.ok(
                APIResponse.response(200,"Update skill successfully", skillService.updateSkill(id, request))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse<String>> deleteSkill(@PathVariable long id) {
        skillService.deleteSkill(id);

        return ResponseEntity.ok(
                APIResponse.response(200,"Delete skill successfully",null)
        );
    }
}
