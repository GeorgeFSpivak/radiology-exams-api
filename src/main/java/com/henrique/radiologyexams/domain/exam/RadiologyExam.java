package com.henrique.radiologyexams.domain.exam;


import com.henrique.radiologyexams.exceptions.DicomStudyRequiredException;
import com.henrique.radiologyexams.exceptions.InvalidDicomStudyAssociationException;
import com.henrique.radiologyexams.exceptions.InvalidExamStatusTransitionException;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "radiology_exams")
public class RadiologyExam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExamStatus status;

    @OneToMany(mappedBy = "exam", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @OrderBy("changedAt ASC, id ASC")
    private List<ExamStatusChange> statusHistory = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "orthanc_study_id", unique = true, length = 64)
    private String orthancStudyId;

    @Column(name = "study_instance_uid", unique = true, length = 64)
    private String studyInstanceUid;

    @Column(name = "dicom_received_at")
    private Instant dicomReceivedAt;

    protected RadiologyExam() {

    }

    public RadiologyExam(String code, Instant createdAt) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Exam code must not be blank.");
        }

        if (code.length() > 50) {
            throw new IllegalArgumentException(
                    "Exam code must have at most 50 characters."
            );
        }

        if (createdAt == null) {
            throw new IllegalArgumentException("CreatedAt must not be null.");
        }

        this.code = code;
        this.status = ExamStatus.REQUESTED;
        this.createdAt = createdAt;
    }

    public Long getId() {

        return id;
    }

    public String getCode() {


        return code;
    }

    public ExamStatus getStatus() {


        return status;
    }

    public List<ExamStatusChange> getStatusHistory() {

        return List.copyOf(statusHistory);
    }

    public ExamStatusChange changeStatus(
            ExamStatus nextStatus,
            Instant changedAt,
            String changedBy
    ) {
        if (!status.canTransitionTo(nextStatus)) {

            throw new InvalidExamStatusTransitionException(status, nextStatus);
        }

        if (nextStatus == ExamStatus.EXAM_PERFORMED && !hasDicomStudy()) {

            throw new DicomStudyRequiredException();
        }

        ExamStatusChange change = new ExamStatusChange(
                this,
                status,
                nextStatus,
                changedAt,
                changedBy
        );

        this.status = nextStatus;
        this.statusHistory.add(change);

        return change;
    }

    public Instant getCreatedAt() {

        return createdAt;
    }

    public Long getVersion() {

        return version;
    }

    public String getOrthancStudyId() {

        return orthancStudyId;
    }

    public String getStudyInstanceUid() {

        return studyInstanceUid;
    }

    public Instant getDicomReceivedAt() {

        return dicomReceivedAt;
    }

    public boolean hasDicomStudy() {

        return orthancStudyId != null;
    }

    public void associateDicomStudy(
            String orthancStudyId,
            String studyInstanceUid,
            Instant dicomReceivedAt
    ){
        if (orthancStudyId == null || orthancStudyId.isBlank()) {
            throw new IllegalArgumentException(
                    "Orthanc Study Id must not be null/blank."
            );
        }

        if (orthancStudyId.length() > 64) {
            throw new IllegalArgumentException(
                    "Orthanc Study Id must not be longer than 64 characters."
            );
        }

        if (studyInstanceUid == null || studyInstanceUid.isBlank()) {
            throw new IllegalArgumentException(
                    "Study Instance Uid must not be null/blank."
            );
        }

        if (studyInstanceUid.length() > 64) {
            throw new IllegalArgumentException(
                    "Study Instance Uid must not be longer than 64 characters."
            );
        }

        if (dicomReceivedAt == null) {
            throw new IllegalArgumentException("Dicom Received At must not be null.");
        }

        if (hasDicomStudy()) {
            throw new InvalidDicomStudyAssociationException(
                    "A dicom study is already associated with this exam."
            );
        }

        if (status != ExamStatus.REQUESTED) {
            throw new InvalidDicomStudyAssociationException(
                    "A dicom study can only be associated with an exam in REQUESTED status."
            );
        }

        this.orthancStudyId = orthancStudyId;
        this.studyInstanceUid = studyInstanceUid;
        this.dicomReceivedAt = dicomReceivedAt;
    }
}
