package com.henrique.radiologyexams.exceptions;


public class ExamAlreadyExistsException extends RuntimeException {

    public ExamAlreadyExistsException(String code) {


        super("Exam with code '" + code + "' already exists.");
    }
}
