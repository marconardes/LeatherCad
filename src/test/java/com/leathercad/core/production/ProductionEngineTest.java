package com.leathercad.core.production;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.leather.StitchConfig;
import com.leathercad.core.leather.StitchElement;
import com.leathercad.core.model.Document;
import com.leathercad.core.model.RectElement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ProductionEngineTest {

    @Test
    public void testProductionSummaryCalculation() {
        Document doc = new Document();
        String layerId = doc.getActiveLayer().getId();

        doc.addElement(new RectElement(layerId, new Rect2D(0, 0, 100, 80)));
        doc.addElement(new RectElement(layerId, new Rect2D(0, 0, 100, 80))); // 2 peças iguais
        doc.addElement(new RectElement(layerId, new Rect2D(0, 0, 95, 55)));

        // Adiciona costura de 100mm
        doc.addElement(new StitchElement(layerId, new com.leathercad.core.geometry.LineSegment(new Point2D(0, 0), new Point2D(100, 0)), StitchConfig.DEFAULT_FRENCH));

        ProductionSummary summary = ProductionCalculator.calculateProduction(doc);

        assertNotNull(summary);
        assertEquals(2, summary.items().size());

        // Verifica agregação das 2 peças de 100x80
        BOMItem piece100x80 = summary.items().stream()
            .filter(item -> item.widthMm() == 100.0 && item.heightMm() == 80.0)
            .findFirst().orElse(null);

        assertNotNull(piece100x80);
        assertEquals(2, piece100x80.quantity());
        assertEquals(160.0, piece100x80.totalAreaCm2());

        // Consumo de couro em ft² maior que zero
        assertTrue(summary.totalLeatherSqFt() > 0.0);

        // Consumo de linha positivo
        assertTrue(summary.totalThreadMeters() > 0.0);

        // Tempo de corte estimado positivo
        assertTrue(summary.estimatedCuttingTimeMinutes() > 0.0);
    }
}
