package com.henrique.radiologyexams.domain.exam;


public enum ExamStatus {
    REQUESTED,
    EXAM_PERFORMED,
    REPORT_IN_PROGRESS,
    REPORT_FINALIZED,
    CANCELED;

    public boolean canTransitionTo(ExamStatus nextStatus) {
        if (nextStatus == null) {
            return false;

        }

        return switch (this) {
            case REQUESTED ->
                    nextStatus == EXAM_PERFORMED || nextStatus == CANCELED;
            case EXAM_PERFORMED ->
                    nextStatus == REPORT_IN_PROGRESS;
            case REPORT_IN_PROGRESS ->
                    nextStatus == REPORT_FINALIZED;
            case REPORT_FINALIZED, CANCELED ->
                    false;

        };
    }
}
