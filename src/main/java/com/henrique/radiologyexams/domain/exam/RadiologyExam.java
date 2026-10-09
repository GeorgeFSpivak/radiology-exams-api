package com.henrique.radiologyexams.domain.exam;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class RadiologyExam {

    private final String code;
    private ExamStatus status;
    private final List<ExamStatusChange> statusHistory = new ArrayList<>();

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

    public List<ExamStatusChange> getStatusHistory() {

        return List.copyOf(statusHistory);
    }

    public void changeStatus(
            ExamStatus nextStatus,
            Instant changedAt,
            String changedBy
    ) {
        if (!status.canTransitionTo(nextStatus)) {
            throw new IllegalStateException(
                    "cannot transition exam from " + status + " to " + nextStatus + ".");
        }

        ExamStatusChange change = new ExamStatusChange(
                status,
                nextStatus,
                changedAt,
                changedBy
        );

        this.status = nextStatus;
        this.statusHistory.add(change);

    }
}
