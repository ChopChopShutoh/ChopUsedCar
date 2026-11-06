package com.usedcar.dto;

import com.usedcar.domain.Car;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;

/** 一覧の絞り込み用ステータス（ALL / 実ステータス / 入庫予定） */
public enum StatusFilter {
    ALL("すべて") {
        @Override
        public Specification<Car> toSpecification() {
            return (root, query, cb) -> cb.conjunction();
        }
    },
    IN_STOCK("在庫中") {
        @Override
        public Specification<Car> toSpecification() {
            return (root, query, cb) -> cb.isNull(root.get("contractDate"));
        }
    },
    CONTRACTED("契約済") {
        @Override
        public Specification<Car> toSpecification() {
            return (root, query, cb) -> cb.and(
                cb.isNotNull(root.get("contractDate")),
                cb.isNull(root.get("deliveryDate")));
        }
    },
    SOLD("納車済") {
        @Override
        public Specification<Car> toSpecification() {
            return (root, query, cb) -> cb.isNotNull(root.get("deliveryDate"));
        }
    },
    SCHEDULED("入庫予定") {
        @Override
        public Specification<Car> toSpecification() {
            return (root, query, cb) -> cb.and(
                cb.isNull(root.get("stockDate")),
                cb.isNotNull(root.get("scheduledStockDate")));
        }
    };

    private final String label;

    StatusFilter(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public abstract Specification<Car> toSpecification();

    public static StatusFilter parse(String value) {
        if (value == null || value.isBlank()) {
            return ALL;
        }
        return StatusFilter.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
