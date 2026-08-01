package com.leathercad.core.geometry;

import com.leathercad.core.model.Document;
import com.leathercad.core.model.PolylineElement;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PolylineTest {

    @Test
    public void testPolylineBoundingBoxAndTranslation() {
        List<Point2D> pts = List.of(
            new Point2D(0, 0),
            new Point2D(50, 0),
            new Point2D(50, 30),
            new Point2D(0, 30)
        );

        Polyline2D poly = new Polyline2D(pts, true);
        assertEquals(50.0, poly.boundingBox().width());
        assertEquals(30.0, poly.boundingBox().height());

        Polyline2D moved = poly.translate(10, 20);
        assertEquals(10.0, moved.boundingBox().minPoint().x());
        assertEquals(20.0, moved.boundingBox().minPoint().y());
    }

    @Test
    public void testPolylineDocumentIntegration() {
        Document doc = new Document();
        String layerId = doc.getActiveLayer().getId();

        List<Point2D> pts = List.of(
            new Point2D(10, 10),
            new Point2D(60, 10),
            new Point2D(60, 40)
        );

        PolylineElement elem = new PolylineElement(layerId, new Polyline2D(pts, false));
        doc.addElement(elem);

        assertEquals(1, doc.getElements().size());
        assertTrue(doc.getElements().getFirst() instanceof PolylineElement);
    }
}
