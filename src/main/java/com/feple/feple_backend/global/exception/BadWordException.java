package com.feple.feple_backend.global.exception;

public class BadWordException extends IllegalArgumentException {

    private final String field;

    public BadWordException(String field) {
        super("금칙어가 포함되어 있습니다.");
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
