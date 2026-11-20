package com.example.lab4.dto;

public record AnalysisMethodRequest(String name, String allowedMaterial, Double minValue,
                                    Double maxValue, Double maxSpread) {
}
