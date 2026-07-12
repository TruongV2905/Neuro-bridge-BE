package com.nqt.workspace_service.dto.request;
import com.nqt.workspace_service.constant.ProjectState;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateStateRequest {
    @NotNull(message = "Project ID cannot be null")
    UUID projectId;
    @NotNull(message = "Status cannot be null")
    ProjectState status;
}
