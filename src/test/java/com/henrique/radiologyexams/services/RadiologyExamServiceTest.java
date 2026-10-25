package com.henrique.radiologyexams.services;

import com.henrique.radiologyexams.domain.exam.*;
import com.henrique.radiologyexams.exceptions.ExamAlreadyExistsException;
import com.henrique.radiologyexams.exceptions.ExamNotFoundException;
import com.henrique.radiologyexams.repositories.RadiologyExamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class RadiologyExamServiceTest {
    private static final Instant FIXED_INSTANT =
            Instant.parse("2026-10-12T10:00:00Z");

    private static final Clock FIXED_CLOCK =
            Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    @Mock
    private RadiologyExamRepository repository;

    private RadiologyExamService service;

    @BeforeEach
    void setup() {

        service = new RadiologyExamService(repository, FIXED_CLOCK);
    }

    @Test
    void shouldCreateExamWithRequestedStatusAndFixedCreatedAt() {
        when(repository.existsByCode("EXAM-001")).thenReturn(false);
        when(repository.save(any(RadiologyExam.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RadiologyExamRequestDTO request = new RadiologyExamRequestDTO("EXAM-001");
        RadiologyExamResponseDTO response = service.createExam(request);

        assertEquals("EXAM-001", response.code());
        assertEquals(ExamStatus.REQUESTED, response.status());
        assertEquals(FIXED_INSTANT, response.createdAt());

        verify(repository).existsByCode("EXAM-001");
        verify(repository).save(any(RadiologyExam.class));
    }

    @Test
    void shouldNotCreateExamWhenCodeAlreadyExists() {
        when(repository.existsByCode("EXAM-001")).thenReturn(true);

        RadiologyExamRequestDTO request = new RadiologyExamRequestDTO("EXAM-001");

        assertThrows(
                ExamAlreadyExistsException.class,
                () -> service.createExam(request)
        );

        verify(repository, never()).save(any(RadiologyExam.class));
    }

    @Test
    void shouldReturnExamDetailsWithStatusHistory() {
        RadiologyExam exam = new RadiologyExam("EXAM-001", FIXED_INSTANT);
        Instant changedAt = Instant.parse("2026-10-12T12:00:00Z");

        exam.associateDicomStudy(
                "orthanc-study-001",
                "1.2.840.113619.2.55.3.604688435.123",
                Instant.parse("2026-10-12T11:00:00Z")
        );

        exam.changeStatus(
                ExamStatus.EXAM_PERFORMED,
                changedAt,
                "technician-01"
        );

        when(repository.findByCode("EXAM-001")).thenReturn(Optional.of(exam));

        RadiologyExamDetailsDTO details = service.getExamByCode("EXAM-001");

        assertEquals("EXAM-001",  details.code());
        assertEquals(ExamStatus.EXAM_PERFORMED, details.status());
        assertEquals(FIXED_INSTANT, details.createdAt());
        assertEquals(1, details.statusHistory().size());

        ExamStatusChangeResponseDTO firstChange =
                details.statusHistory().get(0);

        assertEquals(ExamStatus.REQUESTED, firstChange.previousStatus());
        assertEquals(ExamStatus.EXAM_PERFORMED, firstChange.newStatus());
        assertEquals(changedAt, firstChange.changedAt());
        assertEquals("technician-01", firstChange.changedBy());
    }

    @Test
    void shouldThrowWhenExamCodeDoesNotExist() {
        when(repository.findByCode("UNKNOWN"))
                .thenReturn(Optional.empty());

        assertThrows(
                ExamNotFoundException.class,
                () -> service.getExamByCode("UNKNOWN")
        );

        verify(repository).findByCode("UNKNOWN");
    }

    @Test
    void shouldChangeExamStatusAndReturnChangeDetails() {
        Instant createdAt = Instant.parse("2026-10-12T08:00:00Z");
        RadiologyExam exam = new RadiologyExam("EXAM-001", createdAt);

        exam.associateDicomStudy(
                "orthanc-study-001",
                "1.2.840.113619.2.55.3.604688435.123",
                Instant.parse("2026-10-12T09:00:00Z")
        );

        when(repository.findByCode("EXAM-001"))
                .thenReturn(Optional.of(exam));

        ExamStatusChangeRequestDTO request =
                new  ExamStatusChangeRequestDTO(
                        ExamStatus.EXAM_PERFORMED,
                        "technician-01"
                );

        ExamStatusChangeResponseDTO response =
                service.changeExamStatus("EXAM-001", request);

        assertEquals(ExamStatus.REQUESTED, response.previousStatus());
        assertEquals(ExamStatus.EXAM_PERFORMED, response.newStatus());
        assertEquals(FIXED_INSTANT, response.changedAt());
        assertEquals("technician-01", response.changedBy());
        assertEquals(ExamStatus.EXAM_PERFORMED, exam.getStatus());
        assertEquals(1, exam.getStatusHistory().size());
        verify(repository).findByCode("EXAM-001");
    }

    @Test
    void shouldNotChangeStatusWhenExamCodeDoesNotExist() {
        when(repository.findByCode("UNKNOWN"))
                .thenReturn(Optional.empty());

        ExamStatusChangeRequestDTO request =
                new ExamStatusChangeRequestDTO(
                        ExamStatus.EXAM_PERFORMED,
                        "technician-01"
                );

        assertThrows(
                ExamNotFoundException.class,
                () -> service.changeExamStatus("UNKNOWN", request)
        );

        verify(repository).findByCode("UNKNOWN");
    }

    @Test
    void shouldAssociateDicomStudyAndReturnDetails() {
        Instant createdAt = Instant.parse("2026-10-12T08:00:00Z");
        RadiologyExam exam = new RadiologyExam("EXAM-001", createdAt);

        when(repository.findByCode("EXAM-001"))
                .thenReturn(Optional.of(exam));

        String orthancStudyId = "orthanc-study-001";
        String studyInstanceUid = "1.2.840.113619.2.55.3.604688435.123";

        DicomStudyAssociationResponseDTO response = service.associateDicomStudy(
                "EXAM-001",
                orthancStudyId,
                studyInstanceUid
        );

        assertEquals(orthancStudyId, response.orthancStudyId());
        assertEquals(studyInstanceUid, response.studyInstanceUid());
        assertEquals(FIXED_INSTANT, response.dicomReceivedAt());
        assertEquals(orthancStudyId, exam.getOrthancStudyId());
        assertEquals(studyInstanceUid, exam.getStudyInstanceUid());
        assertEquals(FIXED_INSTANT, exam.getDicomReceivedAt());
        assertEquals(ExamStatus.REQUESTED, exam.getStatus());
        verify(repository).findByCode("EXAM-001");
    }

    @Test
    void shouldNotAssociateDicomStudyWhenExamDoesNotExist() {
        when(repository.findByCode("UNKNOWN"))
                .thenReturn(Optional.empty());

        assertThrows(
                ExamNotFoundException.class,
                () -> service.associateDicomStudy(
                        "UNKNOWN",
                        "orthanc-study-001",
                        "1.2.840.113619.2.55.3.604688435.123"
                )
        );

        verify(repository).findByCode("UNKNOWN");
    }
}
