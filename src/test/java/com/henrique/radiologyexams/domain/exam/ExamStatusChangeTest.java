package com.henrique.radiologyexams.domain.exam;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;


class ExamStatusChangeTest {
    private static final Instant CREATED_AT =
            Instant.parse("2026-10-08T10:00:00Z");

    @Test
    void shouldCreateValidStatusChange() {
        RadiologyExam exam = new RadiologyExam("EXAM-001", CREATED_AT);
        Instant changedAt = Instant.parse("2026-10-08T12:30:00Z");

        ExamStatusChange change = new ExamStatusChange(
                exam,
                ExamStatus.REQUESTED,
                ExamStatus.EXAM_PERFORMED,
                changedAt,
                "technician-01"
        );

        assertSame(exam, change.getExam());
        assertEquals(ExamStatus.REQUESTED, change.getPreviousStatus());
        assertEquals(ExamStatus.EXAM_PERFORMED, change.getNewStatus());
        assertEquals(changedAt, change.getChangedAt());
        assertEquals("technician-01", change.getChangedBy());
    }

    @Test
    void shouldRejectBlankChangedBy() {
        RadiologyExam exam = new RadiologyExam("EXAM-001", CREATED_AT);
        Instant changedAt = Instant.parse("2026-10-08T12:30:00Z");

        assertThrows(
                IllegalArgumentException.class,
                () -> new ExamStatusChange(
                        exam,
                        ExamStatus.REQUESTED,
                        ExamStatus.EXAM_PERFORMED,
                        changedAt,
                        " "
                )
        );
    }

    @Test
    void shouldRejectNullExam() {
        Instant changedAt = Instant.parse("2026-10-08T12:30:00Z");

        assertThrows(
                IllegalArgumentException.class,
                () -> new ExamStatusChange(
                        null,
                        ExamStatus.REQUESTED,
                        ExamStatus.EXAM_PERFORMED,
                        changedAt,
                        "technician-01"
                )
        );
    }

    @Test
    void shouldRejectChangedByLongerThan100Characters() {
        RadiologyExam exam = new RadiologyExam("EXAM-001", CREATED_AT);
        Instant changedAt = Instant.parse("2026-10-08T12:30:00Z");

        assertThrows(
                IllegalArgumentException.class,
                () -> new ExamStatusChange(
                        exam,
                        ExamStatus.REQUESTED,
                        ExamStatus.EXAM_PERFORMED,
                        changedAt,
                        "A".repeat(101)
                )
        );
    }
}
