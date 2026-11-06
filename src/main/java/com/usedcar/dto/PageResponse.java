package com.usedcar.dto;

import java.util.List;

/** 検索結果ページ */
public record PageResponse<T>(
    List<T> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {
}
