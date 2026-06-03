package com.nqt.workspace_service.controllers;
import com.nqt.workspace_service.dto.response.PagedResponse;
import com.nqt.workspace_service.dto.response.WorkspaceResponse;
import com.nqt.workspace_service.dto.response.WorkspaceSummaryResponse;
import com.nqt.workspace_service.services.interfaces.IWorkspaceQueryService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceQueryController {

    private final IWorkspaceQueryService workspaceQueryService;

    /**
     * API: Lấy chi tiết toàn bộ timeline và thông tin một phòng làm việc cụ thể
     */
    @GetMapping("/{workspaceId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "👁️ LẤY CHI TIẾT TIMELINE PHÒNG LÀM VIỆC",
            description = "🛡️ **API ĐẢM BẢO BẢO MẬT CHÉO (TẦNG SERVICE):** Truy xuất toàn bộ thông tin cấu hình phòng, tiến độ dự án, thông tin Client/Expert và lịch sử các gói nộp bài.\n\n" +
                    "### 🔐 Cơ chế chặn quyền nghiêm ngặt:\n" +
                    "* **Chỉ cho phép người trong cuộc:** Hệ thống kiểm tra ID từ Token, chỉ cho phép **Admin hệ thống**, hoặc **chính Client sở hữu**, hoặc **chính Expert được chỉ định** trong phòng này truy cập.\n" +
                    "* **Lỗi 403 bọc lót:** Bất kỳ User nào khác có Token hợp lệ nhưng cố tình thay đổi `workspaceId` trên URL để chọc phá phòng người khác sẽ bị tầng Service chặn đứng ngay lập tức."
    )
    public ResponseEntity<WorkspaceResponse> getWorkspaceDetails(
            @PathVariable UUID workspaceId,
            @AuthenticationPrincipal Jwt jwt) {

        UUID currentUserId = UUID.fromString(jwt.getClaim("sub").toString());
        boolean isAdmin = jwt.getClaimAsStringList("roles") != null
                && jwt.getClaimAsStringList("roles").contains("ROLE_ADMIN");

        WorkspaceResponse response = workspaceQueryService.getWorkspaceDetails(workspaceId, currentUserId, isAdmin);
        return ResponseEntity.ok(response);
    }

    /**
     * API: Lấy danh sách phân trang tất cả các phòng làm việc của User đang đăng nhập (Dashboard)
     */
    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "📋 DANH SÁCH PHÒNG LÀM VIỆC THEO USER (DASHBOARD)",
            description = "📊 **API TRỢ LÝ FRONTEND (HỖ TRỢ UI/UX):** Trả về danh sách tóm tắt tất cả các không gian làm việc mà User hiện tại đang tham gia, phục vụ trực tiếp cho màn hình Dashboard tổng quan.\n\n" +
                    "### ⚙️ Thuật toán tự động hóa ở tầng Backend:\n" +
                    "1. **Sắp xếp mặc định:** Các phòng mới phát sinh (mới được Hire) sẽ luôn được đẩy lên đầu tiên (`createdAt DESC`).\n" +
                    "2. **Tự động đảo thông tin Đối tác (`partner`):** \n" +
                    "   - Nếu bạn đang đăng nhập với tư cách là `CLIENT`, trường `partner` trong JSON trả về sẽ tự động điền Tên/Avatar của ông `EXPERT` làm thuê cho bạn.\n" +
                    "   - Ngược lại, nếu bạn là `EXPERT`, trường `partner` sẽ tự động hiển thị thông tin ông `CLIENT` thuê bạn.\n\n" +
                    "⚠️ **LƯU Ý DÀNH CHO FRONTEND:** Cấu trúc trả về đã được làm phẳng hoàn toàn dưới dạng `PagedResponse`. Frontend chỉ việc bốc dữ liệu ra đổ vào vòng lặp hiển thị Card list và xử lý thanh phân trang mà không cần viết thêm logic hoán đổi vị trí đối tác."
    )
    public ResponseEntity<PagedResponse<WorkspaceSummaryResponse>> getMyWorkspaces(
            @RequestParam(value = "pageNo", defaultValue = "0", required = false) int pageNo,
            @RequestParam(value = "pageSize", defaultValue = "10", required = false) int pageSize,
            @AuthenticationPrincipal Jwt jwt) {

        UUID currentUserId = UUID.fromString(jwt.getClaim("sub").toString());

        PagedResponse<WorkspaceSummaryResponse> response = workspaceQueryService.getWorkspacesByUser(currentUserId, pageNo, pageSize);
        return ResponseEntity.ok(response);
    }
}
