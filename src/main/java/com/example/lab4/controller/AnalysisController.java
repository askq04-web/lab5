package com.example.lab4.controller;

import com.example.lab4.dto.AnalysisRequest;
import com.example.lab4.model.Analysis;
import com.example.lab4.service.AnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@PreAuthorize("hasAnyRole('LAB', 'ADMIN')")
@RequestMapping("/api/analyses")
public class AnalysisController {

    @Autowired
    private AnalysisService analysisService;

    @GetMapping
    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Analysis analysis : analysisService.getAll()) {
            result.add(toJson(analysis));
        }
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getOne(@PathVariable Long id) {
        return toJson(analysisService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@RequestBody AnalysisRequest request) {
        return toJson(analysisService.create(request));
    }

    @PatchMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody AnalysisRequest request) {
        return toJson(analysisService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        analysisService.delete(id);
    }

    @PostMapping("/{id}/start")
    public Map<String, Object> start(@PathVariable Long id) {
        return toJson(analysisService.start(id));
    }

    @PostMapping("/{id}/complete")
    public Map<String, Object> complete(@PathVariable Long id) {
        return toJson(analysisService.complete(id));
    }

    private Map<String, Object> toJson(Analysis analysis) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", analysis.getId());
        map.put("sampleId", analysis.getSample().getId());
        map.put("methodId", analysis.getMethod().getId());
        map.put("status", analysis.getStatus());
        map.put("averageValue", analysis.getAverageValue());
        map.put("completedAt", analysis.getCompletedAt());
        return map;
    }
}
