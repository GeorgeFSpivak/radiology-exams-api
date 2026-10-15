package com.henrique.radiologyexams.domain.exam;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record RadiologyExamRequestDTO(
        @NotBlank(message = "Exam code must not be blank.")
        @Size(max = 50, message = "Exam code must have at most 50 characters.")
        String code
){
}
