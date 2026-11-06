package com.usedcar.service;

/** 車台番号の重複（一意制約違反） */
public class DuplicateChassisNumberException extends RuntimeException {

    private final String chassisNumber;

    public DuplicateChassisNumberException(String chassisNumber) {
        super("車台番号 " + chassisNumber + " は既に登録されています");
        this.chassisNumber = chassisNumber;
    }

    public String chassisNumber() {
        return chassisNumber;
    }
}
