package com.example.lab4.controller;

import com.example.lab4.dto.SampleReport;
import com.example.lab4.dto.SampleRequest;
import com.example.lab4.model.Analysis;
import com.example.lab4.model.Sample;
import com.example.lab4.service.SampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/samples")
public class SampleController {

    @Autowired
    private SampleService sampleService;

    @GetMapping
    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Sample sample : sampleService.getAll()) {
            result.add(toJson(sample));
        }
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getOne(@PathVariable Long id) {
        return toJson(sampleService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@RequestBody SampleRequest request) {
        return toJson(sampleService.create(request));
    }

    @PreAuthorize("hasAnyRole('LAB', 'ADMIN')")
    @PatchMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody SampleRequest request) {
        return toJson(sampleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        sampleService.delete(id);
    }

    @PreAuthorize("hasAnyRole('LAB', 'ADMIN')")
    @PostMapping("/{id}/archive")
    public Map<String, Object> archive(@PathVariable Long id) {
        return toJson(sampleService.archive(id));
    }

    @GetMapping("/{id}/report")
    public Map<String, Object> report(@PathVariable Long id) {
        SampleReport report = sampleService.getReport(id);
        Sample sample = report.sample();

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("sampleId", sample.getId());
        map.put("material", sample.getMaterial());
        map.put("customer", sample.getCustomer().getName());
        map.put("receivedAt", sample.getReceivedAt());
        map.put("status", sample.getStatus());
        List<Map<String, Object>> results = new ArrayList<>();
        for (Analysis a : report.analyses()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("analysisId", a.getId());
            row.put("method", a.getMethod().getName());
            row.put("averageValue", a.getAverageValue());
            row.put("confirmedMeasurements", report.confirmedCounts().get(a.getId()));
            row.put("completedAt", a.getCompletedAt());
            results.add(row);
        }
        map.put("results", results);
        return map;
    }

    private Map<String, Object> toJson(Sample sample) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", sample.getId());
        map.put("material", sample.getMaterial());
        map.put("customerId", sample.getCustomer().getId());
        map.put("receivedAt", sample.getReceivedAt());
        map.put("status", sample.getStatus());
        return map;
    }
}
