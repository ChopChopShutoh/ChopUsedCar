package com.usedcar.repository;

import com.usedcar.domain.Car;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Locale;

/** 一覧検索で使う Specification の部品集 */
public final class CarSpecifications {

    private CarSpecifications() {
    }

    /** 自由検索（車台番号 / 車種 / メーカー / 型式 / 仕入先 / 販売先 / 担当者 / 備考） */
    public static Specification<Car> keyword(String keyword) {
        String like = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.or(
            cb.like(cb.lower(root.get("model")), like),
            cb.like(cb.lower(root.get("manufacturer")), like),
            cb.like(cb.lower(root.get("chassisNumber")), like),
            cb.like(cb.lower(root.get("vehicleType")), like),
            cb.like(cb.lower(root.get("supplierName")), like),
            cb.like(cb.lower(root.get("buyerName")), like),
            cb.like(cb.lower(root.get("buyerStaff")), like),
            cb.like(cb.lower(root.get("sellerStaff")), like),
            cb.like(cb.lower(root.get("memo")), like)
        );
    }

    public static Specification<Car> manufacturerEquals(String manufacturer) {
        return (root, query, cb) -> cb.equal(root.get("manufacturer"), manufacturer);
    }

    public static Specification<Car> stockDateBetween(LocalDate from, LocalDate to) {
        if (from != null && to != null) {
            return (root, query, cb) -> cb.between(root.get("stockDate"), from, to);
        }
        if (from != null) {
            return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("stockDate"), from);
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("stockDate"), to);
    }

    /** 契約日 or 納車日が指定範囲内（粗利実績の集計用） */
    public static Specification<Car> contractedBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> cb.and(
            cb.isNotNull(root.get("contractDate")),
            cb.between(root.get("contractDate"), from, to)
        );
    }

    /** 車検満了日が days 日以内（車検切れ含む） */
    public static Specification<Car> inspectionDueWithin(LocalDate today, int days) {
        LocalDate limit = today.plusDays(days);
        return (root, query, cb) -> cb.and(
            cb.isNotNull(root.get("inspectionExpiry")),
            cb.lessThanOrEqualTo(root.get("inspectionExpiry"), limit),
            cb.isNull(root.get("contractDate"))
        );
    }
}
