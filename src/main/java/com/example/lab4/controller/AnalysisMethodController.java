package com.example.lab4.controller;

import com.example.lab4.dto.AnalysisMethodRequest;
import com.example.lab4.model.AnalysisMethod;
import com.example.lab4.service.AnalysisMethodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analysis-methods")
public class AnalysisMethodController {

    @Autowired
    private AnalysisMethodService analysisMethodService;

    @GetMapping
    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (AnalysisMethod method : analysisMethodService.getAll()) {
            result.add(toJson(method));
        }
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getOne(@PathVariable Long id) {
        return toJson(analysisMethodService.getById(id));
    }

    @PreAuthorize("hasAnyRole('LAB', 'ADMIN')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@RequestBody AnalysisMethodRequest request) {
        return toJson(analysisMethodService.create(request));
    }

    @PreAuthorize("hasAnyRole('LAB', 'ADMIN')")
    @PatchMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody AnalysisMethodRequest request) {
        return toJson(analysisMethodService.update(id, request));
    }

    @PreAuthorize("hasAnyRole('LAB', 'ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        analysisMethodService.delete(id);
    }

    private Map<String, Object> toJson(AnalysisMethod method) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", method.getId());
        map.put("name", method.getName());
        map.put("allowedMaterial", method.getAllowedMaterial());
        map.put("minValue", method.getMinValue());
        map.put("maxValue", method.getMaxValue());
        map.put("maxSpread", method.getMaxSpread());
        return map;
    }
}
