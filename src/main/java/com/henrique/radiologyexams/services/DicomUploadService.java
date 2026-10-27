package com.henrique.radiologyexams.services;


import com.henrique.radiologyexams.domain.exam.DicomStudyAssociationResponseDTO;
import com.henrique.radiologyexams.domain.exam.ExamStatus;
import com.henrique.radiologyexams.domain.exam.RadiologyExam;
import com.henrique.radiologyexams.exceptions.ExamNotFoundException;
import com.henrique.radiologyexams.exceptions.InvalidDicomFileException;
import com.henrique.radiologyexams.exceptions.InvalidDicomStudyAssociationException;
import com.henrique.radiologyexams.integrations.orthanc.OrthancClient;
import com.henrique.radiologyexams.integrations.orthanc.OrthancStudyResponseDTO;
import com.henrique.radiologyexams.integrations.orthanc.OrthancUploadResponseDTO;
import com.henrique.radiologyexams.repositories.RadiologyExamRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@Service
public class DicomUploadService {
    private final RadiologyExamRepository repository;
    private final OrthancClient client;
    private final RadiologyExamService service;

    public DicomUploadService(
            RadiologyExamRepository repository,
            OrthancClient client,
            RadiologyExamService service
    ) {

        this.repository = repository;
        this.client = client;
        this.service = service;
    }

    public DicomStudyAssociationResponseDTO uploadDicomFiles(
            String code,
            List<MultipartFile> files
    ) {
        if (files == null || files.isEmpty()) {
            throw new InvalidDicomFileException(
                    "Please, upload at least one dicom file."
            );
        }

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                throw new InvalidDicomFileException("Dicom files cannot be empty.");
            }
        }

        RadiologyExam exam = repository.findByCode(code)
                .orElseThrow(() -> new ExamNotFoundException(code));

        if (exam.hasDicomStudy()) {
            throw new InvalidDicomStudyAssociationException(
                    "A dicom study is already associated with this exam."
            );
        }
        if (exam.getStatus() != ExamStatus.REQUESTED) {
            throw new InvalidDicomStudyAssociationException(
                    "A dicom study can only be associated with an exam in REQUESTED status."
            );
        }

        String orthancStudyId = null;

        for (MultipartFile file : files) {
            OrthancUploadResponseDTO uploaded = client.uploadInstance(file.getResource());

            if (orthancStudyId == null) {
                orthancStudyId = uploaded.parentStudyId();
            } else if (!orthancStudyId.equals(uploaded.parentStudyId())) {
                throw new InvalidDicomStudyAssociationException(
                        "All files must belong to the same dicom study."
                );
            }
        }

        OrthancStudyResponseDTO study = client.getStudy(orthancStudyId);
        String studyInstanceUid = study.mainDicomTags().studyInstanceUid();

        return service.associateDicomStudy(code, orthancStudyId, studyInstanceUid);
    }
}
