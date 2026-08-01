package com.leathercad.core.model;

import com.leathercad.core.geometry.Circle2D;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;

import java.util.UUID;

public record CircleElement(String id, String layerId, Circle2D circle) implements CADElement {
    public CircleElement(String layerId, Circle2D circle) {
        this(UUID.randomUUID().toString(), layerId, circle);
    }

    @Override
    public boolean containsPoint(Point2D p, double toleranceMm) {
        double dist = circle.center().distanceTo(p);
        return Math.abs(dist - circle.radius()) <= toleranceMm || dist <= circle.radius();
    }

    @Override
    public Rect2D boundingBox() {
        double r = circle.radius();
        return new Rect2D(circle.center().x() - r, circle.center().y() - r, r * 2, r * 2);
    }

    @Override
    public CADElement translate(double dx, double dy) {
        Point2D newCenter = circle.center().translate(dx, dy);
        return new CircleElement(id, layerId, new Circle2D(newCenter, circle.radius()));
    }

    @Override
    public CADElement rotate(Point2D center, double angleDegrees) {
        Point2D newCenter = circle.center().rotate(center, angleDegrees);
        return new CircleElement(id, layerId, new Circle2D(newCenter, circle.radius()));
    }

    @Override
    public CADElement mirror(Point2D axisStart, Point2D axisEnd) {
        double newCenterX = axisStart.x() + (axisStart.x() - circle.center().x());
        Point2D newCenter = new Point2D(newCenterX, circle.center().y());
        return new CircleElement(id, layerId, new Circle2D(newCenter, circle.radius()));
    }

    @Override
    public CADElement scale(Point2D center, double factor) {
        Point2D newCenter = center.lerp(circle.center(), factor);
        return new CircleElement(id, layerId, new Circle2D(newCenter, circle.radius() * factor));
    }

    @Override
    public CADElement copyWithNewId() {
        return new CircleElement(UUID.randomUUID().toString(), layerId, circle);
    }

    @Override
    public CADElement createOffset(Point2D cursorPoint, double distanceMm, String targetLayerId) {
        var offsetCircle = com.leathercad.core.geometry.GeometryOffset.offset(circle, cursorPoint, distanceMm);
        return new CircleElement(UUID.randomUUID().toString(), targetLayerId, offsetCircle);
    }
}
