package com.nqt.common_starter.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;
import org.hibernate.query.SortDirection;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageRequest {
    @Builder.Default
    int page = 0;

    @Builder.Default
    int size = 10;

    @Builder.Default
    String sortBy = "createdAt";

    @Builder.Default
    SortDirection direction = SortDirection.DESCENDING;

    String search;
}
