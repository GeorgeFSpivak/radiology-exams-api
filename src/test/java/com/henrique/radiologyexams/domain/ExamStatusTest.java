package com.henrique.radiologyexams.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;


class ExamStatusTest {

    @Test
    void shouldAllowTransitionFromRequestedToExamPerformed() {
        boolean transitionAllowed =
                ExamStatus.REQUESTED.canTransitionTo(ExamStatus.EXAM_PERFORMED);

        assertTrue(transitionAllowed);
    }

    @Test
    void shouldNotAllowTransitionFromRequestedToReportFinalized() {
        boolean transitionAllowed =
                ExamStatus.REQUESTED.canTransitionTo(ExamStatus.REPORT_FINALIZED);

        assertFalse(transitionAllowed);
    }

    @Test
    void shouldAllowTransitionFromRequestedToCanceled() {
        boolean transitionAllowed =
                ExamStatus.REQUESTED.canTransitionTo(ExamStatus.CANCELED);

        assertTrue(transitionAllowed);
    }

    @Test
    void shouldAllowTransitionFromExamPerformedToReportInProgress() {
        boolean transitionAllowed =
                ExamStatus.EXAM_PERFORMED.canTransitionTo(ExamStatus.REPORT_IN_PROGRESS);

        assertTrue(transitionAllowed);
    }

    @Test
    void shouldAllowTransitionFromReportInProgressToReportFinalized() {
        boolean transitionAllowed =
                ExamStatus.REPORT_IN_PROGRESS.canTransitionTo(ExamStatus.REPORT_FINALIZED);

        assertTrue(transitionAllowed);
    }

    @Test
    void shouldNotAllowTransitionFromReportFinalized() {
        boolean transitionAllowed =
                ExamStatus.REPORT_FINALIZED.canTransitionTo(ExamStatus.REQUESTED);

        assertFalse(transitionAllowed);
    }

    @Test
    void shouldNotAllowTransitionFromCanceled() {
        boolean transitionAllowed =
                ExamStatus.CANCELED.canTransitionTo(ExamStatus.REQUESTED);

        assertFalse(transitionAllowed);
    }

    @Test
    void shouldNotAllowTransitionToNull() {
        boolean transitionAllowed =
                ExamStatus.REQUESTED.canTransitionTo(null);

        assertFalse(transitionAllowed);
    }

}
