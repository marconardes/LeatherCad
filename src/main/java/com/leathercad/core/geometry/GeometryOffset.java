package com.leathercad.core.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilitário responsável pelos cálculos de offset de primitivas geométricas 2D (mm).
 * Suporta determinação dinâmica do lado (interno/externo) com base no cursor do mouse.
 */
public class GeometryOffset {

    private GeometryOffset() {}

    /**
     * Calcula o offset de um segmento de reta em relação à posição do cursor.
     */
    public static LineSegment offset(LineSegment line, Point2D cursor, double distanceMm) {
        if (line == null || cursor == null || distanceMm <= 0) return line;

        Vector2D dir = line.direction();
        Vector2D norm = line.normal(); // Perpendicular padrao (dx, dy) -> (-dy, dx)
        Vector2D w = Vector2D.fromPoints(line.start(), cursor);

        // Se o produto escalar com a normal for positivo, o cursor esta do lado da normal
        double side = w.dot(norm) >= 0 ? 1.0 : -1.0;
        Vector2D offsetVec = norm.scale(side * distanceMm);

        return new LineSegment(line.start().translate(offsetVec), line.end().translate(offsetVec));
    }

    /**
     * Calcula o offset de um círculo (concêntrico) em relação à posição do cursor.
     */
    public static Circle2D offset(Circle2D circle, Point2D cursor, double distanceMm) {
        if (circle == null || cursor == null || distanceMm <= 0) return circle;

        double dist = circle.center().distanceTo(cursor);
        double newRadius;
        if (dist < circle.radius()) {
            // Offset interno
            newRadius = Math.max(0.1, circle.radius() - distanceMm);
        } else {
            // Offset externo
            newRadius = circle.radius() + distanceMm;
        }

        return new Circle2D(circle.center(), newRadius);
    }

    /**
     * Calcula o offset de um arco circular em relação à posição do cursor.
     */
    public static Arc2D offset(Arc2D arc, Point2D cursor, double distanceMm) {
        if (arc == null || cursor == null || distanceMm <= 0) return arc;

        double dist = arc.center().distanceTo(cursor);
        double newRadius;
        if (dist < arc.radius()) {
            newRadius = Math.max(0.1, arc.radius() - distanceMm);
        } else {
            newRadius = arc.radius() + distanceMm;
        }

        return new Arc2D(arc.center(), newRadius, arc.startAngleDegrees(), arc.sweepAngleDegrees());
    }

    /**
     * Calcula o offset de um retângulo (expansão ou recuo).
     */
    public static Rect2D offset(Rect2D rect, Point2D cursor, double distanceMm) {
        if (rect == null || cursor == null || distanceMm <= 0) return rect;

        boolean isInside = rect.contains(cursor);
        Point2D min = rect.minPoint();
        double w = rect.width();
        double h = rect.height();

        if (isInside) {
            // Recuo interno
            double newW = Math.max(0.1, w - 2 * distanceMm);
            double newH = Math.max(0.1, h - 2 * distanceMm);
            Point2D newMin = new Point2D(min.x() + distanceMm, min.y() + distanceMm);

            double rTL = rect.rTopLeft() > 0 ? Math.max(0, rect.rTopLeft() - distanceMm) : 0;
            double rTR = rect.rTopRight() > 0 ? Math.max(0, rect.rTopRight() - distanceMm) : 0;
            double rBR = rect.rBottomRight() > 0 ? Math.max(0, rect.rBottomRight() - distanceMm) : 0;
            double rBL = rect.rBottomLeft() > 0 ? Math.max(0, rect.rBottomLeft() - distanceMm) : 0;

            return new Rect2D(newMin, newW, newH, rTL, rTR, rBR, rBL);
        } else {
            // Expansão externa
            double newW = w + 2 * distanceMm;
            double newH = h + 2 * distanceMm;
            Point2D newMin = new Point2D(min.x() - distanceMm, min.y() - distanceMm);

            double rTL = rect.rTopLeft() > 0 ? rect.rTopLeft() + distanceMm : 0;
            double rTR = rect.rTopRight() > 0 ? rect.rTopRight() + distanceMm : 0;
            double rBR = rect.rBottomRight() > 0 ? rect.rBottomRight() + distanceMm : 0;
            double rBL = rect.rBottomLeft() > 0 ? rect.rBottomLeft() + distanceMm : 0;

            return new Rect2D(newMin, newW, newH, rTL, rTR, rBR, rBL);
        }
    }

