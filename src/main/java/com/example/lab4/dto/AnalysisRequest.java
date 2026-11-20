package com.example.lab4.dto;

import com.example.lab4.model.AnalysisStatus;

public record AnalysisRequest(Long sampleId, Long methodId, AnalysisStatus status) {
}
