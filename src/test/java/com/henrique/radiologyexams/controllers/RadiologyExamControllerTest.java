package com.henrique.radiologyexams.controllers;


import com.henrique.radiologyexams.domain.exam.*;
import com.henrique.radiologyexams.exceptions.ExamAlreadyExistsException;
import com.henrique.radiologyexams.exceptions.ExamNotFoundException;
import com.henrique.radiologyexams.exceptions.InvalidExamStatusTransitionException;
import com.henrique.radiologyexams.services.RadiologyExamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RadiologyExamController.class)
public class RadiologyExamControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RadiologyExamService service;

    @Test
    void shouldCreateExamAndReturnCreatedStatus() throws Exception {
        Instant createdAt = Instant.parse("2026-10-15T02:00:00Z");

        RadiologyExamResponseDTO response =
                new RadiologyExamResponseDTO(
                        "EXAM-001",
                        ExamStatus.REQUESTED,
                        createdAt
                );

        when(service.createExam(any(RadiologyExamRequestDTO.class)))
                .thenReturn(response);

        mockMvc.perform(
                post("/exams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "code": "EXAM-001"
                                }
                                """)
        ).andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("EXAM-001"))
                .andExpect(jsonPath("$.status").value("REQUESTED"))
                .andExpect(jsonPath("$.createdAt")
                        .value("2026-10-15T02:00:00Z"));

        verify(service).createExam(
                new RadiologyExamRequestDTO("EXAM-001")
        );
    }

    @Test
    void shouldReturnBadRequestWhenExamCodeIsBlank() throws Exception {
        mockMvc.perform(
                        post("/exams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                    "code": " "
                                }
                                """)
                ).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail").value("Request validation failed."))
                .andExpect(jsonPath("$.errors.code")
                        .value("Exam code must not be blank."));

        verifyNoInteractions(service);
    }

    @Test
    void shouldReturnExamDetailsByCode() throws Exception {
        Instant createdAt = Instant.parse("2026-10-15T02:00:00Z");
        Instant changedAt = Instant.parse("2026-10-15T10:00:00Z");

        List<ExamStatusChangeResponseDTO> history =
                List.of(new ExamStatusChangeResponseDTO(
                        ExamStatus.REQUESTED,
                        ExamStatus.EXAM_PERFORMED,
                        changedAt,
                        "technician-01"
                ));

        RadiologyExamDetailsDTO details =
                new RadiologyExamDetailsDTO(
                        "EXAM-001",
                        ExamStatus.EXAM_PERFORMED,
                        createdAt,
                        history
                );

        when(service.getExamByCode("EXAM-001")).thenReturn(details);

        mockMvc.perform(
                get("/exams/EXAM-001")
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("EXAM-001"))
                .andExpect(jsonPath("$.status").value("EXAM_PERFORMED"))
                .andExpect(jsonPath("$.createdAt")
                        .value("2026-10-15T02:00:00Z"))
                .andExpect(jsonPath("$.statusHistory[0].previousStatus")
                        .value("REQUESTED"))
                .andExpect(jsonPath("$.statusHistory[0].newStatus")
                        .value("EXAM_PERFORMED"))
                .andExpect(jsonPath("$.statusHistory[0].changedAt")
                        .value("2026-10-15T10:00:00Z"))
                .andExpect(jsonPath("$.statusHistory[0].changedBy")
                        .value("technician-01"));

        verify(service).getExamByCode("EXAM-001");
    }

    @Test
    void shouldChangeExamStatus() throws Exception {
        Instant changedAt =  Instant.parse("2026-10-15T10:00:00Z");

        ExamStatusChangeRequestDTO request =
                new ExamStatusChangeRequestDTO(
                        ExamStatus.EXAM_PERFORMED,
                        "technician-01"
                );

        ExamStatusChangeResponseDTO response =
                new ExamStatusChangeResponseDTO(
                        ExamStatus.REQUESTED,
                        ExamStatus.EXAM_PERFORMED,
                        changedAt,
                        "technician-01"
                );

        when(service.changeExamStatus(
                "EXAM-001",
                request)
        ).thenReturn(response);

        mockMvc.perform(patch("/exams/EXAM-001/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "newStatus": "EXAM_PERFORMED",
                            "changedBy": "technician-01"
                        }
                """)
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.previousStatus")
                        .value("REQUESTED"))
                .andExpect(jsonPath("$.newStatus")
                        .value("EXAM_PERFORMED"))
                .andExpect(jsonPath("$.changedAt")
                        .value("2026-10-15T10:00:00Z"))
                .andExpect(jsonPath("$.changedBy")
                        .value("technician-01"));

        verify(service).changeExamStatus("EXAM-001", request);
    }

    @Test
    void shouldReturnBadRequestWhenNewStatusIsNull() throws Exception {

        mockMvc.perform(patch("/exams/EXAM-001/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "newStatus": null,
                            "changedBy": "technician-01"
                        }
                """)
                ).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.detail").value("Request validation failed."))
                .andExpect(jsonPath("$.errors.newStatus")
                        .value("New status must not be null."));

        verifyNoInteractions(service);
    }

    @Test
    void shouldReturnNotFoundWhenExamDoesNotExist() throws Exception {
        ExamNotFoundException ex =
                new ExamNotFoundException("UNKNOWN");

        when(service.getExamByCode("UNKNOWN")).thenThrow(ex);

        mockMvc.perform(get("/exams/UNKNOWN")
        ).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title")
                        .value("Exam not found"))
                .andExpect(jsonPath("$.detail").value(ex.getMessage()));

        verify(service).getExamByCode("UNKNOWN");
    }

    @Test
    void shouldReturnConflictWhenExamCodeAlreadyExists() throws Exception {
        RadiologyExamRequestDTO request =
                new RadiologyExamRequestDTO("EXAM-001");

        ExamAlreadyExistsException ex =
                new ExamAlreadyExistsException("EXAM-001");

        when(service.createExam(request)).thenThrow(ex);

        mockMvc.perform(post("/exams")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "code": "EXAM-001"
                        }
                        """)
        ).andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title")
                        .value("Exam already exists"))
                .andExpect(jsonPath("$.detail")
                        .value(ex.getMessage()));

        verify(service).createExam(request);
    }

    @Test
    void shouldReturnConflictWhenStatusTransitionIsInvalid() throws Exception {
        ExamStatusChangeRequestDTO request =
                new ExamStatusChangeRequestDTO(
                        ExamStatus.REPORT_FINALIZED,
                        "radiologist-01"
                );

        InvalidExamStatusTransitionException ex =
                new InvalidExamStatusTransitionException(
                        ExamStatus.REQUESTED,
                        ExamStatus.REPORT_FINALIZED
                );

        when(service.changeExamStatus("EXAM-001", request)).thenThrow(ex);

        mockMvc.perform(patch("/exams/EXAM-001/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "newStatus": "REPORT_FINALIZED",
                            "changedBy": "radiologist-01"
                        }
                        """)
        ).andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title")
                        .value("Invalid status transition"))
                .andExpect(jsonPath("$.detail")
                        .value(ex.getMessage()));

        verify(service).changeExamStatus("EXAM-001", request);
    }

    @Test
    void shouldReturnBadRequestWhenStatusDoesNotExist() throws Exception {
        mockMvc.perform(patch("/exams/EXAM-001/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "newStatus": "EXAM_FINALLY",
                            "changedBy": "technician-01"
                        }
                """)
                ).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title")
                        .value("Invalid request"))
                .andExpect(jsonPath("$.detail")
                        .value("Request body is invalid."));


        verifyNoInteractions(service);
    }

    @Test
    void shouldReturnBadRequestWhenRequestBodyIsMalformed() throws Exception {
        mockMvc.perform(post("/exams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "code": "EXAM-001"
                        """)
                ).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title")
                        .value("Invalid request"))
                .andExpect(jsonPath("$.detail")
                        .value("Request body is invalid."));

        verifyNoInteractions(service);
    }
}
