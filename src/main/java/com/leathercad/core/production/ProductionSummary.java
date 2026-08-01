package com.leathercad.core.production;

import java.util.List;

public record ProductionSummary(
    List<BOMItem> items,
    double totalLeatherAreaCm2,
    double totalLeatherSqFt,
    double totalThreadMeters,
    int totalHardwareCount,
    double estimatedCuttingTimeMinutes
) {
    public String formattedSummary() {
        return String.format(
            "=== FICHA TÉCNICA DE PRODUÇÃO ===\n" +
            "Área Total de Couro: %.2f ft² (%.1f cm²)\n" +
            "Consumo de Linha: %.2f metros\n" +
            "Total de Peças: %d peças\n" +
            "Total de Ferragens: %d unidades\n" +
            "Tempo Estimado de Corte: %.1f minutos",
            totalLeatherSqFt,
            totalLeatherAreaCm2,
            totalThreadMeters,
            items.stream().mapToInt(BOMItem::quantity).sum(),
            totalHardwareCount,
            estimatedCuttingTimeMinutes
        );
    }
}
