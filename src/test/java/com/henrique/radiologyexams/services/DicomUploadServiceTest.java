package com.henrique.radiologyexams.services;


import com.henrique.radiologyexams.domain.exam.DicomStudyAssociationResponseDTO;
import com.henrique.radiologyexams.domain.exam.ExamStatus;
import com.henrique.radiologyexams.domain.exam.RadiologyExam;
import com.henrique.radiologyexams.exceptions.ExamNotFoundException;
import com.henrique.radiologyexams.exceptions.InvalidDicomFileException;
import com.henrique.radiologyexams.exceptions.InvalidDicomStudyAssociationException;
import com.henrique.radiologyexams.exceptions.OrthancCommunicationException;
import com.henrique.radiologyexams.integrations.orthanc.OrthancClient;
import com.henrique.radiologyexams.integrations.orthanc.OrthancStudyResponseDTO;
import com.henrique.radiologyexams.integrations.orthanc.OrthancUploadResponseDTO;
import com.henrique.radiologyexams.repositories.RadiologyExamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class DicomUploadServiceTest {
    @Mock
    private RadiologyExamRepository repository;
    @Mock
    private OrthancClient client;
    @Mock
    private RadiologyExamService examService;
    private DicomUploadService service;

    @BeforeEach
    void setup() {
        service = new DicomUploadService(repository, client, examService);
    }

    @Test
    void shouldUploadFileAndAssociateStudy() {
        Instant now = Instant.parse("2020-04-20T00:00:00.00Z");
        RadiologyExam exam = new RadiologyExam("EXAM-001", now);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "image.dcm",
                "application/dicom",
                new byte[]{1,2,3}
        );

        OrthancUploadResponseDTO uploaded = new OrthancUploadResponseDTO(
                "instance-001",
                "study-001",
                "Success"
        );

        OrthancStudyResponseDTO study = new OrthancStudyResponseDTO(
                new OrthancStudyResponseDTO.MainDicomTags("1.2.3")
        );

        DicomStudyAssociationResponseDTO expected = new DicomStudyAssociationResponseDTO(
                "study-001",
                "1.2.3",
                now
        );

        when(repository.findByCode("EXAM-001")).thenReturn(Optional.of(exam));
        when(client.uploadInstance(any(Resource.class))).thenReturn(uploaded);
        when(client.getStudy("study-001")).thenReturn(study);
        when(examService.associateDicomStudy(
                "EXAM-001",
                "study-001",
                "1.2.3")).thenReturn(expected);

        DicomStudyAssociationResponseDTO response =
                service.uploadDicomFiles("EXAM-001", List.of(file));

        assertSame(expected, response);

        verify(client).uploadInstance(any(Resource.class));
        verify(client).getStudy("study-001");
        verify(examService).associateDicomStudy(
                "EXAM-001",
                "study-001",
                "1.2.3"
        );
    }

    @Test
    void shouldUploadTwoFilesFromSameStudyAndAssociateOnce() throws IOException {
        Instant now = Instant.parse("2020-04-20T00:00:00.00Z");
        RadiologyExam exam = new RadiologyExam("EXAM-001", now);

        MockMultipartFile firstFile = new MockMultipartFile(
                "files",
                "first.dcm",
                "application/dicom",
                new byte[]{1,2,3}
        );

        MockMultipartFile secondFile = new MockMultipartFile(
                "files",
                "second.dcm",
                "application/dicom",
                new byte[]{4,5,6}
        );

        OrthancUploadResponseDTO firstUploaded = new OrthancUploadResponseDTO(
                "instance-001",
                "study-001",
                "Success"
        );

        OrthancUploadResponseDTO secondUploaded = new OrthancUploadResponseDTO(
                "instance-002",
                "study-001",
                "Success"
        );

        when(client.uploadInstance(any(Resource.class)))
                .thenReturn(firstUploaded, secondUploaded);

        OrthancStudyResponseDTO study = new OrthancStudyResponseDTO(
                new OrthancStudyResponseDTO.MainDicomTags("1.2.3")
        );

        DicomStudyAssociationResponseDTO expected = new DicomStudyAssociationResponseDTO(
                "study-001",
                "1.2.3",
                now
        );

        when(repository.findByCode("EXAM-001")).thenReturn(Optional.of(exam));
        when(client.getStudy("study-001")).thenReturn(study);
        when(examService.associateDicomStudy(
                "EXAM-001",
                "study-001",
                "1.2.3")).thenReturn(expected);

        DicomStudyAssociationResponseDTO response =
                service.uploadDicomFiles("EXAM-001", List.of(firstFile, secondFile));

        assertSame(expected, response);

        ArgumentCaptor<Resource> resources = ArgumentCaptor.forClass(Resource.class);

        verify(client, times(2)).uploadInstance(resources.capture());

        List<Resource> uploadedResources = resources.getAllValues();

        try (var firstStream = uploadedResources.get(0).getInputStream();
             var secondStream = uploadedResources.get(1).getInputStream()) {
            assertArrayEquals(new byte[]{1, 2, 3}, firstStream.readAllBytes());
            assertArrayEquals(new byte[]{4, 5, 6}, secondStream.readAllBytes());
        }

        verify(client).getStudy("study-001");
        verify(examService).associateDicomStudy(
                "EXAM-001",
                "study-001",
                "1.2.3"
        );
    }

    @Test
    void shouldRejectNullFileList() {
        assertThrows(
                InvalidDicomFileException.class,
                () -> service.uploadDicomFiles("EXAM-001", null)
        );

        verifyNoInteractions(repository, client, examService);
    }

    @Test
    void shouldRejectEmptyFileList() {
        assertThrows(
                InvalidDicomFileException.class,
                () -> service.uploadDicomFiles("EXAM-001", List.of())
        );

        verifyNoInteractions(repository, client, examService);
    }

    @Test
    void shouldRejectNullFileBeforeUploadingAnyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "img.dcm",
                "application/dicom",
                new byte[]{1, 2, 3}
        );

        assertThrows(
                InvalidDicomFileException.class,
                () -> service.uploadDicomFiles("EXAM-001", Arrays.asList(file, null))
        );

        verifyNoInteractions(repository, client, examService);
    }

    @Test
    void shouldRejectEmptyFileBeforeUploadingAnyFile() {
        MockMultipartFile validFile = new MockMultipartFile(
                "files",
                "valid.dcm",
                "application/dicom",
                new byte[]{1, 2, 3}
        );

        MockMultipartFile emptyFile = new MockMultipartFile(
                "files",
                "empty.dcm",
                "application/dicom",
                new byte[0]
        );

        assertThrows(
                InvalidDicomFileException.class,
                () -> service.uploadDicomFiles("EXAM-001", List.of(validFile, emptyFile))
        );

        verifyNoInteractions(repository, client, examService);
    }

    @Test
    void shouldRejectUploadWhenExamDoesNotExist() {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "valid.dcm",
                "application/dicom",
                new byte[]{1, 2, 3}
        );

        when(repository.findByCode("EXAM-001")).thenReturn(Optional.empty());

        assertThrows(ExamNotFoundException.class,
                () -> service.uploadDicomFiles("EXAM-001", List.of(file))
        );

        verify(repository).findByCode("EXAM-001");
        verifyNoInteractions(client, examService);
    }

    @Test
    void shouldNotUploadFilesWhenExamAlreadyHasStudy() {
        Instant now = Instant.parse("2020-04-20T00:00:00.00Z");
        RadiologyExam exam = new RadiologyExam("EXAM-001", now);

        exam.associateDicomStudy(
                "study-001",
                "1.2.3",
                now
        );

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "image.dcm",
                "application/dicom",
                new byte[]{1, 2, 3}
        );

        when(repository.findByCode("EXAM-001")).thenReturn(Optional.of(exam));

        assertThrows(
                InvalidDicomStudyAssociationException.class,
                () -> service.uploadDicomFiles("EXAM-001", List.of(file))
        );

        verify(repository).findByCode("EXAM-001");
        verifyNoInteractions(client, examService);
    }

    @Test
    void shouldNotUploadFilesWhenExamIsCanceled() {
        Instant now = Instant.parse("2020-04-20T00:00:00.00Z");
        RadiologyExam exam = new RadiologyExam("EXAM-001", now);

        exam.changeStatus(
                ExamStatus.CANCELED,
                now.plusSeconds(60),
                "user-001"
        );

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "image.dcm",
                "application/dicom",
                new byte[]{1, 2, 3}
        );

        when(repository.findByCode("EXAM-001")).thenReturn(Optional.of(exam));

        assertThrows(
                InvalidDicomStudyAssociationException.class,
                () -> service.uploadDicomFiles("EXAM-001", List.of(file))
        );

        verify(repository).findByCode("EXAM-001");
        verifyNoInteractions(client, examService);
    }

    @Test
    void shouldNotAssociateFilesFromDifferentStudies() {
        Instant now = Instant.parse("2020-04-20T00:00:00.00Z");
        RadiologyExam exam = new RadiologyExam("EXAM-001", now);

        MockMultipartFile firstFile = new MockMultipartFile(
                "files",
                "image.dcm",
                "application/dicom",
                new byte[]{1, 2, 3}
        );

        MockMultipartFile secondFile = new MockMultipartFile(
                "files",
                "image.dcm",
                "application/dicom",
                new byte[]{4, 5, 6}
        );

        OrthancUploadResponseDTO firstUploaded = new OrthancUploadResponseDTO(
                "instance-001",
                "study-001",
                "Success"
        );

        OrthancUploadResponseDTO secondUploaded = new OrthancUploadResponseDTO(
                "instance-002",
                "study-002",
                "Success"
        );

        when(repository.findByCode("EXAM-001")).thenReturn(Optional.of(exam));
        when(client.uploadInstance(any(Resource.class))).thenReturn(firstUploaded, secondUploaded);

        assertThrows(
                InvalidDicomStudyAssociationException.class,
                () -> service.uploadDicomFiles("EXAM-001", List.of(firstFile, secondFile))
        );

        verify(client, times(2)).uploadInstance(any(Resource.class));
        verify(client, never()).getStudy(anyString());
        verifyNoInteractions(examService);
    }

    @Test
    void shouldPropagateUploadFailureAndNotAssociateStudy() {
        Instant now = Instant.parse("2020-04-20T00:00:00.00Z");
        RadiologyExam exam = new RadiologyExam("EXAM-001", now);

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "image.dcm",
                "application/dicom",
                new byte[]{1, 2, 3}
        );

        OrthancCommunicationException failure = new OrthancCommunicationException(
                "Could not communicate with orthanc.",
                new RuntimeException("Connection failed")
        );

        when(repository.findByCode("EXAM-001")).thenReturn(Optional.of(exam));
        when(client.uploadInstance(any(Resource.class))).thenThrow(failure);

        OrthancCommunicationException thrown = assertThrows(
                OrthancCommunicationException.class,
                () -> service.uploadDicomFiles("EXAM-001", List.of(file))
        );

        assertSame(failure, thrown);

        verify(client).uploadInstance(any(Resource.class));
        verify(client, never()).getStudy(anyString());
        verifyNoInteractions(examService);
    }

    @Test
    void shouldPropagateStudyLookupFailureAndNotAssociateStudy() {
        Instant now = Instant.parse("2020-04-20T00:00:00.00Z");
        RadiologyExam exam = new RadiologyExam("EXAM-001", now);

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "image.dcm",
                "application/dicom",
                new byte[]{1, 2, 3}
        );

        OrthancUploadResponseDTO uploaded = new OrthancUploadResponseDTO(
                "instance-001",
                "study-001",
                "Success"
        );

        OrthancCommunicationException failure = new OrthancCommunicationException(
                "Could not communicate with orthanc.",
                new RuntimeException("Connection failed")
        );

        when(repository.findByCode("EXAM-001")).thenReturn(Optional.of(exam));
        when(client.uploadInstance(any(Resource.class))).thenReturn(uploaded);
        when(client.getStudy("study-001")).thenThrow(failure);

        OrthancCommunicationException thrown = assertThrows(
                OrthancCommunicationException.class,
                () -> service.uploadDicomFiles("EXAM-001", List.of(file))
        );

        assertSame(failure, thrown);

        verify(client).uploadInstance(any(Resource.class));
        verify(client).getStudy("study-001");
        verifyNoInteractions(examService);

    }

    @Test
    void shouldPropagateFinalAssociationRejection() {
        Instant now = Instant.parse("2020-04-20T00:00:00.00Z");
        RadiologyExam exam = new RadiologyExam("EXAM-001", now);

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "image.dcm",
                "application/dicom",
                new byte[]{1, 2, 3}
        );

        OrthancUploadResponseDTO uploaded = new OrthancUploadResponseDTO(
                "instance-001",
                "study-001",
                "Success"
        );

        OrthancStudyResponseDTO study = new OrthancStudyResponseDTO(
                new OrthancStudyResponseDTO.MainDicomTags("1.2.3")
        );

        InvalidDicomStudyAssociationException failure = new InvalidDicomStudyAssociationException(
                "A dicom study can only be associated with an exam in REQUESTED status."
        );

        when(repository.findByCode("EXAM-001")).thenReturn(Optional.of(exam));
        when(client.uploadInstance(any(Resource.class))).thenReturn(uploaded);
        when(client.getStudy("study-001")).thenReturn(study);
        when(examService.associateDicomStudy(
                "EXAM-001", "study-001", "1.2.3")).thenThrow(failure);

        InvalidDicomStudyAssociationException thrown = assertThrows(
                InvalidDicomStudyAssociationException.class,
                () -> service.uploadDicomFiles("EXAM-001", List.of(file))
        );

        assertSame(failure, thrown);

        verify(client).uploadInstance(any(Resource.class));
        verify(client).getStudy("study-001");
        verify(examService).associateDicomStudy(
                "EXAM-001", "study-001", "1.2.3"
        );
    }
}
