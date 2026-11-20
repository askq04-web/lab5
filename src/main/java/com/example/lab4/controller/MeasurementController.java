package com.example.lab4.controller;

import com.example.lab4.dto.MeasurementRequest;
import com.example.lab4.model.Measurement;
import com.example.lab4.service.MeasurementService;
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
@RequestMapping("/api/measurements")
public class MeasurementController {

    @Autowired
    private MeasurementService measurementService;

    @GetMapping
    public List<Map<String, Object>> getAll() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Measurement measurement : measurementService.getAll()) {
            result.add(toJson(measurement));
        }
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getOne(@PathVariable Long id) {
        return toJson(measurementService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@RequestBody MeasurementRequest request) {
        return toJson(measurementService.create(request));
    }

    @PatchMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody MeasurementRequest request) {
        return toJson(measurementService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        measurementService.delete(id);
    }

    @PostMapping("/{id}/confirm")
    public Map<String, Object> confirm(@PathVariable Long id) {
        return toJson(measurementService.confirm(id));
    }

    private Map<String, Object> toJson(Measurement measurement) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", measurement.getId());
        map.put("analysisId", measurement.getAnalysis().getId());
        map.put("value", measurement.getValue());
        map.put("measuredAt", measurement.getMeasuredAt());
        map.put("confirmed", measurement.getConfirmed());
        return map;
    }
}
