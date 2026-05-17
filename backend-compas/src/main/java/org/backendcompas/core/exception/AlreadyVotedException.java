package org.backendcompas.core.exception;

import org.springframework.http.HttpStatus;

public class AlreadyVotedException extends ApiException {
    public AlreadyVotedException() {
        super(HttpStatus.CONFLICT, "You have already voted on this deal.");
    }
}
