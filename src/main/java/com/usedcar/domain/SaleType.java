package com.usedcar.domain;

/** 販売区分 */
public enum SaleType {
    RETAIL("小売"),
    WHOLESALE("業販"),
    AUCTION("オークション"),
    SCRAP("廃車");

    private final String label;

    SaleType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
