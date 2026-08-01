package com.leathercad.core.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record Polyline2D(List<Point2D> points, boolean isClosed) {

    public Polyline2D {
        points = Collections.unmodifiableList(new ArrayList<>(points));
    }

    public Polyline2D(List<Point2D> points) {
        this(points, false);
    }

    public Rect2D boundingBox() {
        if (points.isEmpty()) return new Rect2D(0, 0, 0, 0);
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Point2D p : points) {
            minX = Math.min(minX, p.x());
            minY = Math.min(minY, p.y());
            maxX = Math.max(maxX, p.x());
            maxY = Math.max(maxY, p.y());
        }
        return new Rect2D(new Point2D(minX, minY), Math.max(1.0, maxX - minX), Math.max(1.0, maxY - minY));
    }

    public boolean contains(Point2D p) {
        if (!isClosed || points.size() < 3) {
            for (Point2D pt : points) {
                if (pt.distanceTo(p) <= 3.0) return true;
            }
            return false;
        }

        // Ray casting algorithm for point-in-polygon
        boolean inside = false;
        int n = points.size();
        for (int i = 0, j = n - 1; i < n; j = i++) {
            Point2D pi = points.get(i);
            Point2D pj = points.get(j);
            if (((pi.y() > p.y()) != (pj.y() > p.y())) &&
                (p.x() < (pj.x() - pi.x()) * (p.y() - pi.y()) / (pj.y() - pi.y() + 1e-9) + pi.x())) {
                inside = !inside;
            }
        }
        return inside;
    }

    public Polyline2D translate(double dx, double dy) {
        List<Point2D> newPoints = new ArrayList<>();
        for (Point2D p : points) newPoints.add(p.translate(dx, dy));
        return new Polyline2D(newPoints, isClosed);
    }

    public Polyline2D rotate(Point2D center, double angleDegrees) {
        List<Point2D> newPoints = new ArrayList<>();
        for (Point2D p : points) newPoints.add(p.rotate(center, angleDegrees));
        return new Polyline2D(newPoints, isClosed);
    }

    public Polyline2D scale(Point2D center, double factor) {
        List<Point2D> newPoints = new ArrayList<>();
        for (Point2D p : points) newPoints.add(center.lerp(p, factor));
        return new Polyline2D(newPoints, isClosed);
    }

    public Polyline2D mirror(Point2D axisStart, Point2D axisEnd) {
        List<Point2D> newPoints = new ArrayList<>();
        double dx = axisStart.x();
        for (Point2D p : points) {
            newPoints.add(new Point2D(dx + (dx - p.x()), p.y()));
        }
        return new Polyline2D(newPoints, isClosed);
    }

    public double getSegmentLength(int edgeIndex) {
        if (points.size() < 2) return 0.0;
        int n = points.size();
        int maxEdge = isClosed ? n : n - 1;
        if (edgeIndex < 0 || edgeIndex >= maxEdge) return 0.0;
        Point2D p1 = points.get(edgeIndex);
        Point2D p2 = points.get((edgeIndex + 1) % n);
        return p1.distanceTo(p2);
    }

    public double getVertexAngleDegrees(int vertexIndex) {
        int n = points.size();
        if (n < 3 || vertexIndex < 0 || vertexIndex >= n) return 0.0;
        if (!isClosed && (vertexIndex == 0 || vertexIndex == n - 1)) return 0.0;

        Point2D current = points.get(vertexIndex);
        Point2D prev = points.get((vertexIndex - 1 + n) % n);
        Point2D next = points.get((vertexIndex + 1) % n);

        double v1x = prev.x() - current.x();
        double v1y = prev.y() - current.y();
        double v2x = next.x() - current.x();
        double v2y = next.y() - current.y();

        double len1 = Math.hypot(v1x, v1y);
        double len2 = Math.hypot(v2x, v2y);
        if (len1 < 1e-9 || len2 < 1e-9) return 0.0;

        double dot = (v1x * v2x + v1y * v2y) / (len1 * len2);
        dot = Math.max(-1.0, Math.min(1.0, dot));
        return Math.toDegrees(Math.acos(dot));
    }
}
