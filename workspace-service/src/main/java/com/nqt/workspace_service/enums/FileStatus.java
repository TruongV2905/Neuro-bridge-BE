package com.nqt.workspace_service.enums;

public enum FileStatus {
    PENDING,            // Vừa nộp, chờ client review
    APPROVED,           // Client đã duyệt — không cho sửa
    REJECTED,           // Client từ chối — expert phải upload lại
    REVISION_REQUIRED,  // Yêu cầu chỉnh sửa cụ thể (kèm feedback)
    SUPERSEDED          // Bị thay thế bởi version mới
}
