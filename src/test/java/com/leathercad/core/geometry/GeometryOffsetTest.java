package com.leathercad.core.geometry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GeometryOffsetTest {

    @Test
    public void testLineSegmentOffset() {
        LineSegment line = new LineSegment(new Point2D(0, 0), new Point2D(100, 0));
        // Cursor acima da linha (Y = 10)
        Point2D cursorAbove = new Point2D(50, 10);
        LineSegment offsetAbove = GeometryOffset.offset(line, cursorAbove, 5.0);

        assertEquals(0.0, offsetAbove.start().x(), 1e-4);
        assertEquals(5.0, offsetAbove.start().y(), 1e-4);
        assertEquals(100.0, offsetAbove.end().x(), 1e-4);
        assertEquals(5.0, offsetAbove.end().y(), 1e-4);

        // Cursor abaixo da linha (Y = -10)
        Point2D cursorBelow = new Point2D(50, -10);
        LineSegment offsetBelow = GeometryOffset.offset(line, cursorBelow, 5.0);

        assertEquals(0.0, offsetBelow.start().x(), 1e-4);
        assertEquals(-5.0, offsetBelow.start().y(), 1e-4);
        assertEquals(100.0, offsetBelow.end().x(), 1e-4);
        assertEquals(-5.0, offsetBelow.end().y(), 1e-4);
    }

    @Test
    public void testCircle2DOffsetInternalAndExternal() {
        Circle2D circle = new Circle2D(new Point2D(50, 50), 20.0);

        // Cursor interno
        Circle2D inner = GeometryOffset.offset(circle, new Point2D(50, 50), 5.0);
        assertEquals(15.0, inner.radius(), 1e-4);
        assertEquals(50.0, inner.center().x(), 1e-4);
        assertEquals(50.0, inner.center().y(), 1e-4);

        // Cursor externo
        Circle2D outer = GeometryOffset.offset(circle, new Point2D(100, 50), 5.0);
        assertEquals(25.0, outer.radius(), 1e-4);
        assertEquals(50.0, outer.center().x(), 1e-4);
        assertEquals(50.0, outer.center().y(), 1e-4);
    }

    @Test
    public void testArc2DOffsetInternalAndExternal() {
        Arc2D arc = new Arc2D(new Point2D(0, 0), 30.0, 0, 90);

        // Cursor interno ao raio
        Arc2D inner = GeometryOffset.offset(arc, new Point2D(10, 10), 5.0);
        assertEquals(25.0, inner.radius(), 1e-4);

        // Cursor externo ao raio
        Arc2D outer = GeometryOffset.offset(arc, new Point2D(50, 50), 5.0);
        assertEquals(35.0, outer.radius(), 1e-4);
    }

    @Test
    public void testRect2DOffsetInternalAndExternal() {
        Rect2D rect = new Rect2D(new Point2D(10, 10), 100.0, 50.0);

        // Cursor dentro
        Rect2D inner = GeometryOffset.offset(rect, new Point2D(50, 30), 5.0);
        assertEquals(15.0, inner.minPoint().x(), 1e-4);
        assertEquals(15.0, inner.minPoint().y(), 1e-4);
        assertEquals(90.0, inner.width(), 1e-4);
        assertEquals(40.0, inner.height(), 1e-4);

        // Cursor fora
        Rect2D outer = GeometryOffset.offset(rect, new Point2D(0, 0), 5.0);
        assertEquals(5.0, outer.minPoint().x(), 1e-4);
        assertEquals(5.0, outer.minPoint().y(), 1e-4);
        assertEquals(110.0, outer.width(), 1e-4);
        assertEquals(60.0, outer.height(), 1e-4);
    }

    @Test
    public void testPolyline2DOffsetOpen() {
        List<Point2D> pts = List.of(
            new Point2D(0, 0),
            new Point2D(0, 50),
            new Point2D(50, 50)
        );
        Polyline2D poly = new Polyline2D(pts, false);

        // Cursor a direita da polilinha
        Point2D cursor = new Point2D(10, 25);
        Polyline2D offsetPoly = GeometryOffset.offset(poly, cursor, 5.0);

        assertNotNull(offsetPoly);
        assertEquals(3, offsetPoly.points().size());
        assertEquals(5.0, offsetPoly.points().get(0).x(), 1e-4);
        assertEquals(0.0, offsetPoly.points().get(0).y(), 1e-4);
        assertEquals(5.0, offsetPoly.points().get(1).x(), 1e-4);
        assertEquals(45.0, offsetPoly.points().get(1).y(), 1e-4);
        assertEquals(50.0, offsetPoly.points().get(2).x(), 1e-4);
        assertEquals(45.0, offsetPoly.points().get(2).y(), 1e-4);
    }
}
