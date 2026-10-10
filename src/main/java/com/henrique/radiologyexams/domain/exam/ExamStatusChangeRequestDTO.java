package com.henrique.radiologyexams.domain.exam;



public record ExamStatusChangeRequestDTO(
        ExamStatus newStatus,
        String changedBy
) {
}
