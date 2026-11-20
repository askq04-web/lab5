package com.example.lab4.service;

import com.example.lab4.dto.AnalysisRequest;
import com.example.lab4.model.Analysis;
import com.example.lab4.model.AnalysisMethod;
import com.example.lab4.model.AnalysisStatus;
import com.example.lab4.model.Measurement;
import com.example.lab4.model.Sample;
import com.example.lab4.model.SampleStatus;
import com.example.lab4.repository.AnalysisMethodRepository;
import com.example.lab4.repository.AnalysisRepository;
import com.example.lab4.repository.MeasurementRepository;
import com.example.lab4.repository.SampleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AnalysisService {

    private static final int MIN_CONFIRMED = 2;

    @Autowired
    private AnalysisRepository analysisRepository;
    @Autowired
    private SampleRepository sampleRepository;
    @Autowired
    private AnalysisMethodRepository methodRepository;
    @Autowired
    private MeasurementRepository measurementRepository;

    public List<Analysis> getAll() {
        return analysisRepository.findAll();
    }

    public Analysis getById(Long id) {
        return analysisRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Исследование не найдено"));
    }

    public Analysis create(AnalysisRequest request) {
        if (request.sampleId() == null || request.methodId() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нужно указать sampleId и methodId");
        Sample sample = findSample(request.sampleId());
        AnalysisMethod method = findMethod(request.methodId());
        checkSampleAndMethod(sample, method);

        Analysis analysis = new Analysis();
        analysis.setSample(sample);
        analysis.setMethod(method);
        analysis.setStatus(AnalysisStatus.NEW);
        analysisRepository.save(analysis);
        refreshSampleStatus(sample);
        return analysis;
    }

    public Analysis update(Long id, AnalysisRequest request) {
        Analysis analysis = getById(id);
        if (request.status() != null && request.status() != analysis.getStatus())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Статус меняется операциями start и complete");
        boolean changeSample = request.sampleId() != null && !request.sampleId().equals(analysis.getSample().getId());
        boolean changeMethod = request.methodId() != null && !request.methodId().equals(analysis.getMethod().getId());
        if (changeSample || changeMethod) {
            if (analysis.getStatus() != AnalysisStatus.NEW)
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Пробу и методику можно менять только у ещё не начатого исследования");
            Sample oldSample = analysis.getSample();
            Sample sample = changeSample ? findSample(request.sampleId()) : oldSample;
            AnalysisMethod method = changeMethod ? findMethod(request.methodId()) : analysis.getMethod();
            checkSampleAndMethod(sample, method);

            analysis.setSample(sample);
            analysis.setMethod(method);
            refreshSampleStatus(oldSample);
            refreshSampleStatus(sample);
        }
        return analysis;
    }

    public void delete(Long id) {
        Analysis analysis = getById(id);
        Sample sample = analysis.getSample();
        if (sample.getStatus() == SampleStatus.ARCHIVED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Проба в архиве, её исследования удалять нельзя");
        analysisRepository.delete(analysis);
        refreshSampleStatus(sample);
    }

    public Analysis start(Long id) {
        Analysis analysis = getById(id);
        if (analysis.getStatus() != AnalysisStatus.NEW)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Начать можно только новое исследование");
        if (analysis.getSample().getStatus() == SampleStatus.ARCHIVED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Проба в архиве");

        analysis.setStatus(AnalysisStatus.IN_PROGRESS);
        refreshSampleStatus(analysis.getSample());
        return analysis;
    }

    public Analysis complete(Long id) {
        Analysis analysis = getById(id);
        if (analysis.getStatus() != AnalysisStatus.IN_PROGRESS)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Завершить можно только начатое исследование");
        int confirmed = 0;
        for (Measurement m : measurementRepository.findByAnalysisId(id)) {
            if (m.getConfirmed()) confirmed++;
        }
        if (confirmed < MIN_CONFIRMED)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Для завершения нужно минимум " + MIN_CONFIRMED + " подтверждённых измерения");

        recalcAverage(analysis);
        analysis.setStatus(AnalysisStatus.COMPLETED);
        analysis.setCompletedAt(LocalDateTime.now());
        refreshSampleStatus(analysis.getSample());
        return analysis;
    }

    public void recalcAverage(Analysis analysis) {
        double sum = 0;
        int count = 0;
        for (Measurement m : measurementRepository.findByAnalysisId(analysis.getId())) {
            if (m.getConfirmed()) {
                sum += m.getValue();
                count++;
            }
        }
        if (count == 0) {
            analysis.setAverageValue(null);
        } else {
            analysis.setAverageValue(sum / count);
        }
    }

    public void refreshSampleStatus(Sample sample) {
        if (sample.getStatus() == SampleStatus.ARCHIVED) return;
        List<Analysis> analyses = analysisRepository.findBySampleId(sample.getId());
        boolean allCompleted = !analyses.isEmpty();
        boolean started = false;
        for (Analysis a : analyses) {
            if (a.getStatus() != AnalysisStatus.COMPLETED) allCompleted = false;
            if (a.getStatus() != AnalysisStatus.NEW) started = true;
        }
        if (allCompleted) {
            sample.setStatus(SampleStatus.ANALYZED);
        } else if (started) {
            sample.setStatus(SampleStatus.IN_WORK);
        } else {
            sample.setStatus(SampleStatus.RECEIVED);
        }
    }

    private void checkSampleAndMethod(Sample sample, AnalysisMethod method) {
        if (sample.getStatus() == SampleStatus.ARCHIVED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Проба в архиве");
        if (!sample.getMaterial().equalsIgnoreCase(method.getAllowedMaterial()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Методика не подходит для этого материала");
        if (analysisRepository.existsBySampleIdAndMethodId(sample.getId(), method.getId()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Эта методика уже назначена пробе");
    }

    private Sample findSample(Long id) {
        return sampleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Проба не найдена"));
    }

    private AnalysisMethod findMethod(Long id) {
        return methodRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Методика не найдена"));
    }
}
