package com.nqt.workspace_service.dto.response;
import com.nqt.workspace_service.enums.WorkspaceRole;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorkspaceMemberRespond {
    UUID userId;
    String name;
    String avatarUrl;
    WorkspaceRole role;
}
