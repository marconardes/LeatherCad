package com.leathercad.core.model;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record RectElement(String id, String layerId, Rect2D rect) implements CADElement {
    public RectElement(String layerId, Rect2D rect) {
        this(UUID.randomUUID().toString(), layerId, rect);
    }

    @Override
    public boolean containsPoint(Point2D p, double toleranceMm) {
        return rect.contains(p);
    }

    @Override
    public Rect2D boundingBox() {
        return rect;
    }

    @Override
    public CADElement translate(double dx, double dy) {
        Point2D newMin = rect.minPoint().translate(dx, dy);
        return new RectElement(id, layerId, new Rect2D(newMin, rect.width(), rect.height(), rect.cornerRadius()));
    }

    @Override
    public CADElement rotate(Point2D center, double angleDegrees) {
        Point2D p1 = rect.minPoint().rotate(center, angleDegrees);
        Point2D p2 = new Point2D(rect.minPoint().x() + rect.width(), rect.minPoint().y()).rotate(center, angleDegrees);
        Point2D p3 = new Point2D(rect.minPoint().x() + rect.width(), rect.minPoint().y() + rect.height()).rotate(center, angleDegrees);
        Point2D p4 = new Point2D(rect.minPoint().x(), rect.minPoint().y() + rect.height()).rotate(center, angleDegrees);

        double minX = Math.min(Math.min(p1.x(), p2.x()), Math.min(p3.x(), p4.x()));
        double minY = Math.min(Math.min(p1.y(), p2.y()), Math.min(p3.y(), p4.y()));
        double maxX = Math.max(Math.max(p1.x(), p2.x()), Math.max(p3.x(), p4.x()));
        double maxY = Math.max(Math.max(p1.y(), p2.y()), Math.max(p3.y(), p4.y()));

        Rect2D newRect = new Rect2D(new Point2D(minX, minY), maxX - minX, maxY - minY, rect.cornerRadius());
        return new RectElement(id, layerId, newRect);
    }

    @Override
    public CADElement mirror(Point2D axisStart, Point2D axisEnd) {
        double newMinX = axisStart.x() + (axisStart.x() - rect.minPoint().x() - rect.width());
        Point2D newMin = new Point2D(newMinX, rect.minPoint().y());
        return new RectElement(id, layerId, new Rect2D(newMin, rect.width(), rect.height(), rect.cornerRadius()));
    }

    @Override
    public CADElement scale(Point2D center, double factor) {
        Point2D newMin = center.lerp(rect.minPoint(), factor);
        return new RectElement(id, layerId, new Rect2D(newMin, rect.width() * factor, rect.height() * factor, rect.cornerRadius()));
    }

    @Override
    public List<SubElementRef> getSubElements() {
        return java.util.List.of(
            new SubElementRef(id, SubElementRef.SubElementType.EDGE, 0),
            new SubElementRef(id, SubElementRef.SubElementType.EDGE, 1),
            new SubElementRef(id, SubElementRef.SubElementType.EDGE, 2),
            new SubElementRef(id, SubElementRef.SubElementType.EDGE, 3),
            new SubElementRef(id, SubElementRef.SubElementType.VERTEX, 0),
            new SubElementRef(id, SubElementRef.SubElementType.VERTEX, 1),
            new SubElementRef(id, SubElementRef.SubElementType.VERTEX, 2),
            new SubElementRef(id, SubElementRef.SubElementType.VERTEX, 3)
        );
    }

    @Override
    public Optional<SubElementRef> findSubElementAt(Point2D p, double toleranceMm) {
        Point2D v0 = rect.minPoint();
        Point2D v1 = new Point2D(rect.minPoint().x() + rect.width(), rect.minPoint().y());
        Point2D v2 = new Point2D(rect.minPoint().x() + rect.width(), rect.minPoint().y() + rect.height());
        Point2D v3 = new Point2D(rect.minPoint().x(), rect.minPoint().y() + rect.height());

        if (p.distanceTo(v0) <= toleranceMm) return Optional.of(new SubElementRef(id, SubElementRef.SubElementType.VERTEX, 0));
        if (p.distanceTo(v1) <= toleranceMm) return Optional.of(new SubElementRef(id, SubElementRef.SubElementType.VERTEX, 1));
        if (p.distanceTo(v2) <= toleranceMm) return Optional.of(new SubElementRef(id, SubElementRef.SubElementType.VERTEX, 2));
        if (p.distanceTo(v3) <= toleranceMm) return Optional.of(new SubElementRef(id, SubElementRef.SubElementType.VERTEX, 3));

        var edge0 = new com.leathercad.core.geometry.LineSegment(v0, v1);
        var edge1 = new com.leathercad.core.geometry.LineSegment(v1, v2);
        var edge2 = new com.leathercad.core.geometry.LineSegment(v3, v2);
        var edge3 = new com.leathercad.core.geometry.LineSegment(v0, v3);

        if (edge0.distanceToPoint(p) <= toleranceMm) return Optional.of(new SubElementRef(id, SubElementRef.SubElementType.EDGE, 0));
        if (edge1.distanceToPoint(p) <= toleranceMm) return Optional.of(new SubElementRef(id, SubElementRef.SubElementType.EDGE, 1));
        if (edge2.distanceToPoint(p) <= toleranceMm) return Optional.of(new SubElementRef(id, SubElementRef.SubElementType.EDGE, 2));
        if (edge3.distanceToPoint(p) <= toleranceMm) return Optional.of(new SubElementRef(id, SubElementRef.SubElementType.EDGE, 3));

        return Optional.empty();
    }

    public java.util.List<LineElement> explodeToLines() {
        Point2D min = rect.minPoint();
        double w = rect.width();
        double h = rect.height();
        Point2D p1 = min;
        Point2D p2 = new Point2D(min.x() + w, min.y());
        Point2D p3 = new Point2D(min.x() + w, min.y() + h);
        Point2D p4 = new Point2D(min.x(), min.y() + h);

        return java.util.List.of(
            new LineElement(layerId, new com.leathercad.core.geometry.LineSegment(p1, p2)),
            new LineElement(layerId, new com.leathercad.core.geometry.LineSegment(p2, p3)),
            new LineElement(layerId, new com.leathercad.core.geometry.LineSegment(p3, p4)),
            new LineElement(layerId, new com.leathercad.core.geometry.LineSegment(p4, p1))
        );
    }

    @Override
    public CADElement copyWithNewId() {
        return new RectElement(UUID.randomUUID().toString(), layerId, rect);
    }

    @Override
    public CADElement createOffset(Point2D cursorPoint, double distanceMm, String targetLayerId) {
        var offsetRect = com.leathercad.core.geometry.GeometryOffset.offset(rect, cursorPoint, distanceMm);
        return new RectElement(UUID.randomUUID().toString(), targetLayerId, offsetRect);
    }
}
