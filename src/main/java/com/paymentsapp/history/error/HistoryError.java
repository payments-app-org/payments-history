package com.paymentsapp.history.error;

import com.paymentsapp.platform.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum HistoryError implements ErrorCode {

    OPERATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Операция не найдена"),
    CURSOR_INVALID(HttpStatus.BAD_REQUEST, "Некорректный курсор");

    private final HttpStatus status;
    private final String template;

    HistoryError(HttpStatus status, String template) {
        this.status = status;
        this.template = template;
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String template() {
        return template;
    }
}
