package com.henrique.radiologyexams.domain.exam;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


class RadiologyExamTest {

    @Test
    void shouldCreateExamWithCodeAndRequestedStatus() {
        RadiologyExam exam = new RadiologyExam("EXAM-001");

        assertEquals("EXAM-001", exam.getCode());
        assertEquals(ExamStatus.REQUESTED, exam.getStatus());
        assertTrue(exam.getStatusHistory().isEmpty());
    }

    @Test
    void shouldChangeStatusAndRecordHistoryWhenTransitionIsAllowed() {
        RadiologyExam exam = new RadiologyExam("EXAM-001");
        Instant changedAt = Instant.parse("2026-10-08T12:30:00Z");

        exam.changeStatus(
                ExamStatus.EXAM_PERFORMED,
                changedAt,
                "technician-01"
        );

        assertEquals(ExamStatus.EXAM_PERFORMED, exam.getStatus());
        assertEquals(1, exam.getStatusHistory().size());

        ExamStatusChange change = exam.getStatusHistory().get(0);

        assertEquals(ExamStatus.REQUESTED, change.getPreviousStatus());
        assertEquals(ExamStatus.EXAM_PERFORMED, change.getNewStatus());
        assertEquals(changedAt, change.getChangedAt());
        assertEquals("technician-01", change.getChangedBy());
    }

    @Test
    void shouldNotChangeStatusOrHistoryWhenTransitionIsInvalid() {
        RadiologyExam exam = new RadiologyExam("EXAM-001");
        Instant changedAt = Instant.parse("2026-10-08T12:30:00Z");

        assertThrows(
                IllegalStateException.class,
                () -> exam.changeStatus(
                        ExamStatus.REPORT_FINALIZED,
                        changedAt,
                        "technician-01"
                )
        );

        assertEquals(ExamStatus.REQUESTED, exam.getStatus());
        assertTrue(exam.getStatusHistory().isEmpty());
    }

    @Test
    void shouldRejectBlankCode() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RadiologyExam("   ")
        );
    }

    @Test
    void shouldRejectNullCode() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RadiologyExam(null)
        );
    }
}
