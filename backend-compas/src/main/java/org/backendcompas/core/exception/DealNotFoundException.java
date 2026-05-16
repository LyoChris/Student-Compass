package org.backendcompas.core.exception;

import org.springframework.http.HttpStatus;

public class DealNotFoundException extends ApiException {
    public DealNotFoundException(String id) {
        super(HttpStatus.NOT_FOUND, "Radar deal not found: " + id);
    }
}
