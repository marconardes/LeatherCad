package com.leathercad.core.geometry;

/**
 * Vetor imutável em espaço cartesiano 2D.
 */
public record Vector2D(double dx, double dy) {
    public static final Vector2D ZERO = new Vector2D(0, 0);

    public static Vector2D fromPoints(Point2D from, Point2D to) {
        return new Vector2D(to.x() - from.x(), to.y() - from.y());
    }

    public double length() {
        return Math.hypot(dx, dy);
    }

    public Vector2D normalize() {
        double len = length();
        if (len < 1e-9) return ZERO;
        return new Vector2D(dx / len, dy / len);
    }

    public Vector2D scale(double factor) {
        return new Vector2D(dx * factor, dy * factor);
    }

    public Vector2D perpendicular() {
        double newDx = -dy == 0.0 ? 0.0 : -dy;
        double newDy = dx == 0.0 ? 0.0 : dx;
        return new Vector2D(newDx, newDy);
    }

    public double dot(Vector2D other) {
        return dx * other.dx + dy * other.dy;
    }

    public double cross(Vector2D other) {
        return dx * other.dy - dy * other.dx;
    }

    public double angleDegrees() {
        return Math.toDegrees(Math.atan2(dy, dx));
    }
}
