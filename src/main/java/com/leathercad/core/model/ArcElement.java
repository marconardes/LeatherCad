package com.leathercad.core.model;

import com.leathercad.core.geometry.Arc2D;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;

import java.util.UUID;

public record ArcElement(String id, String layerId, Arc2D arc) implements CADElement {
    public ArcElement(String layerId, Arc2D arc) {
        this(UUID.randomUUID().toString(), layerId, arc);
    }

    @Override
    public boolean containsPoint(Point2D p, double toleranceMm) {
        double dist = arc.center().distanceTo(p);
        if (Math.abs(dist - arc.radius()) > toleranceMm) return false;

        double angle = Math.toDegrees(Math.atan2(p.y() - arc.center().y(), p.x() - arc.center().x()));
        if (angle < 0) angle += 360;

        double start = arc.startAngleDegrees() % 360;
        if (start < 0) start += 360;

        double sweep = arc.sweepAngleDegrees();
        if (sweep >= 360) return true;

        double end = (start + sweep) % 360;
        if (start <= end) {
            return angle >= start && angle <= end;
        } else {
            return angle >= start || angle <= end;
        }
    }

    @Override
    public Rect2D boundingBox() {
        double r = arc.radius();
        return new Rect2D(arc.center().x() - r, arc.center().y() - r, r * 2, r * 2);
    }

    @Override
    public CADElement translate(double dx, double dy) {
        Point2D newCenter = arc.center().translate(dx, dy);
        return new ArcElement(id, layerId, new Arc2D(newCenter, arc.radius(), arc.startAngleDegrees(), arc.sweepAngleDegrees()));
    }

    @Override
    public CADElement rotate(Point2D center, double angleDegrees) {
        Point2D newCenter = arc.center().rotate(center, angleDegrees);
        return new ArcElement(id, layerId, new Arc2D(newCenter, arc.radius(), arc.startAngleDegrees() + angleDegrees, arc.sweepAngleDegrees()));
    }

    @Override
    public CADElement mirror(Point2D axisStart, Point2D axisEnd) {
        double newCenterX = axisStart.x() + (axisStart.x() - arc.center().x());
        Point2D newCenter = new Point2D(newCenterX, arc.center().y());
        return new ArcElement(id, layerId, new Arc2D(newCenter, arc.radius(), 180 - arc.startAngleDegrees(), arc.sweepAngleDegrees()));
    }

    @Override
    public CADElement scale(Point2D center, double factor) {
        Point2D newCenter = center.lerp(arc.center(), factor);
        return new ArcElement(id, layerId, new Arc2D(newCenter, arc.radius() * factor, arc.startAngleDegrees(), arc.sweepAngleDegrees()));
    }

    @Override
    public CADElement copyWithNewId() {
        return new ArcElement(UUID.randomUUID().toString(), layerId, arc);
    }
}
