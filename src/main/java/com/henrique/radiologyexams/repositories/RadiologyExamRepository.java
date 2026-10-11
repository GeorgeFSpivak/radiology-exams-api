package com.henrique.radiologyexams.repositories;

import com.henrique.radiologyexams.domain.exam.RadiologyExam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RadiologyExamRepository
        extends JpaRepository<RadiologyExam, Long>{

    Optional<RadiologyExam> findByCode(String code);

    boolean existsByCode(String code);
}
