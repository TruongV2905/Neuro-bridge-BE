package com.nqt.workspace_service.dto.request;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateProgressRequest {
    @NotNull(message = "Project ID cannot be null")
    UUID projectId;
    @Min(value = 0, message = "Progress must be at least 0%")
    @Max(value = 100, message = "Progress cannot exceed 100%")
    Float progressPercentage;
}
