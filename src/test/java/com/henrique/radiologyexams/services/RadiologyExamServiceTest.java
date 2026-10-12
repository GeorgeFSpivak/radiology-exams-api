package com.henrique.radiologyexams.services;

import com.henrique.radiologyexams.domain.exam.ExamStatus;
import com.henrique.radiologyexams.domain.exam.RadiologyExam;
import com.henrique.radiologyexams.domain.exam.RadiologyExamRequestDTO;
import com.henrique.radiologyexams.domain.exam.RadiologyExamResponseDTO;
import com.henrique.radiologyexams.exceptions.ExamAlreadyExistsException;
import com.henrique.radiologyexams.repositories.RadiologyExamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

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
}
