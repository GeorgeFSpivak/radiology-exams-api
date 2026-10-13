package com.henrique.radiologyexams.exceptions;


public class ExamNotFoundException extends RuntimeException {

    public ExamNotFoundException(String code) {
        super("Exam with code '" + code + "' was not found.");
    }
}
