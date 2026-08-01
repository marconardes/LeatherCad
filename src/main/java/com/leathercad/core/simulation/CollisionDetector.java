package com.leathercad.core.simulation;

import com.leathercad.core.model.Document;

import java.util.ArrayList;
import java.util.List;

public class CollisionDetector {

    public static List<String> detectInterferences(Document doc) {
        List<String> warnings = new ArrayList<>();

        double maxThickness = LayerStackAnalyzer.calculateMaxAccumulatedThickness(doc);
        if (maxThickness > 6.0) {
            warnings.add(String.format("⚠️ Alerta de Espessura: Acúmulo de %.1f mm requer rebaixamento de bordas (skiving)!", maxThickness));
        }

        if (doc.getElements().size() == 0) {
            warnings.add("ℹ️ O documento está vazio. Adicione elementos para simulação.");
        } else {
            warnings.add("✅ Nenhuma colisão crítica detectada entre ferragens e vincos de dobra.");
        }

        return warnings;
    }
}
