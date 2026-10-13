package com.henrique.radiologyexams.services;

import com.henrique.radiologyexams.domain.exam.*;
import com.henrique.radiologyexams.exceptions.ExamAlreadyExistsException;
import com.henrique.radiologyexams.exceptions.ExamNotFoundException;
import com.henrique.radiologyexams.repositories.RadiologyExamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;


@Service
public class RadiologyExamService {
    private final RadiologyExamRepository repository;
    private final Clock clock;

    public RadiologyExamService(
            RadiologyExamRepository repository,
            Clock clock
    ) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public RadiologyExamResponseDTO createExam(
            RadiologyExamRequestDTO request
    ) {

        if (repository.existsByCode(request.code())) {
            throw new ExamAlreadyExistsException(request.code());
        }

        Instant now = currentTime();

        RadiologyExam newExam = new RadiologyExam(request.code(), now);
        RadiologyExam savedExam =  repository.save(newExam);

        return new RadiologyExamResponseDTO(
                savedExam.getCode(),
                savedExam.getStatus(),
                savedExam.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public RadiologyExamDetailsDTO getExamByCode(String code) {
        RadiologyExam exam = repository.findByCode(code)
                .orElseThrow(() -> new ExamNotFoundException(code));

        List<ExamStatusChangeResponseDTO> statusHistory =
                exam.getStatusHistory()
                        .stream()
                        .map(change -> new ExamStatusChangeResponseDTO(
                                change.getPreviousStatus(),
                                change.getNewStatus(),
                                change.getChangedAt(),
                                change.getChangedBy()
                        )).toList();

        return new RadiologyExamDetailsDTO(
                exam.getCode(),
                exam.getStatus(),
                exam.getCreatedAt(),
                statusHistory
        );
    }

    @Transactional
    public ExamStatusChangeResponseDTO changeExamStatus(
            String code,
            ExamStatusChangeRequestDTO request
    ) {
        RadiologyExam exam = repository.findByCode(code)
                .orElseThrow(() -> new ExamNotFoundException(code));

        Instant changedAt = currentTime();

        ExamStatusChange change = exam.changeStatus(
                request.newStatus(),
                changedAt,
                request.changedBy()
        );

        return new ExamStatusChangeResponseDTO(
                change.getPreviousStatus(),
                change.getNewStatus(),
                change.getChangedAt(),
                change.getChangedBy()
        );
    }

    private Instant currentTime() {

        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }
}
