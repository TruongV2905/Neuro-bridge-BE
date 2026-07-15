package com.nqt.rate_service.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RatingRequest {

    @NotNull
    @DecimalMin(value = "1.0", message = "Score tối thiểu 1")
    @DecimalMax(value = "5.0", message = "Score tối đa 5")
    Double score;

    String comment;
}