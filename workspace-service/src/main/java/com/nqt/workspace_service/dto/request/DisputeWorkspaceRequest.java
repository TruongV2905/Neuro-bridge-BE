package com.nqt.workspace_service.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.UUID;

@Data
public class DisputeWorkspaceRequest {
    @NotBlank(message = "Workspace ID không được để trống!")
    private UUID workspaceId;

    // 👈 BẮT BUỘC: Không được null, không được rỗng "", không được chỉ chứa khoảng trắng "   "
    @NotBlank(message = "Bắt buộc phải nhập lý do chi tiết khi mở tranh chấp phòng làm việc!")
    @Size(min = 10, max = 1000, message = "Lý do tranh chấp phải nằm trong khoảng từ 10 đến 1000 ký tự để đảm bảo đủ thông tin phân xử!")
    private String reason;
}
