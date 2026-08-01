package com.leathercad.core.geometry;

/**
 * Segmento de reta imutável 2D entre dois pontos.
 */
public record LineSegment(Point2D start, Point2D end) {
    public double length() {
        return start.distanceTo(end);
    }

    public Point2D midpoint() {
        return start.lerp(end, 0.5);
    }

    public Vector2D direction() {
        return Vector2D.fromPoints(start, end).normalize();
    }

    public Vector2D normal() {
        return direction().perpendicular();
    }

    public Point2D pointAtRatio(double t) {
        return start.lerp(end, t);
    }

    public Point2D pointAt(double t) {
        return start.lerp(end, t);
    }

    public double distanceToPoint(Point2D p) {
        double l2 = start.distanceTo(end);
        if (l2 < 1e-9) return start.distanceTo(p);

        Vector2D v = Vector2D.fromPoints(start, end);
        Vector2D w = Vector2D.fromPoints(start, p);
        double t = Math.max(0, Math.min(1, w.dot(v) / (l2 * l2)));
        Point2D projection = start.translate(v.scale(t));
        return p.distanceTo(projection);
    }

    public LineSegment offset(double distanceMm) {
        Vector2D norm = normal().scale(distanceMm);
        return new LineSegment(start.translate(norm), end.translate(norm));
    }
}
