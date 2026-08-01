package com.leathercad.core.production;

public record BOMItem(
    String pieceName,
    String layerName,
    String materialName,
    double widthMm,
    double heightMm,
    int quantity,
    double unitAreaCm2,
    double totalAreaCm2,
    double totalAreaSqFt
) {
    public String formattedDimensions() {
        return String.format("%.1f x %.1f mm", widthMm, heightMm);
    }
}
