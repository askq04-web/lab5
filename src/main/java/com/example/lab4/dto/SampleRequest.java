package com.example.lab4.dto;

import com.example.lab4.model.SampleStatus;

public record SampleRequest(String material, Long customerId, SampleStatus status) {
}