    /**
     * Calcula o offset de uma polilinha (com canto vivo / mitered join).
     */
    public static Polyline2D offset(Polyline2D polyline, Point2D cursor, double distanceMm) {
        if (polyline == null || cursor == null || distanceMm <= 0) return polyline;

        List<Point2D> pts = polyline.points();
        int n = pts.size();
        if (n < 2) return polyline;

        boolean isClosed = polyline.isClosed();

        // Determinar o lado do offset
        double sideSign = 1.0;
        if (isClosed && n >= 3) {
            boolean isInside = polyline.contains(cursor);
            double signedArea = calculateSignedArea(pts);
            // Se a área for positiva (orientação CCW), a normal padrão aponta para fora (esquerda)
            // Se cursor estiver dentro, queremos mover para dentro (-1.0 se CCW)
            if (signedArea >= 0) {
                sideSign = isInside ? -1.0 : 1.0;
            } else {
                sideSign = isInside ? 1.0 : -1.0;
            }
        } else {
            // Polilinha aberta: lado em relação ao segmento mais próximo
            int closestEdge = findClosestSegmentIndex(pts, isClosed, cursor);
            Point2D p1 = pts.get(closestEdge);
            Point2D p2 = pts.get((closestEdge + 1) % n);
            Vector2D dir = Vector2D.fromPoints(p1, p2).normalize();
            Vector2D norm = dir.perpendicular();
            Vector2D w = Vector2D.fromPoints(p1, cursor);
            sideSign = w.dot(norm) >= 0 ? 1.0 : -1.0;
        }

        // Criar retas paralelas para cada segmento
        int numSegments = isClosed ? n : n - 1;
        List<LineSegment> offsetLines = new ArrayList<>();
        for (int i = 0; i < numSegments; i++) {
            Point2D p1 = pts.get(i);
            Point2D p2 = pts.get((i + 1) % n);
            Vector2D dir = Vector2D.fromPoints(p1, p2).normalize();
            Vector2D norm = dir.perpendicular().scale(sideSign * distanceMm);
            offsetLines.add(new LineSegment(p1.translate(norm), p2.translate(norm)));
        }

        // Interseção analítica entre retas consecutivas (Mitered Join)
        List<Point2D> newPts = new ArrayList<>();

        if (isClosed) {
            for (int i = 0; i < n; i++) {
                LineSegment lPrev = offsetLines.get((i - 1 + n) % n);
                LineSegment lCurr = offsetLines.get(i);
                Point2D inter = intersectLines(lPrev, lCurr);
                if (inter != null) {
                    newPts.add(inter);
                } else {
                    newPts.add(lCurr.start());
                }
            }
        } else {
            // Primeiro ponto
            newPts.add(offsetLines.get(0).start());
            // Pontos intermediários por interseção
            for (int i = 0; i < numSegments - 1; i++) {
                LineSegment lCurr = offsetLines.get(i);
                LineSegment lNext = offsetLines.get(i + 1);
                Point2D inter = intersectLines(lCurr, lNext);
                if (inter != null) {
                    newPts.add(inter);
                } else {
                    newPts.add(lCurr.end());
                }
            }
            // Último ponto
            newPts.add(offsetLines.get(numSegments - 1).end());
        }

        return new Polyline2D(newPts, isClosed);
    }

    private static double calculateSignedArea(List<Point2D> pts) {
        double area = 0.0;
        int n = pts.size();
        for (int i = 0; i < n; i++) {
            Point2D p1 = pts.get(i);
            Point2D p2 = pts.get((i + 1) % n);
            area += (p1.x() * p2.y() - p2.x() * p1.y());
        }
        return area / 2.0;
    }

    private static int findClosestSegmentIndex(List<Point2D> pts, boolean isClosed, Point2D cursor) {
        int n = pts.size();
        int maxEdge = isClosed ? n : n - 1;
        int minIdx = 0;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < maxEdge; i++) {
            Point2D p1 = pts.get(i);
            Point2D p2 = pts.get((i + 1) % n);
            LineSegment seg = new LineSegment(p1, p2);
            double dist = seg.distanceToPoint(cursor);
            if (dist < minDistance) {
                minDistance = dist;
                minIdx = i;
            }
        }
        return minIdx;
    }

    /**
     * Calcula a interseção analítica entre duas retas infinitas que contêm os segmentos seg1 e seg2.
     */
    public static Point2D intersectLines(LineSegment seg1, LineSegment seg2) {
        double x1 = seg1.start().x(), y1 = seg1.start().y();
        double x2 = seg1.end().x(), y2 = seg1.end().y();

        double x3 = seg2.start().x(), y3 = seg2.start().y();
        double x4 = seg2.end().x(), y4 = seg2.end().y();

        double denom = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (Math.abs(denom) < 1e-9) {
            return null; // Paralelas ou colineares
        }

        double t1 = ((x1 - x3) * (y3 - y4) - (y1 - y3) * (x3 - x4)) / denom;
        double px = x1 + t1 * (x2 - x1);
        double py = y1 + t1 * (y2 - y1);

        return new Point2D(px, py);
    }
}
