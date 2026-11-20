package com.example.lab4.service;

import com.example.lab4.dto.AnalysisMethodRequest;
import com.example.lab4.model.AnalysisMethod;
import com.example.lab4.repository.AnalysisMethodRepository;
import com.example.lab4.repository.AnalysisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class AnalysisMethodService {

    @Autowired
    private AnalysisMethodRepository methodRepository;
    @Autowired
    private AnalysisRepository analysisRepository;

    public List<AnalysisMethod> getAll() {
        return methodRepository.findAll();
    }

    public AnalysisMethod getById(Long id) {
        return methodRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Методика не найдена"));
    }

    public AnalysisMethod create(AnalysisMethodRequest request) {
        if (isEmpty(request.name()) || isEmpty(request.allowedMaterial()) || request.minValue() == null
                || request.maxValue() == null || request.maxSpread() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Заполнены не все поля");
        checkValues(request.minValue(), request.maxValue(), request.maxSpread());
        if (methodRepository.existsByName(request.name()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Методика с таким названием уже есть");

        AnalysisMethod method = new AnalysisMethod();
        method.setName(request.name());
        method.setAllowedMaterial(request.allowedMaterial());
        method.setMinValue(request.minValue());
        method.setMaxValue(request.maxValue());
        method.setMaxSpread(request.maxSpread());
        return methodRepository.save(method);
    }

    public AnalysisMethod update(Long id, AnalysisMethodRequest request) {
        AnalysisMethod method = getById(id);
        boolean changesLimits = request.allowedMaterial() != null || request.minValue() != null
                || request.maxValue() != null || request.maxSpread() != null;
        if (changesLimits && analysisRepository.existsByMethodId(id))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Методика уже используется в исследованиях, можно поменять только название");
        if (request.name() != null && request.name().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Название не может быть пустым");
        if (request.allowedMaterial() != null && request.allowedMaterial().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Материал не может быть пустым");
        if (request.name() != null && !request.name().equals(method.getName())
                && methodRepository.existsByName(request.name()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Методика с таким названием уже есть");
        double min = request.minValue() != null ? request.minValue() : method.getMinValue();
        double max = request.maxValue() != null ? request.maxValue() : method.getMaxValue();
        double spread = request.maxSpread() != null ? request.maxSpread() : method.getMaxSpread();
        checkValues(min, max, spread);

        if (request.name() != null) method.setName(request.name());
        if (request.allowedMaterial() != null) method.setAllowedMaterial(request.allowedMaterial());
        method.setMinValue(min);
        method.setMaxValue(max);
        method.setMaxSpread(spread);
        return method;
    }

    public void delete(Long id) {
        AnalysisMethod method = getById(id);
        if (analysisRepository.existsByMethodId(id))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Методика используется в исследованиях");
        methodRepository.delete(method);
    }

    private void checkValues(double min, double max, double spread) {
        if (min > max)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minValue больше maxValue");
        if (spread < 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxSpread не может быть отрицательным");
    }

    private boolean isEmpty(String s) {
        return s == null || s.isBlank();
    }
}
