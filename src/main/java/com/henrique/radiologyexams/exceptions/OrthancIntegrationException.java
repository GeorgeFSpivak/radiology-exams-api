package com.henrique.radiologyexams.exceptions;



public class OrthancIntegrationException extends RuntimeException {

    public OrthancIntegrationException(String message) {

        super(message);
    }

    public OrthancIntegrationException(String message, Throwable cause) {

        super(message, cause);
    }
}
