package com.nqt.workspace_service.dto.request;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)

public class UpdateProjectRequest {
    @NotNull(message = "Project ID cannot be null")
    UUID projectId;
    @NotBlank(message = "Project name cannot be blank")
    String projectName;
    LocalDateTime startDate;
    LocalDateTime deadline;
}
