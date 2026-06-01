package com.nqt.common_starter.util;

import com.nqt.common_starter.dto.request.PageRequest;
import lombok.NoArgsConstructor;
import org.hibernate.query.SortDirection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@NoArgsConstructor
public class PageUtil {
    public static Pageable createPageable(PageRequest request) {
        Sort sort = request.getDirection() == SortDirection.ASCENDING
                ? Sort.by(request.getSortBy()).ascending()
                : Sort.by(request.getSortBy()).descending();

        return org.springframework.data.domain.PageRequest.of(
                request.getPage(),
                request.getSize(),
                sort
        );
    }
}
