package org.backendcompas.modules.recommendations.exception;

import org.backendcompas.core.exception.ApiException;
import org.springframework.http.HttpStatus;

public class AiServiceTimeoutException extends ApiException {

    public AiServiceTimeoutException(String message) {
        super(HttpStatus.GATEWAY_TIMEOUT, message);
    }
}
