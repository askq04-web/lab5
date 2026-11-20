package com.example.lab4.repository;

import com.example.lab4.model.Measurement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeasurementRepository extends JpaRepository<Measurement, Long> {

    List<Measurement> findByAnalysisId(Long analysisId);
}
