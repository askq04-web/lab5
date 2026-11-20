package com.example.lab4.repository;

import com.example.lab4.model.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    List<Analysis> findBySampleId(Long sampleId);

    boolean existsByMethodId(Long methodId);

    boolean existsBySampleIdAndMethodId(Long sampleId, Long methodId);
}
