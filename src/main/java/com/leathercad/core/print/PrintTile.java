package com.leathercad.core.print;

import com.leathercad.core.geometry.Rect2D;

public record PrintTile(
    int pageIndex,
    int totalPages,
    int colIndex,
    int rowIndex,
    int totalCols,
    int totalRows,
    Rect2D worldBounds,
    PrintPaperSize paperSize,
    double marginMm,
    double overlapMm
) {
    public String formattedLabel() {
        return String.format("Folha %d de %d (L%d, C%d)", pageIndex, totalPages, rowIndex + 1, colIndex + 1);
    }
}
