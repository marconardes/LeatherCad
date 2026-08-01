package com.leathercad.core.geometry;

/**
 * Arco circular 2D com centro, raio, ângulo inicial e varredura angular em graus.
 */
public record Arc2D(Point2D center, double radius, double startAngleDegrees, double sweepAngleDegrees) {
    public Point2D startPoint() {
        double rad = Math.toRadians(startAngleDegrees);
        return new Point2D(center.x() + radius * Math.cos(rad), center.y() + radius * Math.sin(rad));
    }

    public Point2D endPoint() {
        double rad = Math.toRadians(startAngleDegrees + sweepAngleDegrees);
        return new Point2D(center.x() + radius * Math.cos(rad), center.y() + radius * Math.sin(rad));
    }

    public double arcLength() {
        return Math.toRadians(Math.abs(sweepAngleDegrees)) * radius;
    }
}
