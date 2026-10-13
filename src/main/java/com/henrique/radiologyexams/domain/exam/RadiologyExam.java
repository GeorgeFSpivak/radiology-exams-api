package com.henrique.radiologyexams.domain.exam;


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

    protected RadiologyExam() {

    }

    public RadiologyExam(String code, Instant createdAt) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Exam code must not be blank.");
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
            throw new IllegalStateException(
                    "cannot transition exam from " + status + " to " + nextStatus + ".");
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
}
