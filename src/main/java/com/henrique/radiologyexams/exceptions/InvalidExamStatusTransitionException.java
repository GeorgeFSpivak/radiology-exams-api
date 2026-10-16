package com.henrique.radiologyexams.exceptions;

import com.henrique.radiologyexams.domain.exam.ExamStatus;

public class InvalidExamStatusTransitionException extends RuntimeException {

    public InvalidExamStatusTransitionException(
            ExamStatus currentStatus,
            ExamStatus nextStatus) {

        super(
                "Cannot transition exam from '"
                        + currentStatus +
                        "' to '"
                        + nextStatus + "'."
        );
    }
}
