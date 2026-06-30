package com.nqt.identity_service.specification;

import com.nqt.identity_service.dto.request.user.UserFilterRequest;
import com.nqt.identity_service.entity.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserSpecification {
    public static Specification<User> filter(UserFilterRequest request) {
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            // search: fullName, email, phone, code
            if (request.getSearch() != null && !request.getSearch().isBlank()) {
                String keyword = "%" + request.getSearch().toLowerCase() + "%";

                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), keyword),
                        cb.like(cb.lower(root.get("email")), keyword),
                        cb.like(cb.lower(root.get("phone")), keyword),
                        cb.like(cb.lower(root.get("code")), keyword)
                ));
            }

            var filter = request.getFilter();

            if (filter != null) {

                if (filter.getRole() != null) {
                    predicates.add(cb.equal(root.get("role"), filter.getRole()));
                }

                if (filter.getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), filter.getStatus()));
                }

                if (filter.getGender() != null) {
                    predicates.add(cb.equal(root.get("gender"), filter.getGender()));
                }

                if (filter.getAddress() != null && !filter.getAddress().isBlank()) {
                    predicates.add(cb.like(
                            cb.lower(root.get("address")),
                            "%" + filter.getAddress().toLowerCase() + "%"
                    ));
                }

                if (filter.getCreatedFrom() != null) {
                    LocalDateTime from = filter.getCreatedFrom().atStartOfDay();
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
                }

                if (filter.getCreatedTo() != null) {
                    LocalDateTime to = filter.getCreatedTo().plusDays(1).atStartOfDay();
                    predicates.add(cb.lessThan(root.get("createdAt"), to));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
