package com.henrique.radiologyexams.repositories;

import com.henrique.radiologyexams.domain.exam.ExamStatus;
import com.henrique.radiologyexams.domain.exam.ExamStatusChange;
import com.henrique.radiologyexams.domain.exam.RadiologyExam;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;


@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
class RadiologyExamRepositoryTest {
    @Autowired
    private RadiologyExamRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    private static final Instant CREATED_AT =
            Instant.parse("2026-10-08T10:00:00Z");

    @Test
    void shouldSaveAndFindExamByCode() {
        RadiologyExam exam = new RadiologyExam("EXAM-DB-001", CREATED_AT);

        repository.saveAndFlush(exam);
        entityManager.clear();

        Optional<RadiologyExam> result =
                repository.findByCode("EXAM-DB-001");

        assertTrue(result.isPresent());

        RadiologyExam savedExam = result.get();

        assertNotNull(savedExam.getId());
        assertNotNull(savedExam.getVersion());
        assertEquals("EXAM-DB-001", savedExam.getCode());
        assertEquals(ExamStatus.REQUESTED, savedExam.getStatus());
        assertEquals(CREATED_AT, savedExam.getCreatedAt());
        assertTrue(savedExam.getStatusHistory().isEmpty());
    }

    @Test
    void shouldSaveAndLoadStatusHistoryInOrder() {
        RadiologyExam exam = new RadiologyExam("EXAM-DB-002", CREATED_AT);
        Instant firstChangedAt = Instant.parse("2026-10-08T12:30:00Z");
        Instant secondChangedAt = Instant.parse("2026-10-08T14:30:00Z");
        Instant receivedAt = Instant.parse("2026-10-08T11:30:00Z");

        exam.associateDicomStudy(
                "orthanc-study-001",
                "1.2.840.113619.2.55.3.604688435.123",
                receivedAt
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

        repository.saveAndFlush(exam);
        entityManager.clear();

        Optional<RadiologyExam> result =
                repository.findByCode("EXAM-DB-002");

        assertTrue(result.isPresent());

        RadiologyExam savedExam = result.get();

        List<ExamStatusChange> history =
                savedExam.getStatusHistory();

        assertEquals(ExamStatus.REPORT_IN_PROGRESS, savedExam.getStatus());
        assertEquals(2, history.size());

        ExamStatusChange firstChange = history.get(0);
        ExamStatusChange secondChange = history.get(1);

        assertNotNull(firstChange.getId());
        assertNotNull(secondChange.getId());

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
    void shouldSaveAndLoadDicomStudyReference() {
        RadiologyExam exam = new RadiologyExam("EXAM-DB-003", CREATED_AT);
        String orthancStudyId = "orthanc-study-003";
        String studyInstanceUid = "1.2.840.113619.2.55.3.604688435.123";
        Instant receivedAt = Instant.parse("2026-10-08T15:30:00Z");

        exam.associateDicomStudy(
                orthancStudyId,
                studyInstanceUid,
                receivedAt
        );

        repository.saveAndFlush(exam);
        entityManager.clear();

        RadiologyExam savedExam =
                repository.findByCode("EXAM-DB-003").orElseThrow();

        assertEquals(orthancStudyId, savedExam.getOrthancStudyId());
        assertEquals(studyInstanceUid,  savedExam.getStudyInstanceUid());
        assertEquals(receivedAt, savedExam.getDicomReceivedAt());
        assertTrue(savedExam.hasDicomStudy());
    }
}
