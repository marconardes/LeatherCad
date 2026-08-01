package com.leathercad.core.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GeometryTest {

    @Test
    public void testPointDistanceAndTranslation() {
        Point2D p1 = new Point2D(0, 0);
        Point2D p2 = new Point2D(3, 4);

        assertEquals(5.0, p1.distanceTo(p2), 1e-9);

        Point2D translated = p1.translate(10, -5);
        assertEquals(10.0, translated.x());
        assertEquals(-5.0, translated.y());
    }

    @Test
    public void testVectorOperations() {
        Vector2D v1 = new Vector2D(1, 0);
        Vector2D v2 = new Vector2D(0, 1);

        assertEquals(0.0, v1.dot(v2), 1e-9);
        assertEquals(1.0, v1.cross(v2), 1e-9);

        Vector2D perp = v1.perpendicular();
        assertEquals(0.0, perp.dx());
        assertEquals(1.0, perp.dy());
    }

    @Test
    public void testLineSegmentDistanceToPoint() {
        LineSegment line = new LineSegment(new Point2D(0, 0), new Point2D(10, 0));
        Point2D p = new Point2D(5, 5);

        assertEquals(5.0, line.distanceToPoint(p), 1e-9);
    }
}
