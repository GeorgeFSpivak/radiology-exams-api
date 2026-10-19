package com.henrique.radiologyexams.services;


import com.henrique.radiologyexams.domain.exam.*;
import com.henrique.radiologyexams.repositories.RadiologyExamRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
class RadiologyExamServiceIntegrationTest {
    @Autowired
    private RadiologyExamService service;

    @Autowired
    private RadiologyExamRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void shouldPersistStatusChangeAndHistory() {
        RadiologyExam exam = new RadiologyExam(
                "EXAM-SERVICE-DB-001",
                Instant.parse("2026-10-13T01:00:00Z")
        );

        exam.associateDicomStudy(
                "orthanc-study-001",
                "1.2.840.113619.2.55.3.604688435.123",
                Instant.parse("2026-10-13T02:00:00Z")
        );

        repository.saveAndFlush(exam);
        entityManager.clear();

        ExamStatusChangeRequestDTO request = new ExamStatusChangeRequestDTO(
                ExamStatus.EXAM_PERFORMED,
                "technician-01"
        );

        ExamStatusChangeResponseDTO response = service.changeExamStatus(
                "EXAM-SERVICE-DB-001",
                request
        );

        entityManager.flush();
        entityManager.clear();

        RadiologyExam savedExam = repository.findByCode("EXAM-SERVICE-DB-001")
                .orElseThrow();

        assertEquals(ExamStatus.EXAM_PERFORMED, savedExam.getStatus());
        assertEquals(1, savedExam.getStatusHistory().size());

        List<ExamStatusChange> history = savedExam.getStatusHistory();
        ExamStatusChange savedChange = history.get(0);

        assertNotNull(savedChange.getId());
        assertEquals(ExamStatus.REQUESTED, savedChange.getPreviousStatus());
        assertEquals(ExamStatus.EXAM_PERFORMED, savedChange.getNewStatus());
        assertEquals(response.changedAt(), savedChange.getChangedAt());
        assertEquals("technician-01", savedChange.getChangedBy());
    }
}
