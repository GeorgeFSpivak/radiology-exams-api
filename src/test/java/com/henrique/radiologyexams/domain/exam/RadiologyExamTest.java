package com.henrique.radiologyexams.domain.exam;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RadiologyExamTest {

    @Test
    void shouldCreateExamWithCodeAndRequestedStatus() {
        RadiologyExam exam = new RadiologyExam("EXAM-001");

        assertEquals("EXAM-001", exam.getCode());
        Assertions.assertEquals(ExamStatus.REQUESTED, exam.getStatus());
    }

    @Test
    void shouldChangeStatusWhenTransitionIsAllowed() {
        RadiologyExam exam = new RadiologyExam("EXAM-001");

        exam.changeStatus(ExamStatus.EXAM_PERFORMED);
        assertEquals(ExamStatus.EXAM_PERFORMED, exam.getStatus());
    }

    @Test
    void shouldNotChangeStatusWhenTransitionIsInvalid() {
        RadiologyExam exam = new RadiologyExam("EXAM-001");

        assertThrows(
                IllegalStateException.class,
                () -> exam.changeStatus(ExamStatus.REPORT_FINALIZED)
        );

        assertEquals(ExamStatus.REQUESTED, exam.getStatus());
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
