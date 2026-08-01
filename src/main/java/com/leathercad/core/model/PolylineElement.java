package com.leathercad.core.model;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Polyline2D;
import com.leathercad.core.geometry.Rect2D;

import java.util.UUID;

public record PolylineElement(String id, String layerId, Polyline2D polyline) implements CADElement {

    public PolylineElement(String layerId, Polyline2D polyline) {
        this(UUID.randomUUID().toString(), layerId, polyline);
    }

    @Override
    public boolean containsPoint(Point2D p, double toleranceMm) {
        return polyline.contains(p);
    }

    @Override
    public Rect2D boundingBox() {
        return polyline.boundingBox();
    }

    @Override
    public CADElement translate(double dx, double dy) {
        return new PolylineElement(id, layerId, polyline.translate(dx, dy));
    }

    @Override
    public CADElement rotate(Point2D center, double angleDegrees) {
        return new PolylineElement(id, layerId, polyline.rotate(center, angleDegrees));
    }

    @Override
    public CADElement mirror(Point2D axisStart, Point2D axisEnd) {
        return new PolylineElement(id, layerId, polyline.mirror(axisStart, axisEnd));
    }

    @Override
    public CADElement scale(Point2D center, double factor) {
        return new PolylineElement(id, layerId, polyline.scale(center, factor));
    }

    public java.util.List<LineElement> explodeToLines() {
        java.util.List<LineElement> lines = new java.util.ArrayList<>();
        var pts = polyline.points();
        for (int i = 0; i < pts.size() - 1; i++) {
            lines.add(new LineElement(layerId, new com.leathercad.core.geometry.LineSegment(pts.get(i), pts.get(i + 1))));
        }
        if (polyline.isClosed() && pts.size() > 2) {
            lines.add(new LineElement(layerId, new com.leathercad.core.geometry.LineSegment(pts.get(pts.size() - 1), pts.get(0))));
        }
        return lines;
    }

    @Override
    public java.util.List<SubElementRef> getSubElements() {
        java.util.List<SubElementRef> subs = new java.util.ArrayList<>();
        var pts = polyline.points();
        for (int i = 0; i < pts.size(); i++) {
            subs.add(new SubElementRef(id, SubElementRef.SubElementType.VERTEX, i));
        }
        int numEdges = polyline.isClosed() ? pts.size() : pts.size() - 1;
        for (int i = 0; i < numEdges; i++) {
            subs.add(new SubElementRef(id, SubElementRef.SubElementType.EDGE, i));
        }
        return subs;
    }

    @Override
    public java.util.Optional<SubElementRef> findSubElementAt(Point2D p, double toleranceMm) {
        var pts = polyline.points();
        for (int i = 0; i < pts.size(); i++) {
            if (p.distanceTo(pts.get(i)) <= toleranceMm) {
                return java.util.Optional.of(new SubElementRef(id, SubElementRef.SubElementType.VERTEX, i));
            }
        }
        for (int i = 0; i < pts.size() - 1; i++) {
            var seg = new com.leathercad.core.geometry.LineSegment(pts.get(i), pts.get(i + 1));
            if (seg.distanceToPoint(p) <= toleranceMm) {
                return java.util.Optional.of(new SubElementRef(id, SubElementRef.SubElementType.EDGE, i));
            }
        }
        if (polyline.isClosed() && pts.size() > 2) {
            var seg = new com.leathercad.core.geometry.LineSegment(pts.get(pts.size() - 1), pts.get(0));
            if (seg.distanceToPoint(p) <= toleranceMm) {
                return java.util.Optional.of(new SubElementRef(id, SubElementRef.SubElementType.EDGE, pts.size() - 1));
            }
        }
        return java.util.Optional.empty();
    }

    @Override
    public CADElement copyWithNewId() {
        return new PolylineElement(UUID.randomUUID().toString(), layerId, polyline);
    }

    @Override
    public CADElement createOffset(Point2D cursorPoint, double distanceMm, String targetLayerId) {
        var offsetPoly = com.leathercad.core.geometry.GeometryOffset.offset(polyline, cursorPoint, distanceMm);
        return new PolylineElement(UUID.randomUUID().toString(), targetLayerId, offsetPoly);
    }
}
