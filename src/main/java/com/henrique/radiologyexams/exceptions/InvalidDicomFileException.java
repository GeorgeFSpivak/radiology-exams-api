package com.henrique.radiologyexams.exceptions;



public class InvalidDicomFileException extends RuntimeException {

    public InvalidDicomFileException(String message, Throwable cause) {

        super(message, cause);
    }
}
