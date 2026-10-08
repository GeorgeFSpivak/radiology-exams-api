package com.henrique.radiologyexams.domain;



public class RadiologyExam {

    private final String code;
    private ExamStatus status;

    public RadiologyExam(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Exam code must not be blank.");
        }

        this.code = code;
        this.status = ExamStatus.REQUESTED;
    }

    public String getCode() {
        return code;
    }

    public ExamStatus getStatus() {
        return status;
    }

    public void changeStatus(ExamStatus nextStatus) {
        if (!status.canTransitionTo(nextStatus)) {
            throw new IllegalStateException(
                    "cannot transition exam from" + status + " to" + nextStatus);
        }

        this.status = nextStatus;
    }

}
