package com.usedcar.web;

/** 不正なリクエストパラメータ */
public class InvalidParameterException extends RuntimeException {
    public InvalidParameterException(String message) {
        super(message);
    }
}
