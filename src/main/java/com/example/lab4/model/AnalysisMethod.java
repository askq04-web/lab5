package com.example.lab4.model;

import jakarta.persistence.*;

@Entity
@Table(name = "analysis_methods")
public class AnalysisMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Column(name = "allowed_material", nullable = false)
    private String allowedMaterial;

    @Column(name = "min_value", nullable = false)
    private Double minValue;

    @Column(name = "max_value", nullable = false)
    private Double maxValue;

    @Column(name = "max_spread", nullable = false)
    private Double maxSpread;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAllowedMaterial() {
        return allowedMaterial;
    }

    public void setAllowedMaterial(String allowedMaterial) {
        this.allowedMaterial = allowedMaterial;
    }

    public Double getMinValue() {
        return minValue;
    }

    public void setMinValue(Double minValue) {
        this.minValue = minValue;
    }

    public Double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(Double maxValue) {
        this.maxValue = maxValue;
    }

    public Double getMaxSpread() {
        return maxSpread;
    }

    public void setMaxSpread(Double maxSpread) {
        this.maxSpread = maxSpread;
    }

}
