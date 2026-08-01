package com.leathercad.core.snap;

import com.leathercad.core.geometry.LineSegment;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.model.*;

import java.util.Optional;

public class SnapEngine {
    public enum SnapType {
        ENDPOINT,
        MIDPOINT,
        CENTER,
        INTERSECTION,
        GRID
    }

    public record SnapResult(Point2D point, SnapType type, String label) {}

    public static Optional<SnapResult> findSnap(Point2D cursor, Document document, double snapRadiusMm, double gridSpacingMm) {
        SnapResult bestSnap = null;
        double minDistance = snapRadiusMm;

        // 1. Snap de Interseção entre Linhas (Maior prioridade geométrica)
        var flatElems = document.getElements();
        for (int i = 0; i < flatElems.size(); i++) {
            if (!(flatElems.get(i) instanceof LineElement l1)) continue;
            for (int j = i + 1; j < flatElems.size(); j++) {
                if (!(flatElems.get(j) instanceof LineElement l2)) continue;
                Point2D intersect = findLineIntersection(l1.line(), l2.line());
                if (intersect != null) {
                    double di = cursor.distanceTo(intersect);
                    if (di <= minDistance) {
                        minDistance = di;
                        bestSnap = new SnapResult(intersect, SnapType.INTERSECTION, "Interseção");
                    }
                }
            }
        }

        // 2. Snap de Endpoints, Midpoints e Centros
        for (CADElement elem : document.getElements()) {
            Layer layer = document.findLayerById(elem.layerId());
            if (layer != null && !layer.isVisible()) continue;

            if (elem instanceof LineElement lineElem) {
                LineSegment seg = lineElem.line();
                // Endpoint Start
                double d1 = cursor.distanceTo(seg.start());
                if (d1 < minDistance) {
                    minDistance = d1;
                    bestSnap = new SnapResult(seg.start(), SnapType.ENDPOINT, "Endpoint");
                }
                // Endpoint End
                double d2 = cursor.distanceTo(seg.end());
                if (d2 < minDistance) {
                    minDistance = d2;
                    bestSnap = new SnapResult(seg.end(), SnapType.ENDPOINT, "Endpoint");
                }
                // Midpoint
                Point2D mid = seg.midpoint();
                double dm = cursor.distanceTo(mid);
                if (dm < minDistance) {
                    minDistance = dm;
                    bestSnap = new SnapResult(mid, SnapType.MIDPOINT, "Midpoint");
                }
            } else if (elem instanceof CircleElement circleElem) {
                Point2D center = circleElem.circle().center();
                double dc = cursor.distanceTo(center);
                if (dc < minDistance) {
                    minDistance = dc;
                    bestSnap = new SnapResult(center, SnapType.CENTER, "Centro");
                }
            } else if (elem instanceof ArcElement arcElem) {
                Point2D center = arcElem.arc().center();
                double dc = cursor.distanceTo(center);
                if (dc < minDistance) {
                    minDistance = dc;
                    bestSnap = new SnapResult(center, SnapType.CENTER, "Centro Arco");
                }
            }
        }

        if (bestSnap != null) {
            return Optional.of(bestSnap);
        }

        // Fallback to Grid Snap
        double snappedX = Math.round(cursor.x() / gridSpacingMm) * gridSpacingMm;
        double snappedY = Math.round(cursor.y() / gridSpacingMm) * gridSpacingMm;
        Point2D gridPoint = new Point2D(snappedX, snappedY);

        if (cursor.distanceTo(gridPoint) <= snapRadiusMm) {
            return Optional.of(new SnapResult(gridPoint, SnapType.GRID, "Grid"));
        }

        return Optional.empty();
    }

    public static Point2D findLineIntersection(LineSegment l1, LineSegment l2) {
        double p0_x = l1.start().x(), p0_y = l1.start().y();
        double p1_x = l1.end().x(), p1_y = l1.end().y();
        double p2_x = l2.start().x(), p2_y = l2.start().y();
        double p3_x = l2.end().x(), p3_y = l2.end().y();

        double s1_x = p1_x - p0_x, s1_y = p1_y - p0_y;
        double s2_x = p3_x - p2_x, s2_y = p3_y - p2_y;

        double det = -s2_x * s1_y + s1_x * s2_y;
        if (Math.abs(det) < 1e-9) return null; // Linhas paralelas

        double s = (-s1_y * (p0_x - p2_x) + s1_x * (p0_y - p2_y)) / det;
        double t = ( s2_x * (p0_y - p2_y) - s2_y * (p0_x - p2_x)) / det;

        if (s >= 0 && s <= 1 && t >= 0 && t <= 1) {
            return new Point2D(p0_x + (t * s1_x), p0_y + (t * s1_y));
        }

        return null;
    }

    public static boolean isGeometricSnap(Point2D point, Document document, double toleranceMm) {
        if (point == null || document == null) return false;
        var snap = findSnap(point, document, toleranceMm, 1.0);
        return snap.isPresent() && snap.get().type() != SnapType.GRID;
    }
}

