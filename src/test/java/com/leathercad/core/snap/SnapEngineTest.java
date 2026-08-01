package com.leathercad.core.snap;

import com.leathercad.core.geometry.LineSegment;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.model.Document;
import com.leathercad.core.model.LineElement;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class SnapEngineTest {

    @Test
    public void testEndpointSnap() {
        Document doc = new Document();
        LineSegment line = new LineSegment(new Point2D(10, 10), new Point2D(50, 10));
        doc.addElement(new LineElement(doc.getActiveLayer().getId(), line));

        // Cursor perto do ponto inicial (10, 10)
        Point2D cursorNearStart = new Point2D(10.5, 9.8);
        var snap = SnapEngine.findSnap(cursorNearStart, doc, 2.0, 1.0);

        assertTrue(snap.isPresent());
        assertEquals(SnapEngine.SnapType.ENDPOINT, snap.get().type());
        assertEquals(10.0, snap.get().point().x(), 1e-9);
        assertEquals(10.0, snap.get().point().y(), 1e-9);
    }

    @Test
    public void testMidpointSnap() {
        Document doc = new Document();
        LineSegment line = new LineSegment(new Point2D(0, 0), new Point2D(10, 0));
        doc.addElement(new LineElement(doc.getActiveLayer().getId(), line));

        // Cursor perto do ponto médio (5, 0)
        Point2D cursorNearMid = new Point2D(5.1, 0.1);
        var snap = SnapEngine.findSnap(cursorNearMid, doc, 1.0, 1.0);

        assertTrue(snap.isPresent());
        assertEquals(SnapEngine.SnapType.MIDPOINT, snap.get().type());
        assertEquals(5.0, snap.get().point().x(), 1e-9);
        assertEquals(0.0, snap.get().point().y(), 1e-9);
    }

    @Test
    public void testIsGeometricSnap() {
        Document doc = new Document();
        LineSegment line = new LineSegment(new Point2D(0, 0), new Point2D(100, 0));
        doc.addElement(new LineElement(doc.getActiveLayer().getId(), line));

        // Ponto perto de endpoint
        assertTrue(SnapEngine.isGeometricSnap(new Point2D(0.5, 0.2), doc, 5.0));

        // Ponto no espaço (sem snap geométrico)
        assertFalse(SnapEngine.isGeometricSnap(new Point2D(50, 50), doc, 5.0));
    }

    @Test
    public void testOrthoVsSnapPriorityInCADTool() {
        com.leathercad.ui.tools.CADTool tool = new com.leathercad.ui.tools.CADTool() {
            @Override public String getName() { return "Test"; }
            @Override public void onMousePressed(javafx.scene.input.MouseEvent e, Point2D p, Document d, com.leathercad.ui.viewport.CameraTransform c) {}
            @Override public void onMouseDragged(javafx.scene.input.MouseEvent e, Point2D p, Document d, com.leathercad.ui.viewport.CameraTransform c) {}
            @Override public void onMouseReleased(javafx.scene.input.MouseEvent e, Point2D p, Document d, com.leathercad.ui.viewport.CameraTransform c) {}
            @Override public void onMouseMoved(javafx.scene.input.MouseEvent e, Point2D p, Document d, com.leathercad.ui.viewport.CameraTransform c) {}
            @Override public void renderOverlay(javafx.scene.canvas.GraphicsContext gc, com.leathercad.ui.viewport.CameraTransform camera) {}
        };

        Point2D base = new Point2D(0, 0);

        // 1. Sem Shift e sem Snap -> Ponto original
        Point2D p1 = tool.getEffectivePoint(base, new Point2D(10, 2), false, false);
        assertEquals(10.0, p1.x(), 1e-9);
        assertEquals(2.0, p1.y(), 1e-9);

        // 2. Com Shift e sem Snap -> Ponto Ortho (dx > dy -> trava em Y=base.y)
        Point2D p2 = tool.getEffectivePoint(base, new Point2D(10, 2), true, false);
        assertEquals(10.0, p2.x(), 1e-9);
        assertEquals(0.0, p2.y(), 1e-9);

        // 3. Com Shift E com Snap Geometrico -> Prioridade total do Snap (ignora Ortho)
        Point2D snapPoint = new Point2D(10, 2);
        Point2D p3 = tool.getEffectivePoint(base, snapPoint, true, true);
        assertEquals(10.0, p3.x(), 1e-9);
        assertEquals(2.0, p3.y(), 1e-9);
    }
}

