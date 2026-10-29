package com.henrique.radiologyexams.controllers;


import com.henrique.radiologyexams.domain.exam.*;
import com.henrique.radiologyexams.services.DicomUploadService;
import com.henrique.radiologyexams.services.RadiologyExamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/exams")
public class RadiologyExamController {
    private final RadiologyExamService service;

    private final DicomUploadService uploadService;

    public RadiologyExamController(
            RadiologyExamService service,
            DicomUploadService uploadService
    ) {

        this.service = service;
        this.uploadService = uploadService;
    }

    @PostMapping
    public ResponseEntity<RadiologyExamResponseDTO> createExam(
            @Valid @RequestBody RadiologyExamRequestDTO request
    ){
        RadiologyExamResponseDTO response =
                service.createExam(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{code}")
    public ResponseEntity<RadiologyExamDetailsDTO> getExamByCode(
            @PathVariable String code
    ){
        RadiologyExamDetailsDTO details = service.getExamByCode(code);
        return ResponseEntity.status(HttpStatus.OK).body(details);
    }

    @PatchMapping("/{code}/status")
    public ResponseEntity<ExamStatusChangeResponseDTO> changeExamStatus(
            @PathVariable String code,
            @Valid @RequestBody ExamStatusChangeRequestDTO request
    ) {
        ExamStatusChangeResponseDTO response =
                service.changeExamStatus(code, request);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping(value = "/{code}/dicom", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DicomStudyAssociationResponseDTO> uploadDicomFile(
            @PathVariable String code,
            @RequestParam("files") List<MultipartFile> files
    ) {
        DicomStudyAssociationResponseDTO response = uploadService.uploadDicomFiles(code, files);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
