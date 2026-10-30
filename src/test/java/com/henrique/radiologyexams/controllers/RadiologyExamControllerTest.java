package com.henrique.radiologyexams.controllers;


import com.henrique.radiologyexams.domain.exam.*;
import com.henrique.radiologyexams.exceptions.*;
import com.henrique.radiologyexams.services.DicomUploadService;
import com.henrique.radiologyexams.services.RadiologyExamService;
import jakarta.servlet.ServletException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @MockitoBean
    private DicomUploadService uploadService;

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

    @Test
    void shouldReturnConflictWhenDicomStudyIsRequired() throws Exception {
        ExamStatusChangeRequestDTO request =
                new ExamStatusChangeRequestDTO(
                        ExamStatus.EXAM_PERFORMED,
                        "technician-01"
                );

        DicomStudyRequiredException ex =
                new DicomStudyRequiredException();

        when(service.changeExamStatus("EXAM-001", request)).thenThrow(ex);

        mockMvc.perform(patch("/exams/EXAM-001/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "newStatus": "EXAM_PERFORMED",
                            "changedBy": "technician-01"
                        }
                        """)
                ).andExpect(status().isConflict())
                .andExpect(jsonPath("$.title")
                        .value("Dicom study required"))
                .andExpect(jsonPath("$.detail")
                        .value(ex.getMessage()));

        verify(service).changeExamStatus("EXAM-001", request);
    }

    @Test
    void shouldReturnConflictWhenOptimisticLockingFails() throws Exception {
        ExamStatusChangeRequestDTO request =
                new ExamStatusChangeRequestDTO(
                        ExamStatus.EXAM_PERFORMED,
                        "technician-01"
                );

        OptimisticLockingFailureException ex =
                new OptimisticLockingFailureException("Concurrent update");

        when(service.changeExamStatus("EXAM-001", request)).thenThrow(ex);

        mockMvc.perform(patch("/exams/EXAM-001/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "newStatus": "EXAM_PERFORMED",
                            "changedBy": "technician-01"
                        }
                        """)
                ).andExpect(status().isConflict())
                .andExpect(jsonPath("$.title")
                        .value("Exam update conflict"))
                .andExpect(jsonPath("$.detail")
                        .value("Exam was updated by another request. Reload it and try again."));

        verify(service).changeExamStatus("EXAM-001", request);
    }

    @Test
    void shouldReturnConflictWhenExamCodeUniqueConstraintIsViolated() throws Exception {
        RadiologyExamRequestDTO request = new RadiologyExamRequestDTO("EXAM-001");

        ConstraintViolationException cause = new ConstraintViolationException(
                "Duplicate exam code",
                new SQLException("Duplicate exam code", "23505"),
                "radiology_exams_code_key"
        );
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("Could not save exam", cause);

        when(service.createExam(request)).thenThrow(ex);

        mockMvc.perform(post("/exams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "code": "EXAM-001"
                        }
                        """)
                ).andExpect(status().isConflict())
                .andExpect(jsonPath("$.title")
                        .value("Exam already exists"))
                .andExpect(jsonPath("$.detail")
                        .value("An exam with this code already exists."));

        verify(service).createExam(request);
    }

    @Test
    void shouldNotTreatOtherConstraintsAsDuplicateExamCode() throws Exception {
        RadiologyExamRequestDTO request = new RadiologyExamRequestDTO("EXAM-001");

        ConstraintViolationException cause = new ConstraintViolationException(
                "Duplicate exam code",
                new SQLException("Duplicate exam code", "23505"),
                "another_constraint"
        );
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("Could not save exam", cause);

        when(service.createExam(request)).thenThrow(ex);

        ServletException failure = assertThrows(ServletException.class, () ->
                mockMvc.perform(post("/exams")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                        {
                            "code": "EXAM-001"
                        }
                        """)
                )
        );

        assertSame(ex, failure.getCause());
    }

    @Test
    void shouldUploadDicomFilesAndReturnAssociation()  throws Exception {
        Instant receivedAt = Instant.parse("2026-10-20T10:00:00Z");

        MockMultipartFile firstFile = new MockMultipartFile(
                "files",
                "first.dcm",
                "application/dicom",
                new byte[]{1, 2, 3}
        );

        MockMultipartFile secondFile = new MockMultipartFile(
                "files",
                "second.dcm",
                "application/dicom",
                new byte[]{4, 5, 6}
        );

        DicomStudyAssociationResponseDTO response = new DicomStudyAssociationResponseDTO(
                "study-001",
                "1.2.3",
                receivedAt
        );

        when(uploadService.uploadDicomFiles(
                "EXAM-001", List.of(firstFile, secondFile))).thenReturn(response);

        mockMvc.perform(
                multipart("/exams/{code}/dicom", "EXAM-001")
                        .file(firstFile)
                        .file(secondFile)
        ).andExpect(status().isOk())
                .andExpect(jsonPath("$.orthancStudyId").value("study-001"))
                .andExpect(jsonPath("$.studyInstanceUid").value("1.2.3"))
                .andExpect(jsonPath("$.dicomReceivedAt").value("2026-10-20T10:00:00Z"));

        verify(uploadService).uploadDicomFiles("EXAM-001", List.of(firstFile, secondFile));
    }

    @Test
    void shouldReturnBadRequestWhenDicomFileIsInvalid()  throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "image.dcm",
                "application/dicom",
                new byte[]{1, 2, 3}
        );

        InvalidDicomFileException ex = new InvalidDicomFileException(
                "Dicom files cannot be empty."
        );

        when(uploadService.uploadDicomFiles(
                "EXAM-001", List.of(file))).thenThrow(ex);

        mockMvc.perform(
                        multipart("/exams/EXAM-001/dicom").file(file)
                ).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Invalid dicom file"))
                .andExpect(jsonPath("$.detail").value(ex.getMessage()));

        verify(uploadService).uploadDicomFiles("EXAM-001", List.of(file));
    }

    @Test
    void shouldReturnConflictWhenDicomStudyAssociationIsInvalid()  throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "image.dcm",
                "application/dicom",
                new byte[]{1, 2, 3}
        );

        InvalidDicomStudyAssociationException ex = new InvalidDicomStudyAssociationException(
                "A dicom study is already associated with this exam."
        );

        when(uploadService.uploadDicomFiles(
                "EXAM-001", List.of(file))).thenThrow(ex);

        mockMvc.perform(
                        multipart("/exams/EXAM-001/dicom").file(file)
                ).andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Invalid dicom study association"))
                .andExpect(jsonPath("$.detail").value(ex.getMessage()));

        verify(uploadService).uploadDicomFiles("EXAM-001", List.of(file));
    }

    @Test
    void shouldReturnServiceUnavailableWhenOrthancCommunicationFails()  throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "image.dcm",
                "application/dicom",
                new byte[]{1, 2, 3}
        );

        OrthancCommunicationException ex = new OrthancCommunicationException(
                "Connection failed at internal orthanc address.",
                new IOException("Connection refused")
        );

        when(uploadService.uploadDicomFiles(
                "EXAM-001", List.of(file))).thenThrow(ex);

        mockMvc.perform(
                        multipart("/exams/EXAM-001/dicom").file(file)
                ).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.title").value("Image service unavailable"))
                .andExpect(jsonPath("$.detail").value("Could not communicate with the image service."));

        verify(uploadService).uploadDicomFiles("EXAM-001", List.of(file));
    }

    @Test
    void shouldReturnBadGatewayWhenOrthancIntegrationFails()  throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "image.dcm",
                "application/dicom",
                new byte[]{1, 2, 3}
        );

        OrthancIntegrationException ex = new OrthancIntegrationException(
                "Orthanc returned a study response without a StudyInstanceUid."
        );

        when(uploadService.uploadDicomFiles(
                "EXAM-001", List.of(file))).thenThrow(ex);

        mockMvc.perform(
                        multipart("/exams/EXAM-001/dicom").file(file)
                ).andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.title").value("Image service integration failure"))
                .andExpect(jsonPath("$.detail").value(
                        "Could not complete the operation with the image service.")
                );

        verify(uploadService).uploadDicomFiles("EXAM-001", List.of(file));
    }

    @Test
    void shouldReturn413WhenUploadSizeIsExceeded()  throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "image.dcm",
                "application/dicom",
                new byte[]{1, 2, 3}
        );

        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(25L * 1024 * 1024);

        when(uploadService.uploadDicomFiles(
                "EXAM-001", List.of(file))).thenThrow(ex);

        mockMvc.perform(
                        multipart("/exams/EXAM-001/dicom").file(file)
                ).andExpect(status().is(413))
                .andExpect(jsonPath("$.status").value(413))
                .andExpect(jsonPath("$.title").value("Upload too large"))
                .andExpect(jsonPath("$.detail").value(
                        "The upload exceeds the allowed file or request size.")
                );

        verify(uploadService).uploadDicomFiles("EXAM-001", List.of(file));
    }
}
