package com.leathercad.core.leather;

import com.leathercad.core.geometry.LineSegment;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.model.CADElement;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record StitchElement(
    String id,
    String layerId,
    LineSegment baseLine,
    StitchConfig config,
    List<Point2D> holePoints
) implements CADElement {

    public StitchElement(String layerId, LineSegment baseLine, StitchConfig config) {
        this(UUID.randomUUID().toString(), layerId, baseLine, config, StitchEngine.calculateStitchHoles(baseLine, config));
    }

    @Override
    public boolean containsPoint(Point2D p, double toleranceMm) {
        for (Point2D hole : holePoints) {
            if (hole.distanceTo(p) <= toleranceMm + config.holeDiameterMm() / 2.0) {
                return true;
            }
        }
        return baseLine.distanceToPoint(p) <= toleranceMm;
    }

    @Override
    public Rect2D boundingBox() {
        double minX = Math.min(baseLine.start().x(), baseLine.end().x());
        double minY = Math.min(baseLine.start().y(), baseLine.end().y());
        double w = Math.abs(baseLine.end().x() - baseLine.start().x());
        double h = Math.abs(baseLine.end().y() - baseLine.start().y());
        return new Rect2D(minX, minY, w, h);
    }

    @Override
    public CADElement translate(double dx, double dy) {
        LineSegment newBase = new LineSegment(baseLine.start().translate(dx, dy), baseLine.end().translate(dx, dy));
        return new StitchElement(id, layerId, newBase, config, StitchEngine.calculateStitchHoles(newBase, config));
    }

    @Override
    public CADElement rotate(Point2D center, double angleDegrees) {
        LineSegment newBase = new LineSegment(baseLine.start().rotate(center, angleDegrees), baseLine.end().rotate(center, angleDegrees));
        return new StitchElement(id, layerId, newBase, config, StitchEngine.calculateStitchHoles(newBase, config));
    }

    @Override
    public CADElement mirror(Point2D axisStart, Point2D axisEnd) {
        double dx = axisStart.x();
        Point2D s = new Point2D(dx + (dx - baseLine.start().x()), baseLine.start().y());
        Point2D e = new Point2D(dx + (dx - baseLine.end().x()), baseLine.end().y());
        LineSegment newBase = new LineSegment(s, e);
        return new StitchElement(id, layerId, newBase, config, StitchEngine.calculateStitchHoles(newBase, config));
    }

    @Override
    public CADElement scale(Point2D center, double factor) {
        LineSegment newBase = new LineSegment(center.lerp(baseLine.start(), factor), center.lerp(baseLine.end(), factor));
        return new StitchElement(id, layerId, newBase, config, StitchEngine.calculateStitchHoles(newBase, config));
    }

    @Override
    public CADElement copyWithNewId() {
        return new StitchElement(UUID.randomUUID().toString(), layerId, baseLine, config, StitchEngine.calculateStitchHoles(baseLine, config));
    }
}
