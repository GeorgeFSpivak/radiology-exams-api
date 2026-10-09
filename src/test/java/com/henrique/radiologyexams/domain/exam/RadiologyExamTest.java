package com.henrique.radiologyexams.domain.exam;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

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

    @Test
    void shouldNotChangeStatusOrHistoryWhenChangedAtIsNull() {
        RadiologyExam exam = new RadiologyExam("EXAM-001");

        assertThrows(
                IllegalArgumentException.class,
                () -> exam.changeStatus(
                        ExamStatus.EXAM_PERFORMED,
                        null,
                        "technician-01"
                )
        );


        assertEquals(ExamStatus.REQUESTED, exam.getStatus());
        assertTrue(exam.getStatusHistory().isEmpty());
    }

    @Test
    void shouldNotChangeStatusOrHistoryWhenChangedByIsBlank() {
        RadiologyExam exam = new RadiologyExam("EXAM-001");
        Instant changedAt = Instant.parse("2026-10-08T12:30:00Z");

        assertThrows(
                IllegalArgumentException.class,
                () -> exam.changeStatus(
                        ExamStatus.EXAM_PERFORMED,
                        changedAt,
                        " "
                )
        );

        assertEquals(ExamStatus.REQUESTED, exam.getStatus());
        assertTrue(exam.getStatusHistory().isEmpty());
    }

    @Test
    void shouldRecordStatusChangesInOrder() {
        RadiologyExam exam = new RadiologyExam("EXAM-001");
        Instant firstChangedAt = Instant.parse("2026-10-08T12:30:00Z");
        Instant secondChangedAt = Instant.parse("2026-10-09T12:30:00Z");

        exam.changeStatus(
                ExamStatus.EXAM_PERFORMED,
                firstChangedAt,
                "technician-01"
        );
        exam.changeStatus(
                ExamStatus.REPORT_IN_PROGRESS,
                secondChangedAt,
                "radiologist-01"
        );

        List<ExamStatusChange> history = exam.getStatusHistory();

        ExamStatusChange firstChange  = history.get(0);
        ExamStatusChange secondChange = history.get(1);

        assertEquals(ExamStatus.REPORT_IN_PROGRESS, exam.getStatus());
        assertEquals(2, history.size());
        assertEquals(ExamStatus.REQUESTED, firstChange.getPreviousStatus());
        assertEquals(ExamStatus.EXAM_PERFORMED, firstChange.getNewStatus());
        assertEquals(ExamStatus.EXAM_PERFORMED, secondChange.getPreviousStatus());
        assertEquals(ExamStatus.REPORT_IN_PROGRESS, secondChange.getNewStatus());
        assertEquals(firstChangedAt, firstChange.getChangedAt());
        assertEquals(secondChangedAt, secondChange.getChangedAt());
        assertEquals("technician-01", firstChange.getChangedBy());
        assertEquals("radiologist-01", secondChange.getChangedBy());

    }

    @Test
    void shouldNotAllowExternalChangesToStatusHistory() {
        RadiologyExam exam = new RadiologyExam("EXAM-001");
        Instant changedAt = Instant.parse("2026-10-08T12:30:00Z");

        exam.changeStatus(
                ExamStatus.EXAM_PERFORMED,
                changedAt,
                "technician-01"
        );

        List<ExamStatusChange> history = exam.getStatusHistory();

        assertThrows(
                UnsupportedOperationException.class,
                () -> history.clear()
        );

        assertEquals(1, history.size());
    }
}
