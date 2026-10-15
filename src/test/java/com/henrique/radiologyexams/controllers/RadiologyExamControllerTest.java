package com.henrique.radiologyexams.controllers;


import com.henrique.radiologyexams.domain.exam.ExamStatus;
import com.henrique.radiologyexams.domain.exam.RadiologyExamRequestDTO;
import com.henrique.radiologyexams.domain.exam.RadiologyExamResponseDTO;
import com.henrique.radiologyexams.services.RadiologyExamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
