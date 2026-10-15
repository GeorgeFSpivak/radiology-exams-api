package com.henrique.radiologyexams.controllers;


import com.henrique.radiologyexams.domain.exam.RadiologyExamDetailsDTO;
import com.henrique.radiologyexams.domain.exam.RadiologyExamRequestDTO;
import com.henrique.radiologyexams.domain.exam.RadiologyExamResponseDTO;
import com.henrique.radiologyexams.services.RadiologyExamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/exams")
public class RadiologyExamController {
    private final RadiologyExamService service;

    public RadiologyExamController(RadiologyExamService service){
        this.service = service;
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
    public ResponseEntity<RadiologyExamDetailsDTO> getExamByCode(@PathVariable String code){
        RadiologyExamDetailsDTO details = service.getExamByCode(code);
        return ResponseEntity.status(HttpStatus.OK).body(details);
    }
}
