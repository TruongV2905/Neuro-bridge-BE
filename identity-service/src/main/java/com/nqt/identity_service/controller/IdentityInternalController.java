package com.nqt.identity_service.controller;

import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.identity_service.dto.request.skill.SkillSuggestionRequest;
import com.nqt.identity_service.dto.response.UserResponse;
import com.nqt.identity_service.dto.response.skill.SkillSuggestionResult;
import com.nqt.identity_service.service.SkillService;
import com.nqt.identity_service.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/internal")
public class IdentityInternalController {
    UserService userService;
    SkillService skillService;

    @GetMapping("/current-user")
    public ResponseEntity<UserResponse> currentUser() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @PostMapping("skill/suggest")
    public SkillSuggestionResult suggestSkills(@RequestBody SkillSuggestionRequest request) {
        return skillService.suggestSkills(request);
    }

}
