package com.nqt.chat_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParticipantDetail {
    /** User code, ví dụ "CLIENT001" hoặc "EXPERT001". */
    String code;

    /** Họ tên hiển thị. */
    String fullName;

    /** URL avatar. */
    String avatarUrl;

    /** Role gốc từ identity: "CLIENT" / "EXPERT" / "ADMIN". */
    String role;
}
