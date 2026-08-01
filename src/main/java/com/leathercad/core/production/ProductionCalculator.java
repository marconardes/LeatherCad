package com.leathercad.core.production;

import com.leathercad.core.model.*;
import com.leathercad.core.leather.StitchElement;

import java.util.*;

public class ProductionCalculator {

    public static final double CM2_TO_SQFT = 929.0304;

    public static ProductionSummary calculateProduction(Document doc) {
        Map<String, Map<String, Integer>> pieceCounts = new LinkedHashMap<>();
        Map<String, RectElement> pieceTemplates = new LinkedHashMap<>();

        double totalStitchLengthMm = 0.0;
        int hardwareCount = 0;
        double totalPerimeterMm = 0.0;

        for (CADElement elem : doc.getElements()) {
            if (elem instanceof RectElement rectElem) {
                var rect = rectElem.rect();
                var layer = doc.findLayerById(elem.layerId());
                String layerName = layer != null ? layer.getName() : "Camada";
                String matName = (layer != null && layer.getMaterial() != null) ? layer.getMaterial().name() : "Couro";

                String key = String.format("%s|%s|%.1fx%.1f", layerName, matName, rect.width(), rect.height());
                pieceCounts.computeIfAbsent(key, k -> new LinkedHashMap<>()).merge("qty", 1, Integer::sum);
                pieceTemplates.putIfAbsent(key, rectElem);

                totalPerimeterMm += (rect.width() + rect.height()) * 2.0;

            } else if (elem instanceof StitchElement stitchElem) {
                totalStitchLengthMm += stitchElem.baseLine().length();
            } else if (elem instanceof CircleElement) {
                hardwareCount++;
            } else if (elem instanceof LineElement lineElem) {
                totalPerimeterMm += lineElem.line().length();
            }
        }

        List<BOMItem> bomItems = new ArrayList<>();
        double totalAreaCm2 = 0.0;

        for (Map.Entry<String, RectElement> entry : pieceTemplates.entrySet()) {
            String key = entry.getKey();
            RectElement rectElem = entry.getValue();
            var rect = rectElem.rect();
            var layer = doc.findLayerById(rectElem.layerId());
            String layerName = layer != null ? layer.getName() : "Camada";
            String matName = (layer != null && layer.getMaterial() != null) ? layer.getMaterial().name() : "Couro";
            int qty = pieceCounts.get(key).get("qty");

            double unitArea = (rect.width() * rect.height()) / 100.0; // cm²
            double totalItemArea = unitArea * qty;
            double totalItemSqFt = totalItemArea / CM2_TO_SQFT;

            totalAreaCm2 += totalItemArea;

            bomItems.add(new BOMItem(
                "Peça de Couro",
                layerName,
                matName,
                rect.width(),
                rect.height(),
                qty,
                unitArea,
                totalItemArea,
                totalItemSqFt
            ));
        }

        double totalSqFt = totalAreaCm2 / CM2_TO_SQFT;
        // Consumo de linha = Comprimento da costura (m) * 1.25 de margem de nó/arremate
        double threadMeters = (totalStitchLengthMm / 1000.0) * 1.25;

        // Estimativa de tempo de corte: velocidade media de corte 50 mm/s
        double estCuttingTimeMinutes = (totalPerimeterMm / 50.0 / 60.0) + 1.0;

        return new ProductionSummary(
            bomItems,
            totalAreaCm2,
            totalSqFt,
            threadMeters,
            hardwareCount,
            estCuttingTimeMinutes
        );
    }
}
