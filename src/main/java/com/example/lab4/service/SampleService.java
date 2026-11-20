package com.example.lab4.service;

import com.example.lab4.dto.SampleReport;
import com.example.lab4.dto.SampleRequest;
import com.example.lab4.model.Analysis;
import com.example.lab4.model.Measurement;
import com.example.lab4.model.Role;
import com.example.lab4.model.Sample;
import com.example.lab4.model.SampleStatus;
import com.example.lab4.model.User;
import com.example.lab4.repository.AnalysisRepository;
import com.example.lab4.repository.MeasurementRepository;
import com.example.lab4.repository.SampleRepository;
import com.example.lab4.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class SampleService {

    @Autowired
    private SampleRepository sampleRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AnalysisRepository analysisRepository;
    @Autowired
    private MeasurementRepository measurementRepository;
    @Autowired
    private UserService userService;

    public List<Sample> getAll() {
        User me = userService.getCurrentUser();
        if (me.getRole() == Role.CUSTOMER) return sampleRepository.findByCustomerId(me.getId());
        return sampleRepository.findAll();
    }

    public Sample getById(Long id) {
        Sample sample = sampleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Проба не найдена"));
        User me = userService.getCurrentUser();
        if (me.getRole() == Role.CUSTOMER && !sample.getCustomer().getId().equals(me.getId()))
            throw new AccessDeniedException("Чужая проба");
        return sample;
    }

    public Sample create(SampleRequest request) {
        if (request.material() == null || request.material().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Не указан material");
        User me = userService.getCurrentUser();
        User customer = me;
        if (me.getRole() != Role.CUSTOMER) {
            if (request.customerId() == null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Нужно указать customerId");
            customer = findCustomer(request.customerId());
        }

        Sample sample = new Sample();
        sample.setMaterial(request.material());
        sample.setCustomer(customer);
        sample.setReceivedAt(LocalDateTime.now());
        sample.setStatus(SampleStatus.RECEIVED);
        return sampleRepository.save(sample);
    }

    public Sample update(Long id, SampleRequest request) {
        Sample sample = getById(id);
        if (sample.getStatus() == SampleStatus.ARCHIVED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Проба в архиве, менять её нельзя");
        if (request.status() != null && request.status() != sample.getStatus())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Статус пробы меняется бизнес-операциями");
        if (request.material() != null) {
            if (request.material().isBlank())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Материал не может быть пустым");
            if (!request.material().equalsIgnoreCase(sample.getMaterial())
                    && !analysisRepository.findBySampleId(id).isEmpty())
                throw new ResponseStatusException(HttpStatus.CONFLICT, "По пробе уже есть исследования, материал менять нельзя");
        }
        User customer = request.customerId() != null ? findCustomer(request.customerId()) : sample.getCustomer();

        if (request.material() != null) sample.setMaterial(request.material());
        sample.setCustomer(customer);
        return sample;
    }

    public void delete(Long id) {
        Sample sample = getById(id);
        if (userService.getCurrentUser().getRole() == Role.CUSTOMER && sample.getStatus() != SampleStatus.RECEIVED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Заказчик может отозвать пробу только пока работа не началась");
        sampleRepository.delete(sample);
    }

    public Sample archive(Long id) {
        Sample sample = getById(id);
        if (sample.getStatus() != SampleStatus.ANALYZED)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "В архив можно отправить только пробу, у которой завершены все исследования");
        sample.setStatus(SampleStatus.ARCHIVED);
        return sample;
    }

    public SampleReport getReport(Long id) {
        Sample sample = getById(id);
        if (sample.getStatus() != SampleStatus.ANALYZED && sample.getStatus() != SampleStatus.ARCHIVED)
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Протокол готов только когда все исследования пробы завершены");
        List<Analysis> analyses = analysisRepository.findBySampleId(id);
        Map<Long, Integer> counts = new HashMap<>();
        for (Analysis a : analyses) {
            int count = 0;
            for (Measurement m : measurementRepository.findByAnalysisId(a.getId())) {
                if (m.getConfirmed()) count++;
            }
            counts.put(a.getId(), count);
        }
        return new SampleReport(sample, analyses, counts);
    }

    private User findCustomer(Long id) {
        User customer = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Заказчик не найден"));
        if (customer.getRole() != Role.CUSTOMER)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Пробу можно оформить только на заказчика");
        return customer;
    }
}
