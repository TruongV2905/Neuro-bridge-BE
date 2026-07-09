package com.nqt.payment_service.specification;

import com.nqt.payment_service.dto.request.PaymentTransactionFilterRequest;
import com.nqt.payment_service.dto.request.SubPaymentTransactionFilterRequest;
import com.nqt.payment_service.entity.PaymentTransaction;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class PaymentTransactionSpecification {

    private PaymentTransactionSpecification() {}

    public static Specification<PaymentTransaction> filter(PaymentTransactionFilterRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            SubPaymentTransactionFilterRequest filter = request.getFilter();

            if (filter == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            if (filter.getFromUserCode() != null && !filter.getFromUserCode().isBlank()) {
                predicates.add(cb.equal(root.get("fromUserCode"), filter.getFromUserCode()));
            }

            if (filter.getToUserCode() != null && !filter.getToUserCode().isBlank()) {
                predicates.add(cb.equal(root.get("toUserCode"), filter.getToUserCode()));
            }

            if (filter.getInvolvingUserCode() != null && !filter.getInvolvingUserCode().isBlank()) {
                String code = filter.getInvolvingUserCode();
                predicates.add(cb.or(
                        cb.equal(root.get("fromUserCode"), code),
                        cb.equal(root.get("toUserCode"), code)
                ));
            }

            if (filter.getType() != null) {
                predicates.add(cb.equal(root.get("type"), filter.getType()));
            }

            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getReferenceId() != null && !filter.getReferenceId().isBlank()) {
                predicates.add(cb.equal(root.get("referenceId"), filter.getReferenceId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}