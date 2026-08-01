package com.leathercad.core.model;

import com.leathercad.core.geometry.LineSegment;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;

import java.util.UUID;

public record LineElement(String id, String layerId, LineSegment line) implements CADElement {
    public LineElement(String layerId, LineSegment line) {
        this(UUID.randomUUID().toString(), layerId, line);
    }

    @Override
    public boolean containsPoint(Point2D p, double toleranceMm) {
        return line.distanceToPoint(p) <= toleranceMm;
    }

    @Override
    public Rect2D boundingBox() {
        double minX = Math.min(line.start().x(), line.end().x());
        double minY = Math.min(line.start().y(), line.end().y());
        double width = Math.abs(line.end().x() - line.start().x());
        double height = Math.abs(line.end().y() - line.start().y());
        return new Rect2D(minX, minY, width, height);
    }

    @Override
    public CADElement translate(double dx, double dy) {
        Point2D newStart = line.start().translate(dx, dy);
        Point2D newEnd = line.end().translate(dx, dy);
        return new LineElement(id, layerId, new LineSegment(newStart, newEnd));
    }

    @Override
    public CADElement rotate(Point2D center, double angleDegrees) {
        Point2D newStart = line.start().rotate(center, angleDegrees);
        Point2D newEnd = line.end().rotate(center, angleDegrees);
        return new LineElement(id, layerId, new LineSegment(newStart, newEnd));
    }

    @Override
    public CADElement mirror(Point2D axisStart, Point2D axisEnd) {
        // Horizontal/Vertical mirror relative to axisStart
        double newStartX = axisStart.x() + (axisStart.x() - line.start().x());
        double newEndX = axisStart.x() + (axisStart.x() - line.end().x());
        Point2D s = new Point2D(newStartX, line.start().y());
        Point2D e = new Point2D(newEndX, line.end().y());
        return new LineElement(id, layerId, new LineSegment(s, e));
    }

    @Override
    public CADElement scale(Point2D center, double factor) {
        Point2D s = center.lerp(line.start(), factor);
        Point2D e = center.lerp(line.end(), factor);
        return new LineElement(id, layerId, new LineSegment(s, e));
    }

    @Override
    public java.util.List<SubElementRef> getSubElements() {
        return java.util.List.of(
            new SubElementRef(id, SubElementRef.SubElementType.EDGE, 0),
            new SubElementRef(id, SubElementRef.SubElementType.VERTEX, 0),
            new SubElementRef(id, SubElementRef.SubElementType.VERTEX, 1)
        );
    }

    @Override
    public java.util.Optional<SubElementRef> findSubElementAt(Point2D p, double toleranceMm) {
        if (p.distanceTo(line.start()) <= toleranceMm) return java.util.Optional.of(new SubElementRef(id, SubElementRef.SubElementType.VERTEX, 0));
        if (p.distanceTo(line.end()) <= toleranceMm) return java.util.Optional.of(new SubElementRef(id, SubElementRef.SubElementType.VERTEX, 1));
        if (line.distanceToPoint(p) <= toleranceMm) return java.util.Optional.of(new SubElementRef(id, SubElementRef.SubElementType.EDGE, 0));
        return java.util.Optional.empty();
    }

    @Override
    public CADElement copyWithNewId() {
        return new LineElement(UUID.randomUUID().toString(), layerId, line);
    }

    @Override
    public CADElement createOffset(Point2D cursorPoint, double distanceMm, String targetLayerId) {
        var offsetLine = com.leathercad.core.geometry.GeometryOffset.offset(line, cursorPoint, distanceMm);
        return new LineElement(UUID.randomUUID().toString(), targetLayerId, offsetLine);
    }
}
