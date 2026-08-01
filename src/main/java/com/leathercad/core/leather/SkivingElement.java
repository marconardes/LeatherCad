package com.leathercad.core.leather;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.model.CADElement;

import java.util.UUID;

/**
 * Área de Chanfro / Desbaste do couro para rebaixo nas bordas e dobras.
 */
public record SkivingElement(
    String id,
    String layerId,
    Rect2D area,
    double skiveWidthMm,
    double targetThicknessMm
) implements CADElement {

    public SkivingElement(String layerId, Rect2D area, double skiveWidthMm) {
        this(UUID.randomUUID().toString(), layerId, area, skiveWidthMm, 0.6);
    }

    @Override
    public boolean containsPoint(Point2D p, double toleranceMm) {
        return area.contains(p);
    }

    @Override
    public Rect2D boundingBox() {
        return area;
    }

    @Override
    public CADElement translate(double dx, double dy) {
        return new SkivingElement(id, layerId, new Rect2D(area.minPoint().translate(dx, dy), area.width(), area.height(), area.cornerRadius()), skiveWidthMm, targetThicknessMm);
    }

    @Override
    public CADElement rotate(Point2D center, double angleDegrees) {
        return new SkivingElement(id, layerId, new Rect2D(area.minPoint().rotate(center, angleDegrees), area.width(), area.height(), area.cornerRadius()), skiveWidthMm, targetThicknessMm);
    }

    @Override
    public CADElement mirror(Point2D axisStart, Point2D axisEnd) {
        double dx = axisStart.x();
        Point2D newMin = new Point2D(dx + (dx - area.minPoint().x() - area.width()), area.minPoint().y());
        return new SkivingElement(id, layerId, new Rect2D(newMin, area.width(), area.height(), area.cornerRadius()), skiveWidthMm, targetThicknessMm);
    }

    @Override
    public CADElement scale(Point2D center, double factor) {
        return new SkivingElement(id, layerId, new Rect2D(center.lerp(area.minPoint(), factor), area.width() * factor, area.height() * factor, area.cornerRadius()), skiveWidthMm * factor, targetThicknessMm);
    }

    @Override
    public CADElement copyWithNewId() {
        return new SkivingElement(UUID.randomUUID().toString(), layerId, area, skiveWidthMm, targetThicknessMm);
    }
}
