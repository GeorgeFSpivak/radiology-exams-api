package com.henrique.radiologyexams.domain.exam;

import jakarta.persistence.*;

import java.time.Instant;


@Entity
@Table(name = "exam_status_changes")
public class ExamStatusChange {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_id", nullable = false)
    private RadiologyExam exam;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false, length = 30)
    private ExamStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private ExamStatus newStatus;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;

    protected ExamStatusChange() {

    }

    ExamStatusChange(
            RadiologyExam exam,
            ExamStatus previousStatus,
            ExamStatus newStatus,
            Instant changedAt,
            String changedBy
    ) {
        if (exam == null) {
            throw new IllegalArgumentException("Exam must not be null.");
        }

        if (previousStatus == null) {
            throw new IllegalArgumentException("Previous status must not be null.");
        }

        if (newStatus == null) {
            throw new IllegalArgumentException("New status must not be null.");
        }

        if (changedAt == null) {
            throw new IllegalArgumentException("Change date must not be null.");
        }

        if (changedBy == null || changedBy.isBlank()) {
            throw new IllegalArgumentException("Changed by must not be blank.");
        }

        if (changedBy.length() > 100) {
            throw new IllegalArgumentException("Changed by must have at most 100 characters.");
        }

        this.exam = exam;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.changedAt = changedAt;
        this.changedBy = changedBy;
    }

    public Long getId() {

        return id;
    }

    public RadiologyExam getExam() {

        return exam;
    }

    public ExamStatus getPreviousStatus() {
        return previousStatus;
    }

    public ExamStatus getNewStatus() {
        return newStatus;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    public String getChangedBy() {
        return changedBy;
    }

}
