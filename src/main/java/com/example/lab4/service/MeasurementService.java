package com.example.lab4.service;

import com.example.lab4.dto.MeasurementRequest;
import com.example.lab4.model.Analysis;
import com.example.lab4.model.AnalysisMethod;
import com.example.lab4.model.AnalysisStatus;
import com.example.lab4.model.Measurement;
import com.example.lab4.repository.AnalysisRepository;
import com.example.lab4.repository.MeasurementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class MeasurementService {

    @Autowired
    private MeasurementRepository measurementRepository;
    @Autowired
    private AnalysisRepository analysisRepository;
    @Autowired
    private AnalysisService analysisService;

    public List<Measurement> getAll() {
        return measurementRepository.findAll();
    }

    public Measurement getById(Long id) {
        return measurementRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Измерение не найдено"));
    }

    public Measurement create(MeasurementRequest request) {
        if (request.analysisId() == null || request.value() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нужно указать analysisId и value");
        Analysis analysis = analysisRepository.findById(request.analysisId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Исследование не найдено"));
        if (analysis.getStatus() != AnalysisStatus.IN_PROGRESS)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Добавлять измерения можно только в начатое исследование");
        boolean confirmed = request.confirmed() != null && request.confirmed();
        check(analysis, null, request.value(), confirmed);

        Measurement m = new Measurement();
        m.setAnalysis(analysis);
        m.setValue(request.value());
        m.setConfirmed(confirmed);
        m.setMeasuredAt(LocalDateTime.now());
        measurementRepository.save(m);
        analysisService.recalcAverage(analysis);
        return m;
    }

    public Measurement update(Long id, MeasurementRequest request) {
        Measurement m = getById(id);
        Analysis analysis = m.getAnalysis();
        if (request.analysisId() != null && !request.analysisId().equals(analysis.getId()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Исследование у измерения менять нельзя");
        checkNotCompleted(analysis);
        double value = request.value() != null ? request.value() : m.getValue();
        boolean confirmed = request.confirmed() != null ? request.confirmed() : m.getConfirmed();
        check(analysis, id, value, confirmed);

        m.setValue(value);
        m.setConfirmed(confirmed);
        analysisService.recalcAverage(analysis);
        return m;
    }

    public void delete(Long id) {
        Measurement m = getById(id);
        Analysis analysis = m.getAnalysis();
        checkNotCompleted(analysis);
        measurementRepository.delete(m);
        analysisService.recalcAverage(analysis);
    }

    public Measurement confirm(Long id) {
        Measurement m = getById(id);
        Analysis analysis = m.getAnalysis();
        if (analysis.getStatus() != AnalysisStatus.IN_PROGRESS)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Подтверждать измерения можно только в начатом исследовании");
        if (m.getConfirmed())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Измерение уже подтверждено");
        check(analysis, id, m.getValue(), true);

        m.setConfirmed(true);
        analysisService.recalcAverage(analysis);
        return m;
    }

    private void checkNotCompleted(Analysis analysis) {
        if (analysis.getStatus() == AnalysisStatus.COMPLETED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Исследование завершено, измерения менять нельзя");
    }

    private void check(Analysis analysis, Long id, double value, boolean confirmed) {
        AnalysisMethod method = analysis.getMethod();
        if (value < method.getMinValue() || value > method.getMaxValue())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Значение вне диапазона методики");
        if (!confirmed) return;

        double min = value;
        double max = value;
        for (Measurement other : measurementRepository.findByAnalysisId(analysis.getId())) {
            if (other.getConfirmed() && !other.getId().equals(id)) {
                min = Math.min(min, other.getValue());
                max = Math.max(max, other.getValue());
            }
        }
        if (max - min > method.getMaxSpread())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Разброс подтверждённых значений больше допустимого");
    }
}
