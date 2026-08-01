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
        StitchConfig config = StitchConfig.DEFAULT_FRENCH; // pitch 3.85mm

        StitchElement stitch = new StitchElement("layer1", line, config);

        assertNotNull(stitch.holePoints());
        assertTrue(stitch.holePoints().size() >= 10);
        assertEquals(0.0, stitch.holePoints().get(0).x(), 1e-9);
        assertEquals(38.5, stitch.holePoints().get(stitch.holePoints().size() - 1).x(), 1e-9);
    }
}
