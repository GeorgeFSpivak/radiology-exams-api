package com.henrique.radiologyexams.domain.exam;

import com.henrique.radiologyexams.exceptions.DicomStudyRequiredException;
import com.henrique.radiologyexams.exceptions.InvalidDicomStudyAssociationException;
import com.henrique.radiologyexams.exceptions.InvalidExamStatusTransitionException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


class RadiologyExamTest {
    private static final Instant CREATED_AT =
            Instant.parse("2026-10-08T10:00:00Z");
    private static final Instant DICOM_RECEIVED_AT =
            Instant.parse("2026-10-08T11:00:00Z");
    private static final String ORTHANC_STUDY_ID =
            "orthanc-study-001";
    private static final String STUDY_INSTANCE_UID =
            "1.2.840.113619.2.55.3.604688435.123";

    @Test
    void shouldCreateExamWithCodeRequestedStatusAndCreatedAt() {
        RadiologyExam exam = new RadiologyExam("EXAM-001", CREATED_AT);

        assertEquals("EXAM-001", exam.getCode());
        assertEquals(ExamStatus.REQUESTED, exam.getStatus());
        assertTrue(exam.getStatusHistory().isEmpty());
        assertEquals(CREATED_AT, exam.getCreatedAt());
        assertFalse(exam.hasDicomStudy());
    }

    @Test
    void shouldChangeStatusAndRecordHistoryWhenTransitionIsAllowed() {
        RadiologyExam exam = new RadiologyExam("EXAM-001", CREATED_AT);
        Instant changedAt = Instant.parse("2026-10-08T12:30:00Z");

        exam.associateDicomStudy(
                ORTHANC_STUDY_ID,
                STUDY_INSTANCE_UID,
                DICOM_RECEIVED_AT
        );

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
        RadiologyExam exam = new RadiologyExam("EXAM-001", CREATED_AT);
        Instant changedAt = Instant.parse("2026-10-08T12:30:00Z");

        assertThrows(
                InvalidExamStatusTransitionException.class,
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
                () -> new RadiologyExam("   ", CREATED_AT)
        );
    }

