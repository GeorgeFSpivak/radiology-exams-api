package com.henrique.radiologyexams.domain.exam;


import java.util.List;

public record RadiologyExamDetailsDTO(
        String code,
        ExamStatus status,
        List<ExamStatusChangeResponseDTO> statusHistory
) {
}
