package com.leathercad.core.nesting;

import java.util.List;

public record NestingResult(
    List<NestingPiece> placedPieces,
    HideSheet hideSheet,
    double totalHideAreaCm2,
    double usedAreaCm2,
    double wasteAreaCm2,
    double efficiencyPercent,
    int sheetCount
) {
    public String formattedSummary() {
        return String.format(
            "Pele/Placa: %s\nAproveitamento (Yield): %.1f%%\nÁrea Usada: %.1f cm²\nDesperdício: %.1f cm² (%.1f%%)\nPeças Encaixadas: %d / %d",
            hideSheet.name(),
            efficiencyPercent,
            usedAreaCm2,
            wasteAreaCm2,
            100.0 - efficiencyPercent,
            placedPieces.stream().filter(NestingPiece::isPlaced).count(),
            placedPieces.size()
        );
    }
}
