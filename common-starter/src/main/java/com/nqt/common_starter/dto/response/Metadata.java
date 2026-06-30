package com.nqt.common_starter.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Metadata {
    int page;
    int size;
    int currentElements;
    long totalElements;
    int totalPages;
    boolean hasNext;
    boolean hasPrevious;
}
