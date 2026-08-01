package com.leathercad.core.simulation;

import com.leathercad.core.materials.LeatherMaterial;
import com.leathercad.core.model.Document;
import com.leathercad.core.model.Layer;

public class LayerStackAnalyzer {

    public static double calculateMaxAccumulatedThickness(Document doc) {
        double maxThickness = 0.0;
        for (Layer layer : doc.getLayers()) {
            LeatherMaterial mat = layer.getMaterial();
            if (mat != null) {
                maxThickness += mat.thicknessMm();
            } else {
                maxThickness += 1.4;
            }
        }
        return maxThickness;
    }
}
