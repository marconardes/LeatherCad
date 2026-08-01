package com.leathercad.core.geometry;

/**
 * Ponto imutável em espaço cartesiano 2D com coordenadas em milímetros (mm).
 */
public record Point2D(double x, double y) {
    public static final Point2D ZERO = new Point2D(0, 0);

    public Point2D translate(double dx, double dy) {
        return new Point2D(x + dx, y + dy);
    }

    public Point2D translate(Vector2D vec) {
        return new Point2D(x + vec.dx(), y + vec.dy());
    }

    public double distanceTo(Point2D other) {
        return Math.hypot(other.x - x, other.y - y);
    }

    public Point2D rotate(Point2D center, double angleDegrees) {
        double rad = Math.toRadians(angleDegrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double dx = x - center.x;
        double dy = y - center.y;
        return new Point2D(
            center.x + (dx * cos - dy * sin),
            center.y + (dx * sin + dy * cos)
        );
    }

    public Point2D lerp(Point2D target, double t) {
        return new Point2D(
            x + (target.x - x) * t,
            y + (target.y - y) * t
        );
    }

    @Override
    public String toString() {
        return String.format("(%.2f, %.2f)mm", x, y);
    }
}
