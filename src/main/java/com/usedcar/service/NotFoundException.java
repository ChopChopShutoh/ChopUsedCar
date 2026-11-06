package com.usedcar.service;

/** 指定 ID のレコードが存在しない場合 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