    @Test
    void shouldRejectNullCode() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RadiologyExam(null, CREATED_AT)
        );
    }

    @Test
    void shouldNotChangeStatusOrHistoryWhenChangedAtIsNull() {
        RadiologyExam exam = new RadiologyExam("EXAM-001", CREATED_AT);

        exam.associateDicomStudy(
                ORTHANC_STUDY_ID,
                STUDY_INSTANCE_UID,
                DICOM_RECEIVED_AT
        );

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
        RadiologyExam exam = new RadiologyExam("EXAM-001", CREATED_AT);
        Instant changedAt = Instant.parse("2026-10-08T12:30:00Z");

        exam.associateDicomStudy(
                ORTHANC_STUDY_ID,
                STUDY_INSTANCE_UID,
                DICOM_RECEIVED_AT
        );

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
        RadiologyExam exam = new RadiologyExam("EXAM-001", CREATED_AT);
        Instant firstChangedAt = Instant.parse("2026-10-08T12:30:00Z");
        Instant secondChangedAt = Instant.parse("2026-10-09T12:30:00Z");

        exam.associateDicomStudy(
                ORTHANC_STUDY_ID,
                STUDY_INSTANCE_UID,
                DICOM_RECEIVED_AT
        );

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
        RadiologyExam exam = new RadiologyExam("EXAM-001", CREATED_AT);
        Instant changedAt = Instant.parse("2026-10-08T12:30:00Z");

        exam.associateDicomStudy(
                ORTHANC_STUDY_ID,
                STUDY_INSTANCE_UID,
                DICOM_RECEIVED_AT
        );

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

    @Test
    void shouldRejectNullCreatedAt() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RadiologyExam("EXAM-001", null)
        );
    }

    @Test
    void shouldRejectCodeLongerThan50Characters() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RadiologyExam("A".repeat(51), CREATED_AT)
        );
    }

    @Test
    void shouldAssociateDicomStudy() {
        RadiologyExam exam = new RadiologyExam("EXAM-001", CREATED_AT);

        exam.associateDicomStudy(
                ORTHANC_STUDY_ID,
                STUDY_INSTANCE_UID,
                DICOM_RECEIVED_AT
        );

        assertEquals(ORTHANC_STUDY_ID, exam.getOrthancStudyId());
        assertEquals(STUDY_INSTANCE_UID, exam.getStudyInstanceUid());
        assertEquals(DICOM_RECEIVED_AT, exam.getDicomReceivedAt());
        assertTrue(exam.hasDicomStudy());
    }

    @Test
    void shouldNotAssociateDicomStudyWhenExamIsCanceled() {
        RadiologyExam exam = new RadiologyExam("EXAM-001", CREATED_AT);
        Instant changedAt = Instant.parse("2026-10-08T12:30:00Z");

        exam.changeStatus(
                ExamStatus.CANCELED,
                changedAt,
                "technician-01"
        );

        assertThrows(
                InvalidDicomStudyAssociationException.class,
                () -> exam.associateDicomStudy(
                        ORTHANC_STUDY_ID,
                        STUDY_INSTANCE_UID,
                        DICOM_RECEIVED_AT
                )
        );

        assertEquals(ExamStatus.CANCELED, exam.getStatus());
        assertNull(exam.getOrthancStudyId());
        assertNull(exam.getStudyInstanceUid());
        assertNull(exam.getDicomReceivedAt());
        assertFalse(exam.hasDicomStudy());
    }

    @Test
    void shouldRejectSecondDicomStudyAssociation() {
        RadiologyExam exam = new RadiologyExam("EXAM-001", CREATED_AT);

        exam.associateDicomStudy(
                ORTHANC_STUDY_ID,
                STUDY_INSTANCE_UID,
                DICOM_RECEIVED_AT
        );

        assertThrows(
                InvalidDicomStudyAssociationException.class,
                () -> exam.associateDicomStudy(
                        "another-orthanc-study",
                        "1.2.840.113619.2.55.3.999",
                        DICOM_RECEIVED_AT.plusSeconds(60)
                )
        );

        assertEquals(ORTHANC_STUDY_ID, exam.getOrthancStudyId());
        assertEquals(STUDY_INSTANCE_UID, exam.getStudyInstanceUid());
        assertEquals(DICOM_RECEIVED_AT, exam.getDicomReceivedAt());
    }

    @Test
    void shouldNotChangeToExamPerformedWithoutDicomStudy() {
        RadiologyExam exam = new RadiologyExam("EXAM-001", CREATED_AT);
        Instant changedAt = Instant.parse("2026-10-08T12:30:00Z");

        assertThrows(
                DicomStudyRequiredException.class,
                () -> exam.changeStatus(
                        ExamStatus.EXAM_PERFORMED,
                        changedAt,
                        "technician-01"
                )
        );

        assertEquals(ExamStatus.REQUESTED, exam.getStatus());
        assertTrue(exam.getStatusHistory().isEmpty());
        assertFalse(exam.hasDicomStudy());
    }

    @Test
    void shouldCancelRequestedExamWithoutDicomStudy() {
        RadiologyExam exam = new RadiologyExam("EXAM-001", CREATED_AT);
        Instant changedAt = Instant.parse("2026-10-08T12:30:00Z");

        exam.changeStatus(
                ExamStatus.CANCELED,
                changedAt,
                "technician-01"
        );

        assertEquals(ExamStatus.CANCELED, exam.getStatus());
        assertEquals(1, exam.getStatusHistory().size());
        assertFalse(exam.hasDicomStudy());
    }

    @Test
    void shouldRejectNullOrthancStudyId() {
        RadiologyExam exam = new RadiologyExam("EXAM-001", CREATED_AT);

        assertThrows(
                IllegalArgumentException.class,
                () -> exam.associateDicomStudy(
                        null,
                        STUDY_INSTANCE_UID,
                        DICOM_RECEIVED_AT
                )
        );

        assertFalse(exam.hasDicomStudy());
    }

    @Test
    void shouldRejectBlankOrthancStudyId() {
        RadiologyExam exam = new RadiologyExam("EXAM-001", CREATED_AT);

        assertThrows(
                IllegalArgumentException.class,
                () -> exam.associateDicomStudy(
                        " ",
                        STUDY_INSTANCE_UID,
                        DICOM_RECEIVED_AT
                )
        );

        assertFalse(exam.hasDicomStudy());
    }

    @Test
    void shouldRejectOrthancStudyIdLongerThan64Characters() {
        RadiologyExam exam = new RadiologyExam("EXAM-001", CREATED_AT);

        assertThrows(
                IllegalArgumentException.class,
                () -> exam.associateDicomStudy(
                        "A".repeat(65),
                        STUDY_INSTANCE_UID,
                        DICOM_RECEIVED_AT
                )
        );

        assertFalse(exam.hasDicomStudy());
    }

    @Test
    void shouldRejectNullStudyInstanceUid() {
        RadiologyExam exam = new RadiologyExam("EXAM-001", CREATED_AT);

        assertThrows(
                IllegalArgumentException.class,
                () -> exam.associateDicomStudy(
                        ORTHANC_STUDY_ID,
                        null,
                        DICOM_RECEIVED_AT
                )
        );

        assertFalse(exam.hasDicomStudy());
    }

    @Test
    void shouldRejectBlankStudyInstanceUid() {
        RadiologyExam exam = new RadiologyExam("EXAM-001", CREATED_AT);

        assertThrows(
                IllegalArgumentException.class,
                () -> exam.associateDicomStudy(
                        ORTHANC_STUDY_ID,
                        " ",
                        DICOM_RECEIVED_AT
                )
        );

        assertFalse(exam.hasDicomStudy());
    }

    @Test
    void shouldRejectStudyInstanceUidLongerThan64Characters() {
        RadiologyExam exam = new RadiologyExam("EXAM-001", CREATED_AT);

        assertThrows(
                IllegalArgumentException.class,
                () -> exam.associateDicomStudy(
                        ORTHANC_STUDY_ID,
                        "1.2." + "3".repeat(61),
                        DICOM_RECEIVED_AT
                )
        );

        assertFalse(exam.hasDicomStudy());
    }

    @Test
    void shouldRejectNullDicomReceivedAt() {
        RadiologyExam exam = new RadiologyExam("EXAM-001", CREATED_AT);

        assertThrows(
                IllegalArgumentException.class,
                () -> exam.associateDicomStudy(
                        ORTHANC_STUDY_ID,
                        STUDY_INSTANCE_UID,
                        null
                )
        );

        assertFalse(exam.hasDicomStudy());
    }
}
