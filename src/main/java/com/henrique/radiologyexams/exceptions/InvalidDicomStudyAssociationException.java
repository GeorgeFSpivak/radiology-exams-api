package com.henrique.radiologyexams.exceptions;


public class InvalidDicomStudyAssociationException extends RuntimeException {

    public InvalidDicomStudyAssociationException(String message) {

        super(message);
    }
}
