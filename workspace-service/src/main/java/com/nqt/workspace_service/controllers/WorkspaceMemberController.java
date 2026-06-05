package com.nqt.workspace_service.controllers;
import com.nqt.common_starter.dto.response.APIResponse;
import com.nqt.workspace_service.dto.response.WorkspaceMemberRespond;
import com.nqt.workspace_service.services.interfaces.IWorkspaceMemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/workspace-members")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Workspace Member Controller", description = "Quản lý và truy vấn thông tin thành viên trong không gian làm việc (Workspace)")
public class WorkspaceMemberController {

    IWorkspaceMemberService memberService;

    @Operation(
            summary = "Lấy thông tin chi tiết của một thành viên",
            description = "Truy xuất thông tin profile bao gồm tên, ảnh đại diện và vai trò của một thành viên dựa trên ID người dùng"
    )
    @GetMapping("/{userId}")
    public ResponseEntity<APIResponse<WorkspaceMemberRespond>> getWorkspaceMember(
            @PathVariable
            @Parameter(description = "ID của người dùng (User ID) cần tra cứu", example = "82cf948e-740b-458c-8b4c-e6a92404a20d")
            UUID userId) {
        return ResponseEntity.ok(APIResponse.response(
                HttpStatus.OK.value(),
                "Get workspace member chi tiết thành công",
                memberService.getWorkspaceMember(userId)
        ));
    }

    @Operation(
            summary = "Lấy danh sách thành viên thuộc một Không gian làm việc (Workspace)",
            description = "Trả về danh sách tất cả các thành viên hiện tại đang tham gia vào một Workspace cụ thể dựa trên Workspace ID"
    )
    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<APIResponse<List<WorkspaceMemberRespond>>> getMembersByWorkspace(
            @PathVariable
            @Parameter(description = "ID của Không gian làm việc (Workspace ID) cần lấy danh sách thành viên", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            UUID workspaceId) {
        return ResponseEntity.ok(APIResponse.response(
                HttpStatus.OK.value(),
                "Get danh sách thành viên workspace thành công",
                memberService.getMembersByWorkspace(workspaceId)
        ));
    }
}
