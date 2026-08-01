package com.leathercad.core.geometry;

import java.util.Optional;

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

    /**
     * Constrói um arco passando pelos pontos P0 (início), P1 (fim) e P2 (ponto de passagem).
     */
    public static Optional<Arc2D> fromThreePoints(Point2D p0, Point2D p1, Point2D p2) {
        if (p0 == null || p1 == null || p2 == null) {
            return Optional.empty();
        }

        if (p0.distanceTo(p1) < 1e-4 || p0.distanceTo(p2) < 1e-4 || p1.distanceTo(p2) < 1e-4) {
            return Optional.empty();
        }

        double mid01X = (p0.x() + p1.x()) / 2.0;
        double mid01Y = (p0.y() + p1.y()) / 2.0;
        double mid12X = (p1.x() + p2.x()) / 2.0;
        double mid12Y = (p1.y() + p2.y()) / 2.0;

        double a1 = p1.x() - p0.x();
        double b1 = p1.y() - p0.y();
        double c1 = a1 * mid01X + b1 * mid01Y;

        double a2 = p2.x() - p1.x();
        double b2 = p2.y() - p1.y();
        double c2 = a2 * mid12X + b2 * mid12Y;

        double det = a1 * b2 - a2 * b1;
        if (Math.abs(det) < 1e-7) {
            return Optional.empty(); // Pontos colineares
        }

        double cx = (c1 * b2 - c2 * b1) / det;
        double cy = (a1 * c2 - a2 * c1) / det;
        Point2D center = new Point2D(cx, cy);
        double radius = center.distanceTo(p0);

        double a0 = Math.toDegrees(Math.atan2(p0.y() - cy, p0.x() - cx));
        double a1Deg = Math.toDegrees(Math.atan2(p1.y() - cy, p1.x() - cx));
        double a2Deg = Math.toDegrees(Math.atan2(p2.y() - cy, p2.x() - cx));

        double sweepCCW = (a1Deg - a0) % 360.0;
        if (sweepCCW <= 0) sweepCCW += 360.0;

        double relA2 = (a2Deg - a0) % 360.0;
        if (relA2 < 0) relA2 += 360.0;

        double sweep;
        if (relA2 < sweepCCW) {
            sweep = sweepCCW;
        } else {
            sweep = sweepCCW - 360.0;
        }

        return Optional.of(new Arc2D(center, radius, a0, sweep));
    }
}

