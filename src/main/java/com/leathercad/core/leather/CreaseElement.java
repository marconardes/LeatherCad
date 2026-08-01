package com.leathercad.core.leather;

import com.leathercad.core.geometry.LineSegment;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.model.CADElement;

import java.util.UUID;

/**
 * Linha de Vinco / Boleador aplicada parallelamente à borda do couro.
 */
public record CreaseElement(
    String id,
    String layerId,
    LineSegment line,
    double marginMm,
    double depthMm
) implements CADElement {

    public CreaseElement(String layerId, LineSegment line, double marginMm) {
        this(UUID.randomUUID().toString(), layerId, line, marginMm, 0.5);
    }

    @Override
    public boolean containsPoint(Point2D p, double toleranceMm) {
        return line.distanceToPoint(p) <= toleranceMm;
    }

    @Override
    public Rect2D boundingBox() {
        double minX = Math.min(line.start().x(), line.end().x());
        double minY = Math.min(line.start().y(), line.end().y());
        double w = Math.abs(line.end().x() - line.start().x());
        double h = Math.abs(line.end().y() - line.start().y());
        return new Rect2D(minX, minY, w, h);
    }

    @Override
    public CADElement translate(double dx, double dy) {
        return new CreaseElement(id, layerId, new LineSegment(line.start().translate(dx, dy), line.end().translate(dx, dy)), marginMm, depthMm);
    }

    @Override
    public CADElement rotate(Point2D center, double angleDegrees) {
        return new CreaseElement(id, layerId, new LineSegment(line.start().rotate(center, angleDegrees), line.end().rotate(center, angleDegrees)), marginMm, depthMm);
    }

    @Override
    public CADElement mirror(Point2D axisStart, Point2D axisEnd) {
        double dx = axisStart.x();
        Point2D s = new Point2D(dx + (dx - line.start().x()), line.start().y());
        Point2D e = new Point2D(dx + (dx - line.end().x()), line.end().y());
        return new CreaseElement(id, layerId, new LineSegment(s, e), marginMm, depthMm);
    }

    @Override
    public CADElement scale(Point2D center, double factor) {
        return new CreaseElement(id, layerId, new LineSegment(center.lerp(line.start(), factor), center.lerp(line.end(), factor)), marginMm * factor, depthMm);
    }

    @Override
    public CADElement copyWithNewId() {
        return new CreaseElement(UUID.randomUUID().toString(), layerId, line, marginMm, depthMm);
    }
}
