package com.example.lab4.dto;

import com.example.lab4.model.Analysis;
import com.example.lab4.model.Sample;

import java.util.List;
import java.util.Map;

public record SampleReport(Sample sample, List<Analysis> analyses, Map<Long, Integer> confirmedCounts) {
}
