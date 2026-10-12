package com.henrique.radiologyexams.domain.exam;


import java.time.Instant;
import java.util.List;

public record RadiologyExamDetailsDTO(
        String code,
        ExamStatus status,
        Instant createdAt,
        List<ExamStatusChangeResponseDTO> statusHistory
) {
}
