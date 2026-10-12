package com.henrique.radiologyexams.domain.exam;


import java.time.Instant;

public record RadiologyExamResponseDTO(
        String code,
        ExamStatus status,
        Instant createdAt
) {
}
