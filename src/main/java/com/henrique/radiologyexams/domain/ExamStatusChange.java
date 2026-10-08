package com.henrique.radiologyexams.domain;

import java.time.Instant;


public class ExamStatusChange {

    private final ExamStatus previousStatus;
    private final ExamStatus newStatus;
    private final Instant changedAt;
    private final String changedBy;

    public ExamStatusChange(
            ExamStatus previousStatus,
            ExamStatus newStatus,
            Instant changedAt,
            String changedBy
    ) {
        if (previousStatus == null) {
            throw new IllegalArgumentException("Previous status must not be null.");
        }

        if (newStatus == null) {
            throw new IllegalArgumentException("New status must not be null.");
        }

        if (changedAt == null) {
            throw new IllegalArgumentException("Change date must not be null.");
        }

        if (changedBy == null || changedBy.isBlank()) {
            throw new IllegalArgumentException("Changed by must not be blank.");
        }

        if (!previousStatus.canTransitionTo(newStatus)) {
            throw new IllegalArgumentException(
                    "Invalid status transition from " +
                    previousStatus + "to" + newStatus);
        }

        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.changedAt = changedAt;
        this.changedBy = changedBy;
    }

    public ExamStatus getPreviousStatus() {
        return previousStatus;
    }

    public ExamStatus getNewStatus() {
        return newStatus;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public String getChangedBy() {
        return changedBy;
    }

}
