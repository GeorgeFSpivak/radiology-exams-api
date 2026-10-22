package com.henrique.radiologyexams.services;


import com.henrique.radiologyexams.config.PostgresTestConfiguration;
import com.henrique.radiologyexams.domain.exam.*;
import com.henrique.radiologyexams.repositories.RadiologyExamRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;


@SpringBootTest
@Import(PostgresTestConfiguration.class)
class RadiologyExamServiceIntegrationTest {
    @Autowired
    private RadiologyExamService service;

    @Autowired
    private RadiologyExamRepository repository;


    @Test
    @Sql(
            statements = {
                    """
                    DELETE FROM exam_status_changes
                    WHERE exam_id IN (
                        SELECT id FROM radiology_exams
                        WHERE code = 'EXAM-SERVICE-DB-001'
                    );
                    """,
                    """
                    DELETE FROM radiology_exams
                    WHERE code = 'EXAM-SERVICE-DB-001';
                    """
            },
            executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
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

        ExamStatusChangeRequestDTO request = new ExamStatusChangeRequestDTO(
                ExamStatus.EXAM_PERFORMED,
                "technician-01"
        );

        ExamStatusChangeResponseDTO response = service.changeExamStatus(
                "EXAM-SERVICE-DB-001",
                request
        );

        RadiologyExamDetailsDTO savedExam =
                service.getExamByCode("EXAM-SERVICE-DB-001");

        assertEquals(ExamStatus.EXAM_PERFORMED, savedExam.status());
        assertEquals(1, savedExam.statusHistory().size());

        ExamStatusChangeResponseDTO savedChange =
                savedExam.statusHistory().get(0);

        assertEquals(ExamStatus.REQUESTED, savedChange.previousStatus());
        assertEquals(ExamStatus.EXAM_PERFORMED, savedChange.newStatus());
        assertEquals(response.changedAt(), savedChange.changedAt());
        assertEquals("technician-01", savedChange.changedBy());
    }
}
