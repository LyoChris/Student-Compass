package org.backendcompas.modules.recommendations.exception;

import org.backendcompas.core.exception.ApiException;
import org.springframework.http.HttpStatus;

public class AiServiceUnavailableException extends ApiException {

    public AiServiceUnavailableException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
