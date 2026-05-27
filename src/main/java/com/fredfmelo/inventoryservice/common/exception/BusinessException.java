package com.fredfmelo.inventoryservice.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;

public class BusinessException extends RuntimeException {

    @NonNull
    private final HttpStatus status;

    public BusinessException(String message){
        super(message);
        this.status = HttpStatus.UNPROCESSABLE_ENTITY;
    }

    public BusinessException(String message, @NonNull HttpStatus status) {
        super(message);
        this.status = status;
    }

    public BusinessException(String message, Throwable cause){
        super(message, cause);
        this.status = HttpStatus.UNPROCESSABLE_ENTITY;
    }

    @NonNull
    public HttpStatus getStatus() {
        return status;
    }
    

}