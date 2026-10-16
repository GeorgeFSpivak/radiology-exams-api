package com.henrique.radiologyexams.controllers;


import com.henrique.radiologyexams.domain.exam.*;
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
                ).andExpect(status().isBadRequest());

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
                ).andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
