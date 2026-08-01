package com.leathercad.core.leather;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.model.CADElement;

import java.util.UUID;

/**
 * Marcador de Alinhamento (Notch em V ou T) para encaixe perfeito entre peças de couro.
 */
public record NotchElement(
    String id,
    String layerId,
    Point2D position,
    double angleDegrees,
    double sizeMm,
    NotchType type
) implements CADElement {

    public enum NotchType {
        V_NOTCH,
        T_MARK,
        SLIT
    }

    public NotchElement(String layerId, Point2D position, double angleDegrees) {
        this(UUID.randomUUID().toString(), layerId, position, angleDegrees, 3.0, NotchType.V_NOTCH);
    }

    @Override
    public boolean containsPoint(Point2D p, double toleranceMm) {
        return position.distanceTo(p) <= toleranceMm + sizeMm;
    }

    @Override
    public Rect2D boundingBox() {
        return new Rect2D(position.x() - sizeMm, position.y() - sizeMm, sizeMm * 2, sizeMm * 2);
    }

    @Override
    public CADElement translate(double dx, double dy) {
        return new NotchElement(id, layerId, position.translate(dx, dy), angleDegrees, sizeMm, type);
    }

    @Override
    public CADElement rotate(Point2D center, double angleDegrees) {
        return new NotchElement(id, layerId, position.rotate(center, angleDegrees), (this.angleDegrees + angleDegrees) % 360, sizeMm, type);
    }

    @Override
    public CADElement mirror(Point2D axisStart, Point2D axisEnd) {
        double dx = axisStart.x();
        Point2D newPos = new Point2D(dx + (dx - position.x()), position.y());
        return new NotchElement(id, layerId, newPos, 180 - angleDegrees, sizeMm, type);
    }

    @Override
    public CADElement scale(Point2D center, double factor) {
        return new NotchElement(id, layerId, center.lerp(position, factor), angleDegrees, sizeMm * factor, type);
    }

    @Override
    public CADElement copyWithNewId() {
        return new NotchElement(UUID.randomUUID().toString(), layerId, position, angleDegrees, sizeMm, type);
    }
}
