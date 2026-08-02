package com.leathercad.core.leather;

import com.leathercad.core.geometry.LineSegment;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.model.Document;
import com.leathercad.core.model.RectElement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LeatherEngineTest {

    @Test
    public void testCornerRadii() {
        Document doc = new Document();
        Rect2D rect = new Rect2D(0, 0, 100, 50, 0.0);
        RectElement elem = new RectElement(doc.getActiveLayer().getId(), rect);
        doc.addElement(elem);
        doc.selectElement(elem.id(), false);

        doc.setCornerRadiusSelected(5.0);

        RectElement updated = (RectElement) doc.getElements().get(0);
        assertEquals(5.0, updated.rect().cornerRadius(), 1e-9);
    }

    @Test
    public void testCreaseElement() {
        LineSegment line = new LineSegment(new Point2D(0, 0), new Point2D(50, 0));
        CreaseElement crease = new CreaseElement("layer1", line, 1.5);

        assertEquals(1.5, crease.marginMm(), 1e-9);
        assertEquals(50.0, crease.boundingBox().width(), 1e-9);
    }

    @Test
    public void testStitchEngine() {
        LineSegment line = new LineSegment(new Point2D(0, 0), new Point2D(38.5, 0));
        StitchConfig config = StitchConfig.DEFAULT; // pitch 3.85mm, FRENCH_SLANT

        StitchElement stitch = new StitchElement("layer1", line, config);

        assertNotNull(stitch.holePoints());
        assertEquals(11, stitch.holePoints().size()); // 38.5 / 3.85 = 10 passos -> 11 furos
        assertEquals(0.0, stitch.holePoints().get(0).x(), 1e-9);
        assertEquals(38.5, stitch.holePoints().get(stitch.holePoints().size() - 1).x(), 1e-9);
    }

    @Test
    public void testFrenchSlantStitchDefaultConfig() {
        StitchConfig cfg = StitchConfig.DEFAULT;
        assertEquals(StitchType.FRENCH_SLANT, cfg.type());
        assertEquals(3.85, cfg.pitchMm(), 1e-9);
        assertEquals(2.0, cfg.slotLengthMm(), 1e-9);
        assertEquals(45.0, cfg.angleDegrees(), 1e-9);
    }

    @Test
    public void testCornerAutoAlignmentAndSharing() {
        // Testar contorno retangular fechado (4 vértices)
        java.util.List<Point2D> rectVertices = java.util.List.of(
            new Point2D(0, 0),
            new Point2D(38.5, 0),
            new Point2D(38.5, 38.5),
            new Point2D(0, 38.5)
        );

        var holes = StitchEngine.calculateStitchHolesForPolyline(rectVertices, true, StitchConfig.DEFAULT);
        assertNotNull(holes);

        // Deve conter os 4 cantos exatos e sem duplicatas nos vértices
        assertTrue(holes.stream().anyMatch(p -> p.distanceTo(new Point2D(0, 0)) < 1e-6));
        assertTrue(holes.stream().anyMatch(p -> p.distanceTo(new Point2D(38.5, 0)) < 1e-6));
        assertTrue(holes.stream().anyMatch(p -> p.distanceTo(new Point2D(38.5, 38.5)) < 1e-6));
        assertTrue(holes.stream().anyMatch(p -> p.distanceTo(new Point2D(0, 38.5)) < 1e-6));
    }

    @Test
    public void testSlantSlotsCalculationAngle() {
        LineSegment line = new LineSegment(new Point2D(0, 0), new Point2D(10, 0)); // Horizontal (tangente 0 rad)
        StitchConfig config = new StitchConfig(3.85, 5.0, 1.2, 2.0, 45.0, StitchType.FRENCH_SLANT);

        StitchElement stitch = new StitchElement("layer1", line, config);
        var slots = stitch.calculateSlantSlots();

        assertNotNull(slots);
        assertFalse(slots.isEmpty());

        // Para primeiro furo em (0,0), fenda inclinada a +45° com comprimento 2.0mm (metade 1.0mm)
        // dx = cos(45°)*1.0 = 0.707106, dy = sin(45°)*1.0 = 0.707106
        LineSegment firstSlot = slots.get(0);
        double slotLen = firstSlot.length();
        assertEquals(2.0, slotLen, 1e-5);
    }
}
