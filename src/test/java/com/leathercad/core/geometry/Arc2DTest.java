package com.leathercad.core.geometry;

import org.junit.jupiter.api.Test;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class Arc2DTest {

    @Test
    public void testFromThreePointsValidArc() {
        // Arco passando por P0(10, 0), P1(0, 10) com P2 no arco em (7.071, 7.071) [Raio 10 com centro na origem]
        Point2D p0 = new Point2D(10, 0);
        Point2D p1 = new Point2D(0, 10);
        Point2D p2 = new Point2D(7.0710678, 7.0710678);

        Optional<Arc2D> result = Arc2D.fromThreePoints(p0, p1, p2);
        assertTrue(result.isPresent());

        Arc2D arc = result.get();
        assertEquals(0.0, arc.center().x(), 1e-3);
        assertEquals(0.0, arc.center().y(), 1e-3);
        assertEquals(10.0, arc.radius(), 1e-3);
        assertEquals(0.0, arc.startAngleDegrees(), 1e-3);
        assertEquals(90.0, arc.sweepAngleDegrees(), 1e-3);
    }

    @Test
    public void testFromThreePointsCollinear() {
        // Pontos colineares (0,0), (5,5), (10,10)
        Point2D p0 = new Point2D(0, 0);
        Point2D p1 = new Point2D(10, 10);
        Point2D p2 = new Point2D(5, 5);

        Optional<Arc2D> result = Arc2D.fromThreePoints(p0, p1, p2);
        assertFalse(result.isPresent(), "Pontos colineares devem retornar Optional.empty()");
    }

    @Test
    public void testFromThreePointsDuplicatePoints() {
        Point2D p0 = new Point2D(5, 5);
        Point2D p1 = new Point2D(5, 5);
        Point2D p2 = new Point2D(10, 10);

        Optional<Arc2D> result = Arc2D.fromThreePoints(p0, p1, p2);
        assertFalse(result.isPresent(), "Pontos duplicados devem retornar Optional.empty()");
    }
}
