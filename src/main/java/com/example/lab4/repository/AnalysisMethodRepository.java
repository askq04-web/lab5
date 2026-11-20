package com.example.lab4.repository;

import com.example.lab4.model.AnalysisMethod;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisMethodRepository extends JpaRepository<AnalysisMethod, Long> {

    boolean existsByName(String name);
}
