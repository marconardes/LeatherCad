package com.leathercad.core.model;

import com.leathercad.core.geometry.Bezier2D;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;

import java.util.UUID;

public record BezierElement(String id, String layerId, Bezier2D bezier) implements CADElement {
    public BezierElement(String layerId, Bezier2D bezier) {
        this(UUID.randomUUID().toString(), layerId, bezier);
    }

    @Override
    public boolean containsPoint(Point2D p, double toleranceMm) {
        // 1. Deteção direta nos 4 nós de controle (start, control1, control2, end)
        if (p.distanceTo(bezier.start()) <= toleranceMm * 2.0 ||
            p.distanceTo(bezier.control1()) <= toleranceMm * 2.0 ||
            p.distanceTo(bezier.control2()) <= toleranceMm * 2.0 ||
            p.distanceTo(bezier.end()) <= toleranceMm * 2.0) {
            return true;
        }

        // 2. Amostragem de alta densidade (60 pontos ao longo do arco Bézier)
        int samples = 60;
        Point2D prev = bezier.start();
        for (int i = 1; i <= samples; i++) {
            double t = (double) i / samples;
            Point2D curr = bezier.evaluate(t);
            double dist = distanceToSegment(p, prev, curr);
            if (dist <= toleranceMm) return true;
            prev = curr;
        }
        return false;
    }

    private double distanceToSegment(Point2D p, Point2D a, Point2D b) {
        double l2 = a.distanceTo(b);
        if (l2 < 1e-9) return a.distanceTo(p);
        double t = Math.max(0, Math.min(1, ((p.x() - a.x()) * (b.x() - a.x()) + (p.y() - a.y()) * (b.y() - a.y())) / (l2 * l2)));
        Point2D proj = new Point2D(a.x() + t * (b.x() - a.x()), a.y() + t * (b.y() - a.y()));
        return p.distanceTo(proj);
    }

    @Override
    public Rect2D boundingBox() {
        double minX = Math.min(Math.min(bezier.start().x(), bezier.end().x()), Math.min(bezier.control1().x(), bezier.control2().x()));
        double minY = Math.min(Math.min(bezier.start().y(), bezier.end().y()), Math.min(bezier.control1().y(), bezier.control2().y()));
        double maxX = Math.max(Math.max(bezier.start().x(), bezier.end().x()), Math.max(bezier.control1().x(), bezier.control2().x()));
        double maxY = Math.max(Math.max(bezier.start().y(), bezier.end().y()), Math.max(bezier.control1().y(), bezier.control2().y()));
        return new Rect2D(minX, minY, maxX - minX, maxY - minY);
    }

    @Override
    public CADElement translate(double dx, double dy) {
        Bezier2D newBezier = new Bezier2D(
            bezier.start().translate(dx, dy),
            bezier.control1().translate(dx, dy),
            bezier.control2().translate(dx, dy),
            bezier.end().translate(dx, dy)
        );
        return new BezierElement(id, layerId, newBezier);
    }

    @Override
    public CADElement rotate(Point2D center, double angleDegrees) {
        Bezier2D newBezier = new Bezier2D(
            bezier.start().rotate(center, angleDegrees),
            bezier.control1().rotate(center, angleDegrees),
            bezier.control2().rotate(center, angleDegrees),
            bezier.end().rotate(center, angleDegrees)
        );
        return new BezierElement(id, layerId, newBezier);
    }

    @Override
    public CADElement mirror(Point2D axisStart, Point2D axisEnd) {
        double dx = axisStart.x();
        Bezier2D newBezier = new Bezier2D(
            new Point2D(dx + (dx - bezier.start().x()), bezier.start().y()),
            new Point2D(dx + (dx - bezier.control1().x()), bezier.control1().y()),
            new Point2D(dx + (dx - bezier.control2().x()), bezier.control2().y()),
            new Point2D(dx + (dx - bezier.end().x()), bezier.end().y())
        );
        return new BezierElement(id, layerId, newBezier);
    }

    @Override
    public CADElement scale(Point2D center, double factor) {
        Bezier2D newBezier = new Bezier2D(
            center.lerp(bezier.start(), factor),
            center.lerp(bezier.control1(), factor),
            center.lerp(bezier.control2(), factor),
            center.lerp(bezier.end(), factor)
        );
        return new BezierElement(id, layerId, newBezier);
    }

    @Override
    public CADElement copyWithNewId() {
        return new BezierElement(UUID.randomUUID().toString(), layerId, bezier);
    }
}
