package com.nqt.identity_service.controller;

import com.nqt.common_starter.dto.request.PageRequest;
import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.common_starter.dto.response.PageResponse;
import com.nqt.identity_service.dto.request.skill.SkillRequest;
import com.nqt.identity_service.dto.response.skill.SkillResponse;
import com.nqt.identity_service.service.SkillService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/skill")
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

    @PostMapping(
            value = "/import",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<APIResponse<Void>> createSkillsByFile(
            @RequestParam("file") MultipartFile file
    ) {
        skillService.createSkillsByFile(file);

        return ResponseEntity.status(201).body(
                APIResponse.response(
                        201,
                        "Import skills successfully",
                        null
                )
        );
    }


    @GetMapping
    public ResponseEntity<APIResponse<PageResponse<SkillResponse>>> getAllSkills(@ModelAttribute PageRequest pageRequest) {
        return ResponseEntity.ok(
                APIResponse.response(200,"Get skills successfully",skillService.getAllSkills(pageRequest))
        );
    }

    @GetMapping("/admin")
    public ResponseEntity<APIResponse<PageResponse<SkillResponse>>> getAllSkillsByAdmin(@ModelAttribute PageRequest pageRequest) {
        return ResponseEntity.ok(
                APIResponse.response(200,"Get skills successfully",skillService.getAllSkillsForAdmin(pageRequest))
        );
    }


    @GetMapping("/{id}")
    public ResponseEntity<APIResponse<SkillResponse>> getSkillById(@PathVariable long id) {
        return ResponseEntity.ok(
                APIResponse.response(200,"Get skill successfully",skillService.getSkillById(id))
        );
    }

    @GetMapping("/expert/{code}")
    public ResponseEntity<APIResponse<List<SkillResponse>>> getSkillsByExpertCode(@PathVariable String code) {
        return ResponseEntity.ok(
                APIResponse.response(200,"Get skill successfully",skillService.getAllSkillsByExpertCode(code)));

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
        skillService.toggleSkill(id);

        return ResponseEntity.ok(
                APIResponse.response(200,"Delete skill successfully",null)
        );
    }
}
