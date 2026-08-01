package com.leathercad.core.production;

import java.util.List;

/**
 * Representa uma etapa técnica de montagem na oficina de marcenaria/artesanato em couro.
 */
public record AssemblyStep(
    int stepNumber,
    String stageName,
    String title,
    String description,
    List<String> requiredTools,
    List<String> requiredSupplies,
    List<String> targetElementNames
) {}
