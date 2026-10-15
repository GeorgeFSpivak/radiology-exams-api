package com.henrique.radiologyexams.domain.exam;


import jakarta.validation.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExamRequestDTOValidationTest {
    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setupValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void shouldRejectBlankExamCode() {
        RadiologyExamRequestDTO request =
                new RadiologyExamRequestDTO(" ");

        Set<ConstraintViolation<RadiologyExamRequestDTO>> violations =
                validator.validate(request);

        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldAcceptValidExamRequest() {
        RadiologyExamRequestDTO request =
                new RadiologyExamRequestDTO("EXAM-001");

        Set<ConstraintViolation<RadiologyExamRequestDTO>> violations =
                validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldRejectExamCodeLongerThan50Characters() {
        RadiologyExamRequestDTO request =
                new RadiologyExamRequestDTO("A".repeat(51));

        Set<ConstraintViolation<RadiologyExamRequestDTO>> violations =
                validator.validate(request);

        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldAcceptValidStatusChangeRequest() {
        ExamStatusChangeRequestDTO change =
                new ExamStatusChangeRequestDTO(
                        ExamStatus.EXAM_PERFORMED,
                        "technician-01"
                );

        Set<ConstraintViolation<ExamStatusChangeRequestDTO>> violations =
                validator.validate(change);

        assertTrue(violations.isEmpty());
    }

    @Test
    void shouldRejectNullNewStatus() {
        ExamStatusChangeRequestDTO change =
                new ExamStatusChangeRequestDTO(
                        null,
                        "technician-01"
                );

        Set<ConstraintViolation<ExamStatusChangeRequestDTO>>  violations =
                validator.validate(change);

        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldRejectBlankChangedBy() {
        ExamStatusChangeRequestDTO change =
                new ExamStatusChangeRequestDTO(
                        ExamStatus.EXAM_PERFORMED,
                        " "
                );

        Set<ConstraintViolation<ExamStatusChangeRequestDTO>>   violations =
                validator.validate(change);

        assertFalse(violations.isEmpty());
    }

    @Test
    void shouldRejectChangedByLongerThan100Characters() {
        ExamStatusChangeRequestDTO change =
                new ExamStatusChangeRequestDTO(
                        ExamStatus.EXAM_PERFORMED,
                        "A".repeat(101)
                );

        Set<ConstraintViolation<ExamStatusChangeRequestDTO>>   violations =
                validator.validate(change);

        assertFalse(violations.isEmpty());
    }
}
