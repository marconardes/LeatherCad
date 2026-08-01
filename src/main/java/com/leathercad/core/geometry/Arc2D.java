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

    public record FilletResult(Point2D tA, Point2D tB, Point2D center, double effectiveRadius, double startDeg, double sweepDeg, Arc2D arc) {}

    /**
     * Calcula o arco de concordância tangencial (Fillet) entre o segmento (pA -> vertex) e (vertex -> pB).
     */
    public static Optional<FilletResult> calculateFillet(Point2D pA, Point2D vertex, Point2D pB, double radiusMm) {
        if (pA == null || vertex == null || pB == null || radiusMm <= 0) {
            return Optional.empty();
        }

        double lenA = pA.distanceTo(vertex);
        double lenB = pB.distanceTo(vertex);
        if (lenA < 1e-4 || lenB < 1e-4) {
            return Optional.empty();
        }

        Point2D u = new Point2D((pA.x() - vertex.x()) / lenA, (pA.y() - vertex.y()) / lenA);
        Point2D w = new Point2D((pB.x() - vertex.x()) / lenB, (pB.y() - vertex.y()) / lenB);

        double cosTheta = u.x() * w.x() + u.y() * w.y();
        if (Math.abs(cosTheta) >= 0.999) {
            return Optional.empty(); // Linhas colineares ou paralelas
        }

        double halfAngleRad = Math.acos(cosTheta) / 2.0;
        double tangentDist = radiusMm / Math.tan(halfAngleRad);

        double maxTangent = Math.min(lenA, lenB) * 0.95;
        if (tangentDist > maxTangent) {
            tangentDist = maxTangent;
        }
        double effectiveRadius = tangentDist * Math.tan(halfAngleRad);

        Point2D tA = new Point2D(vertex.x() + tangentDist * u.x(), vertex.y() + tangentDist * u.y());
        Point2D tB = new Point2D(vertex.x() + tangentDist * w.x(), vertex.y() + tangentDist * w.y());

        Point2D nU = new Point2D(-u.y(), u.x());
        if (nU.x() * w.x() + nU.y() * w.y() < 0) {
            nU = new Point2D(-nU.x(), -nU.y());
        }
        Point2D center = new Point2D(tA.x() + effectiveRadius * nU.x(), tA.y() + effectiveRadius * nU.y());

        double startDeg = Math.toDegrees(Math.atan2(tA.y() - center.y(), tA.x() - center.x()));
        double endDeg = Math.toDegrees(Math.atan2(tB.y() - center.y(), tB.x() - center.x()));
        double sweepDeg = endDeg - startDeg;

        while (sweepDeg <= -180.0) sweepDeg += 360.0;
        while (sweepDeg > 180.0) sweepDeg -= 360.0;

        Arc2D arc = new Arc2D(center, effectiveRadius, startDeg, sweepDeg);
        return Optional.of(new FilletResult(tA, tB, center, effectiveRadius, startDeg, sweepDeg, arc));
    }
}


