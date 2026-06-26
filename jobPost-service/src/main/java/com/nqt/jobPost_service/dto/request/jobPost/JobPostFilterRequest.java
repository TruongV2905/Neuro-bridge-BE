package com.nqt.jobPost_service.dto.request.jobPost;

import com.nqt.common_starter.dto.request.PageRequest;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class JobPostFilterRequest  extends PageRequest {
    SubJobPostFilterRequest filter;
}
