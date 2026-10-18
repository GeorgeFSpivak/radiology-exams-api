package com.henrique.radiologyexams.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;


@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ExamNotFoundException.class)
    public ProblemDetail handleExamNotFound(
            ExamNotFoundException ex
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );

        problem.setTitle("Exam not found");

        return problem;
    }

    @ExceptionHandler(ExamAlreadyExistsException.class)
    public ProblemDetail handleExamAlreadyExists(
            ExamAlreadyExistsException ex
    ){
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );

        problem.setTitle("Exam already exists");

        return problem;
    }

    @ExceptionHandler(InvalidExamStatusTransitionException.class)
    public ProblemDetail handleInvalidExamStatusTransition(
            InvalidExamStatusTransitionException ex
    ){
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                ex.getMessage()
        );

        problem.setTitle("Invalid status transition");

        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(
            MethodArgumentNotValidException ex
    ){
        Map<String, String> errors = new LinkedHashMap<>();

        for (FieldError fielderror : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(
                    fielderror.getField(),
                    fielderror.getDefaultMessage()
            );
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request validation failed."
        );

        problem.setTitle("Invalid request");
        problem.setProperty("errors", errors);

        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableRequest(
            HttpMessageNotReadableException ex
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request body is invalid."
        );

        problem.setTitle("Invalid request");

        return problem;
    }
}
