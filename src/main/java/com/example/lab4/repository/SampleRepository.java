package com.example.lab4.repository;

import com.example.lab4.model.Sample;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SampleRepository extends JpaRepository<Sample, Long> {

    List<Sample> findByCustomerId(Long customerId);
}
