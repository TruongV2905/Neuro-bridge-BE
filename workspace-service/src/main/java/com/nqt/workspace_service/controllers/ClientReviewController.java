package com.nqt.workspace_service.controllers;
import com.nqt.workspace_service.dto.request.ReviewPackageRequest;
import com.nqt.workspace_service.dto.response.SubmissionPackageResponse;
import com.nqt.workspace_service.services.interfaces.IClientReviewService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/client")
@RequiredArgsConstructor
public class ClientReviewController {

    private final IClientReviewService clientReviewService;

    /**
     * API: Client nghiệm thu, chấp nhận sản phẩm và ra lệnh giải ngân
     */
    @PostMapping("/approve")
    @PreAuthorize("hasRole('CLIENT') and isAuthenticated()")
    @Operation(
            summary = "✅ CLIENT DUYỆT SẢN PHẨM & HOÀN THÀNH DỰ ÁN",
            description = "🔥 **API TRỌNG YẾU (DÒNG TIỀN):** Được gọi khi Khách hàng (Client) hài lòng với gói sản phẩm nộp lên và bấm nút **Duyệt (Approve)**.\n\n" +
                    "### 🔄 Chuỗi nghiệp vụ tự động kích hoạt ngầm:\n" +
                    "1. **Đổi trạng thái Gói nộp:** Chuyển từ `PENDING` sang `APPROVED`.\n" +
                    "2. **Hoàn thành dự án:** Trạng thái dự án chuyển sang `COMPLETED`, tiến độ tự động nhảy lên `100%`.\n" +
                    "3. **Đóng băng không gian:** Trạng thái Workspace chuyển sang `ARCHIVED` (Chỉ đọc, đóng các tính năng tương tác).\n" +
                    "4. **Kích hoạt giải ngân (Bỏ ngỏ):** Phát sự kiện qua Kafka sang `payment-service` để tự động **giải ngân tiền cọc từ ví Escrow** về tài khoản Expert.\n" +
                    "5. **Bắn thông báo (Bỏ ngỏ):** Phát tín hiệu sang `notification-service` để gửi thông báo chúc mừng Real-time cho cả Client lẫn Expert.\n\n" +
                    "⚠️ **LƯU Ý DÀNH CHO FRONTEND:** Sau khi API này trả về thành công (200 OK), hãy chuyển giao diện của Client và Expert sang màn hình **Lịch sử phòng làm việc (Đã đóng lưu trữ)** và hiển thị trạng thái hoàn thành."
    )
    public ResponseEntity<SubmissionPackageResponse> approvePackage(
            @RequestBody ReviewPackageRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID currentClientId = UUID.fromString(jwt.getClaim("sub").toString());
        SubmissionPackageResponse response = clientReviewService.approvePackage(request, currentClientId);
        return ResponseEntity.ok(response);
    }

    /**
     * API: Client từ chối sản phẩm, yêu cầu Expert làm lại bản vá khác
     */
    @PostMapping("/reject")
    @PreAuthorize("hasRole('CLIENT') and isAuthenticated()")
    @Operation(
            summary = "❌ CLIENT TỪ CHỐI SẢN PHẨM & YÊU CẦU SỬA ĐỔI",
            description = "🔄 **API ĐIỀU PHỐI LUỒNG LÀM VIỆC:** Được gọi khi Khách hàng (Client) không đồng ý với gói sản phẩm nộp lên và yêu cầu Expert phải chỉnh sửa, sửa lỗi.\n\n" +
                    "### 🛑 Điều kiện ràng buộc dữ liệu (Validation):\n" +
                    "* **Trường `feedback` không được để trống:** Frontend **bắt buộc** phải hiển thị ô nhập lý do/yêu cầu chỉnh sửa và truyền dữ liệu vào request. Nếu truyền `null` hoặc chuỗi rỗng (`\"\"`), hệ thống sẽ lập tức ném lỗi 400.\n\n" +
                    "### 🔄 Chuỗi nghiệp vụ tự động cập nhật ngầm:\n" +
                    "1. **Đổi trạng thái Gói nộp:** Chuyển sang trạng thái `REJECTED` để lưu vết bản nộp lỗi.\n" +
                    "2. **Cập nhật trạng thái Dự án:** Trạng thái tổng thể lùi về `REVISION_REQUESTED` (Yêu cầu chỉnh sửa bản vá).\n" +
                    "3. **Mở khóa Workspace:** Không gian làm việc vẫn giữ trạng thái hoạt động (Active), hệ thống ghi nhận feedback để Expert dựa vào đó sửa bài và kích hoạt luồng nộp lại gói sản phẩm mới.\n\n" +
                    "⚠️ **LƯU Ý DÀNH CHO FRONTEND:** Khi nhận kết quả trả về thành công (200 OK), hãy hiển thị thông báo gửi yêu cầu thành công, đồng thời cập nhật giao diện của Expert để hiển thị nút **'Nộp lại sản phẩm bồi hoàn'** kèm nội dung feedback của Client."
    )
    public ResponseEntity<SubmissionPackageResponse> rejectPackage(
            @RequestBody ReviewPackageRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        UUID currentClientId = UUID.fromString(jwt.getClaim("sub").toString());
        SubmissionPackageResponse response = clientReviewService.rejectPackage(request, currentClientId);
        return ResponseEntity.ok(response);
    }
}
