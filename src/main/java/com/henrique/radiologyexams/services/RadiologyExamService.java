package com.henrique.radiologyexams.services;

import com.henrique.radiologyexams.domain.exam.RadiologyExam;
import com.henrique.radiologyexams.domain.exam.RadiologyExamRequestDTO;
import com.henrique.radiologyexams.domain.exam.RadiologyExamResponseDTO;
import com.henrique.radiologyexams.exceptions.ExamAlreadyExistsException;
import com.henrique.radiologyexams.repositories.RadiologyExamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;


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

        Instant now = clock.instant();

        RadiologyExam newExam = new RadiologyExam(request.code(), now);
        RadiologyExam savedExam =  repository.save(newExam);

        return new RadiologyExamResponseDTO(
                savedExam.getCode(),
                savedExam.getStatus(),
                savedExam.getCreatedAt()
        );
    }
}
