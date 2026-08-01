package com.leathercad.core.simulation;

import java.util.List;

public record SimulationResult(
    double totalAreaMm2,
    double totalVolumeMm3,
    double totalWeightGrams,
    double maxAccumulatedThicknessMm,
    int layerCount,
    List<String> warnings
) {}
