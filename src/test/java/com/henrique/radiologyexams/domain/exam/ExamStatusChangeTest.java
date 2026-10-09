package com.henrique.radiologyexams.domain.exam;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class ExamStatusChangeTest {

    @Test
    void shouldCreateValidStatusChange() {
        Instant changedAt = Instant.parse("2026-10-08T00:30:00Z");

        ExamStatusChange change = new ExamStatusChange(
                ExamStatus.REQUESTED,
                ExamStatus.EXAM_PERFORMED,
                changedAt,
                "technician-01"
        );

        assertEquals(ExamStatus.REQUESTED, change.getPreviousStatus());
        assertEquals(ExamStatus.EXAM_PERFORMED, change.getNewStatus());
        assertEquals(changedAt, change.getChangedAt());
        assertEquals("technician-01", change.getChangedBy());
    }

    @Test
    void shouldRejectBlankChangedBy() {
        Instant changedAt = Instant.parse("2026-10-08T00:30:00Z");

        assertThrows(
                IllegalArgumentException.class,
                () -> new ExamStatusChange(
                        ExamStatus.REQUESTED,
                        ExamStatus.EXAM_PERFORMED,
                        changedAt,
                        " "
                )
        );

    }

}
