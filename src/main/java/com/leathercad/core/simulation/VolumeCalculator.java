package com.leathercad.core.simulation;

import com.leathercad.core.materials.LeatherMaterial;
import com.leathercad.core.model.CADElement;
import com.leathercad.core.model.Document;
import com.leathercad.core.model.Layer;
import com.leathercad.core.model.RectElement;

public class VolumeCalculator {

    public static double calculateTotalAreaMm2(Document doc) {
        double area = 0.0;
        for (CADElement elem : doc.getElements()) {
            if (elem instanceof RectElement rectElem) {
                area += rectElem.rect().width() * rectElem.rect().height();
            }
        }
        return area;
    }

    public static double calculateTotalVolumeMm3(Document doc) {
        double volume = 0.0;
        for (Layer layer : doc.getLayers()) {
            LeatherMaterial mat = layer.getMaterial();
            double thickness = mat != null ? mat.thicknessMm() : 1.4;
            for (CADElement elem : doc.getElements()) {
                if (elem.layerId().equals(layer.getId()) && elem instanceof RectElement rectElem) {
                    volume += rectElem.rect().width() * rectElem.rect().height() * thickness;
                }
            }
        }
        return volume;
    }

    public static double calculateTotalWeightGrams(Document doc) {
        // Gramatura g/m² -> g/mm² = weightGsm / 1,000,000
        double totalGrams = 0.0;
        for (Layer layer : doc.getLayers()) {
            LeatherMaterial mat = layer.getMaterial();
            double gsm = mat != null ? mat.weightGsm() : 850.0;
            double gramsPerMm2 = gsm / 1_000_000.0;

            for (CADElement elem : doc.getElements()) {
                if (elem.layerId().equals(layer.getId()) && elem instanceof RectElement rectElem) {
                    double area = rectElem.rect().width() * rectElem.rect().height();
                    totalGrams += area * gramsPerMm2;
                }
            }
        }
        return totalGrams;
    }
}
