package com.nqt.workspace_service.controller;
import com.nqt.workspace_service.dto.request.DisputeWorkspaceRequest;
import com.nqt.workspace_service.service.interfaces.IWorkspaceOrchestrationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/orchestration")
@RequiredArgsConstructor
public class WorkspaceOrchestrationController {

    private final IWorkspaceOrchestrationService orchestrationService;

    /**
     * API: Báo cáo/Kích hoạt tình trạng tranh chấp phòng làm việc
     * Phân quyền: Người tham gia (CLIENT/EXPERT) hoặc ADMIN hệ thống
     */
    @PostMapping("/dispute")
    @PreAuthorize("hasRole('CLIENT') or hasRole('EXPERT') or hasRole('ADMIN')")
    @Operation(
            summary = "🚨 MỞ TRANH CHẤP KHÔNG GIAN LÀM VIỆC",
            description = "🔥 **API CẤP CỨU HỆ THỐNG:** Được gọi khi một trong hai bên (Client hoặc Expert) xảy ra bất đồng mâu thuẫn không thể tự giải quyết và bấm nút **Khiếu nại/Tranh chấp (Dispute)**.\n\n" +
                    "### 🛑 Ràng buộc dữ liệu nghiêm ngặt (Validation):\n" +
                    "* **Trường `reason` là BẮT BUỘC:** Frontend **phải validate** không cho phép để trống, không được nhập toàn khoảng trắng. Độ dài lý do phải từ **10 ký tự trở lên**.\n\n" +
                    "### 🔄 Chuỗi nghiệp vụ hệ thống kích hoạt ngầm:\n" +
                    "1. **Phong tỏa dòng tiền:** Trạng thái dự án và Workspace chuyển lập tức sang `DISPUTED`. Ví thanh toán Escrow bị **khóa đóng băng tạm thời**, không cho phép tự động giải ngân hay rút tiền.\n" +
                    "2. **Chuyển quyền phân xử:** Hệ thống phát tín hiệu lên Dashboard của Admin để Ban quản trị nhảy vào làm trọng tài, liên hệ hai bên và đưa ra phán quyết hoàn tiền hay chuyển tiền tiếp."
    )
    public ResponseEntity<String> disputeWorkspace( @Valid @RequestBody DisputeWorkspaceRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID currentUserId = UUID.fromString(jwt.getClaim("sub").toString());
        boolean isAdmin = jwt.getClaimAsStringList("roles") != null
                && jwt.getClaimAsStringList("roles").contains("ROLE_ADMIN");

        orchestrationService.disputeWorkspace(request, currentUserId, isAdmin);
        return ResponseEntity.ok("Không gian làm việc đã được chuyển sang trạng thái tranh chấp khẩn cấp.");
    }

    /**
     * API: Đóng băng/Lưu trữ phòng làm việc thủ công (Chỉ dành cho ADMIN hoặc hệ thống)
     */
    @PutMapping("/{workspaceId}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "📦 ADMIN CHỦ ĐỘNG ĐÓNG BĂNG & LƯU TRỮ PHÒNG LÀM VIỆC",
            description = "👔 **API QUẢN TRỊ HỆ THỐNG (ADMIN ONLY):** Được gọi khi Quản trị viên muốn dọn dẹp dữ liệu, chủ động đóng băng một phòng làm việc đã hoàn thành để chuyển vào mục lưu trữ lịch sử (Archived).\n\n" +
                    "### 🛑 Điều kiện và ràng buộc chặt chẽ (Business Rules):\n" +
                    "* **Dự án phải hoàn thành trước:** API chỉ chấp nhận các Workspace có trạng thái dự án bên trong là **`COMPLETED`**. Nếu dự án chưa xong (`ACTIVE`), đang sửa sửa đổi (`REVISION_REQUESTED`) hoặc đang cãi vã (`DISPUTED`), hệ thống sẽ chặn lại và ném lỗi `422 Unprocessable Entity` / `500 Internal Server Error` (do ném ra `IllegalStateException`).\n\n" +
                    "### 🔄 Chuỗi nghiệp vụ hệ thống kích hoạt ngầm (Event-Driven):\n" +
                    "1. **Đóng băng cục bộ:** Cập nhật trạng thái Workspace sang `ARCHIVED` (Khóa toàn bộ tính năng chỉnh sửa dữ liệu).\n" +
                    "2. **Đóng băng cuộc hội thoại (Bỏ ngỏ):** Phát sự kiện qua Kafka topic `chat-control-events` sang `chat-service` để tự động chuyển trạng thái `ChatSessionStatus` sang **`CLOSED`** (Khóa cứng ô nhập liệu, chặn quyền gửi tin nhắn).\n" +
                    "3. **Bắn thông báo (Bỏ ngỏ):** Phát sự kiện qua Kafka topic `notification-events` sang `notification-service` để gửi thông báo thông tin đến cả Client và Expert.\n\n" +
                    "⚠️ **LƯU Ý DÀNH CHO FRONTEND:** Khi Admin gọi API này thành công (200 OK), hãy làm mới danh sách quản trị. Đồng thời, giao diện phía Client và Expert của phòng này sẽ tự động chuyển hoàn toàn sang **Chế độ xem lịch sử (Read-only UI)**."
    )
    public ResponseEntity<String> archiveWorkspace(@PathVariable UUID workspaceId) {
        orchestrationService.archiveWorkspace(workspaceId);
        return ResponseEntity.ok("Không gian làm việc đã được lưu trữ đóng băng thành công.");
    }
}
