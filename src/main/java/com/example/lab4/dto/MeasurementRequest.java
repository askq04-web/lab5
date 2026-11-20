package com.example.lab4.dto;

public record MeasurementRequest(Long analysisId, Double value, Boolean confirmed) {
}
