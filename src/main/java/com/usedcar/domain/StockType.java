package com.usedcar.domain;

/** 入庫区分 */
public enum StockType {
    TRADE_IN("下取り"),
    BUY_OUT("買取"),
    AUCTION("オークション");

    private final String label;

    StockType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
