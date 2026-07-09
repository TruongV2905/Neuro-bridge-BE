package com.nqt.jobPost_service.specification;


import com.nqt.jobPost_service.dto.request.jobPost.JobPostFilterRequest;
import com.nqt.jobPost_service.dto.request.jobPost.SubJobPostFilterRequest;
import com.nqt.jobPost_service.entity.JobPost;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class JobPostSpecification {
    public static Specification<JobPost> filter(JobPostFilterRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            query.distinct(true);

            String search = request.getSearch();

            if (search != null && !search.isBlank()) {
                String keyword = "%" + search.trim().toLowerCase() + "%";

                Predicate titlePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("title")),
                        keyword
                );

                Predicate descriptionPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("description")),
                        keyword
                );

                predicates.add(criteriaBuilder.or(titlePredicate, descriptionPredicate));
            }

            SubJobPostFilterRequest filter = request.getFilter();

            if (filter == null) {
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            }

            if (filter.getCreatorCode() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("creatorCode"),
                        filter.getCreatorCode()
                ));
            }

            if (filter.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("status"),
                        filter.getStatus()
                ));
            }

            if (filter.getMinBudget() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("budgetTo"),
                        filter.getMinBudget()
                ));
            }

            if (filter.getMaxBudget() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("budgetFrom"),
                        filter.getMaxBudget()
                ));
            }
            if (filter.getEstimatedDaysFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("estimatedDays"),
                        filter.getEstimatedDaysFrom()
                ));
            }
            if (filter.getEstimatedDaysTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("estimatedDays"),
                        filter.getEstimatedDaysTo()
                ));
            }
            if (filter.getRequiredSkillIds() != null && !filter.getRequiredSkillIds().isEmpty()) {
                Join<JobPost, Long> requiredSkillJoin = root.join("requiredSkillIds", JoinType.INNER);
                predicates.add(requiredSkillJoin.in(filter.getRequiredSkillIds()));
            }
            if (filter.getPreferredSkillIds() != null && !filter.getPreferredSkillIds().isEmpty()) {
                Join<JobPost, Long> preferredSkillJoin = root.join("preferredSkillIds", JoinType.INNER);
                predicates.add(preferredSkillJoin.in(filter.getPreferredSkillIds()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
