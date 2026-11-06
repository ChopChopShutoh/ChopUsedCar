package com.usedcar.domain;

/**
 * 在庫ステータス（派生値）。
 * 契約日 / 納車日の有無から導出する。永続化はしない。
 */
public enum CarStatus {
    IN_STOCK("在庫中"),
    CONTRACTED("契約済"),
    SOLD("納車済");

    private final String label;

    CarStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
