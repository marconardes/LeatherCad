package com.leathercad.core.simulation;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.model.Document;
import com.leathercad.core.model.RectElement;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SimulationEngineTest {

    @Test
    public void testVolumeAndWeightCalculations() {
        Document doc = new Document();
        String layerId = doc.getActiveLayer().getId();

        // Placa 100mm x 100mm = 10.000 mm²
        doc.addElement(new RectElement(layerId, new Rect2D(new Point2D(0, 0), 100.0, 100.0)));

        double area = VolumeCalculator.calculateTotalAreaMm2(doc);
        assertEquals(10000.0, area, 1e-4);

        double volume = VolumeCalculator.calculateTotalVolumeMm3(doc);
        assertTrue(volume > 0.0);

        double weight = VolumeCalculator.calculateTotalWeightGrams(doc);
        assertTrue(weight > 0.0);
    }

    @Test
    public void testLayerStackAnalyzerAndCollisions() {
        Document doc = new Document();
        double maxThickness = LayerStackAnalyzer.calculateMaxAccumulatedThickness(doc);
        assertTrue(maxThickness >= 1.0);

        List<String> warnings = CollisionDetector.detectInterferences(doc);
        assertNotNull(warnings);
        assertTrue(warnings.size() > 0);
    }

    @Test
    public void testFoldSimulatorProjection() {
        Point2D p = new Point2D(50, 50);
        Point2D projected = FoldSimulator.projectIsometric(p, 90.0, 100.0, 100.0);

        assertNotNull(projected);
        assertNotEquals(p.x(), projected.x());
    }
}
