package com.fredfmelo.inventoryservice.common.exception;

import org.springframework.http.HttpStatus;

import io.micrometer.common.lang.NonNull;

public class TechnicalException extends RuntimeException {

    @NonNull
    private final HttpStatus status;

    public TechnicalException(String message){
        super(message);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public TechnicalException(String message, @NonNull HttpStatus status) {
        super(message);
        this.status = status;
    }

    public TechnicalException(String message, Throwable cause){
        super(message, cause);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    @NonNull
    public HttpStatus getStatus() {
        return status;
    }
}