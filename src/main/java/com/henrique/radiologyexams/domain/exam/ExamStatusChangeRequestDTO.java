package com.henrique.radiologyexams.domain.exam;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record ExamStatusChangeRequestDTO(
        @NotNull(message = "New status must not be null.")
        ExamStatus newStatus,
        @NotBlank(message = "Changed by must not be blank.")
        @Size(max = 100, message = "Changed by must have at most 100 characters.")
        String changedBy
) {
}
