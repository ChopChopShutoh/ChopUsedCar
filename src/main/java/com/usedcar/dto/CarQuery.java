package com.usedcar.dto;

import com.usedcar.domain.SaleType;
import com.usedcar.domain.StockType;

import java.time.LocalDate;

/** 一覧検索の検索条件 */
public record CarQuery(
    String search,
    StatusFilter status,
    StockType stockType,
    SaleType saleType,
    String manufacturer,
    LocalDate stockDateFrom,
    LocalDate stockDateTo,
    String sortField,
    String sortDirection,
    int page,
    int size
) {
}
