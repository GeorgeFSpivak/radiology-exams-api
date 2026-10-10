package com.henrique.radiologyexams.domain.exam;


import java.time.Instant;

public record ExamStatusChangeResponseDTO(
        ExamStatus previousStatus,
        ExamStatus newStatus,
        Instant changedAt,
        String changedBy
) {
}
