package com.nqt.workspace_service.controllers;
import com.nqt.workspace_service.dto.request.SubmitPackageRequest;
import com.nqt.workspace_service.dto.response.SubmissionPackageResponse;
import com.nqt.workspace_service.services.interfaces.IExpertSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/expert")
@RequiredArgsConstructor
public class ExpertSubmissionController {

    private final IExpertSubmissionService expertSubmissionService;

    /**
     * API: Expert thực hiện gửi gói báo cáo sản phẩm
     * Phân quyền: Phải có quyền cấu hình EXPERT trong hệ thống.
     */
    @PostMapping("/submit")
    @PreAuthorize("hasRole('EXPERT') and isAuthenticated()")
    @Operation(
            summary = "📤 EXPERT SUBMIT & RESUBMIT GÓI SẢN PHẨM",
            description = "🚀 **API ĐIỀU PHỐI TIẾN ĐỘ DỰ ÁN:** Được gọi khi Chuyên gia (Expert) hoàn thành công việc hoặc một giai đoạn, tiến hành đóng gói tài liệu/mã nguồn và nộp lên hệ thống để Client duyệt.\n\n" +
                    "### ⚙️ Các cơ chế tự động vận hành ở tầng Backend:\n" +
                    "1. **Tự động tăng Version:** Hệ thống tự động truy vấn các bản nộp cũ trong Workspace để tính toán phiên bản tiếp theo (Ví dụ: `v1` -> `v2` -> `v3`), Frontend không cần truyền thủ công.\n" +
                    "2. **Cấu hình Đóng băng Linh hoạt (`customReleaseDays`):** Tự động thiết lập thời gian hết hạn nghiệm thu dựa trên tham số `customReleaseDays` của request (Mặc định là **3 ngày** nếu truyền `null`). Quá thời hạn này mà Client không bấm Duyệt/Từ chối, hệ thống sẽ kích hoạt luồng giải ngân tự động.\n" +
                    "3. **Lưu trữ Cascade (Composition):** Tự động ánh xạ và lưu vết đồng thời danh sách File đính kèm (`DeliverableFile`) gắn chặt vào vòng đời của Gói nộp.\n" +
                    "4. **Chuyển trạng thái tổng quan:** Trạng thái dự án trong Workspace lập tức chuyển từ `ACTIVE` (trường hợp SUBMIT) hoặc `REVISION_REQUESTED` (trường hợp RESUBMIT) sang **`UNDER_REVIEW`** (Chờ Client đánh giá).\n\n" +
                    "⚠️ **LƯU Ý DÀNH CHO FRONTEND:** Sau khi nhận kết quả trả về `201 Created`, hãy làm mới (Refresh) màn hình tiến trình của cả hai bên. Khóa nút nộp bài phía Expert (chỉ mở lại nếu bị Client từ chối) và bật các nút **'Duyệt (Approve)' / 'Yêu cầu sửa đổi (Reject)'** trên màn hình của Client."
    )
    public ResponseEntity<SubmissionPackageResponse> submitPackage(
            @RequestBody SubmitPackageRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Object subClaim = jwt.getClaim("sub");
        if (subClaim == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID currentExpertId = UUID.fromString(subClaim.toString());
        SubmissionPackageResponse response = expertSubmissionService.submitPackage(request, currentExpertId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
