package com.leathercad.core.export;

import com.leathercad.core.geometry.LineSegment;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.model.Document;
import com.leathercad.core.model.LineElement;
import com.leathercad.core.model.RectElement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ExportTest {

    @Test
    public void testSVGExport() {
        Document doc = new Document();
        LineSegment line = new LineSegment(new Point2D(0, 0), new Point2D(85, 54));
        doc.addElement(new LineElement(doc.getActiveLayer().getId(), line));

        String svg = SVGExporter.exportToString(doc);

        assertNotNull(svg);
        assertTrue(svg.contains("<svg"));
        assertTrue(svg.contains("<line"));
        assertTrue(svg.contains("85.00"));
    }

    @Test
    public void testDXFExport() {
        Document doc = new Document();
        Rect2D rect = new Rect2D(0, 0, 100, 50);
        doc.addElement(new RectElement(doc.getActiveLayer().getId(), rect));

        String dxf = DXFExporter.exportToString(doc);

        assertNotNull(dxf);
        assertTrue(dxf.contains("SECTION"));
        assertTrue(dxf.contains("ENTITIES"));
        assertTrue(dxf.contains("LINE"));
        assertTrue(dxf.contains("EOF"));
    }
}
