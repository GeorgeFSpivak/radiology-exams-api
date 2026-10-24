package com.henrique.radiologyexams.exceptions;



public class OrthancCommunicationException extends RuntimeException {

    public OrthancCommunicationException(String message, Throwable cause) {

        super(message, cause);
    }
}
