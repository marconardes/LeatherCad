package com.leathercad.core.production;

import java.util.List;

/**
 * Ficha completa de Manual de Montagem e Guia de Oficina para artigos de couro.
 */
public record AssemblyManual(
    String projectTitle,
    int totalPiecesCount,
    int totalStitchHolesCount,
    double totalStitchLengthMeters,
    List<AssemblyStep> steps,
    List<String> masterToolsList,
    List<String> masterSuppliesList
) {}
