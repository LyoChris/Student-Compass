package org.backendcompas.core.exception;

import org.springframework.http.HttpStatus;

public class MutedUserException extends ApiException {
    public MutedUserException() {
        super(HttpStatus.FORBIDDEN, "Your account has been muted due to low trust score and cannot post new content.");
    }
}
