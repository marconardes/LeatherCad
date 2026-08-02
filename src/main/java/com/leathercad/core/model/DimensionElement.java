package com.leathercad.core.model;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;

import java.util.UUID;

public record DimensionElement(
    String id,
    String layerId,
    Point2D start,
    Point2D end,
    DimensionType type,
    double offsetMm,
    String customLabel
) implements CADElement {

    public enum DimensionType {
        LINEAR,
        RADIUS,
        ANGULAR,
        CALLOUT_NOTE,

        // Aliases legados para retrocompatibilidade
        HORIZONTAL,
        VERTICAL,
        DIAMETER
    }

    public DimensionElement(String layerId, Point2D start, Point2D end, DimensionType type, double offsetMm) {
        this(UUID.randomUUID().toString(), layerId, start, end, type, offsetMm, null);
    }

    public DimensionElement(String layerId, Point2D start, Point2D end, DimensionType type, double offsetMm, String customLabel) {
        this(UUID.randomUUID().toString(), layerId, start, end, type, offsetMm, customLabel);
    }

    public String formattedText() {
        if (customLabel != null && !customLabel.isBlank()) return customLabel;
        return switch (type) {
            case LINEAR -> String.format(java.util.Locale.US, "%.2f mm", start.distanceTo(end));
            case HORIZONTAL -> String.format(java.util.Locale.US, "%.2f mm", Math.abs(end.x() - start.x()));
            case VERTICAL -> String.format(java.util.Locale.US, "%.2f mm", Math.abs(end.y() - start.y()));
            case RADIUS -> String.format(java.util.Locale.US, "R %.2f mm", start.distanceTo(end));
            case DIAMETER -> String.format(java.util.Locale.US, "Ø %.2f mm", start.distanceTo(end) * 2);
            case ANGULAR -> "45.00°";
            case CALLOUT_NOTE -> "Nota Técnica";
        };
    }

    @Override
    public boolean containsPoint(Point2D p, double toleranceMm) {
        if (start.distanceTo(p) <= toleranceMm || end.distanceTo(p) <= toleranceMm) return true;
        Point2D mid = new Point2D((start.x() + end.x()) / 2.0, (start.y() + end.y()) / 2.0);
        if (mid.distanceTo(p) <= toleranceMm * 2.0) return true;
        var seg = new com.leathercad.core.geometry.LineSegment(start, end);
        return seg.distanceToPoint(p) <= toleranceMm;
    }

    @Override
    public Rect2D boundingBox() {
        double minX = Math.min(start.x(), end.x());
        double minY = Math.min(start.y(), end.y());
        double w = Math.abs(end.x() - start.x());
        double h = Math.abs(end.y() - start.y());
        return new Rect2D(minX, minY, Math.max(w, 1.0), Math.max(h, 1.0));
    }

    @Override
    public CADElement translate(double dx, double dy) {
        return new DimensionElement(id, layerId, start.translate(dx, dy), end.translate(dx, dy), type, offsetMm, customLabel);
    }

    @Override
    public CADElement rotate(Point2D center, double angleDegrees) {
        return new DimensionElement(id, layerId, start.rotate(center, angleDegrees), end.rotate(center, angleDegrees), type, offsetMm, customLabel);
    }

    @Override
    public CADElement mirror(Point2D axisStart, Point2D axisEnd) {
        double newStartX = axisStart.x() + (axisStart.x() - start.x());
        double newEndX = axisStart.x() + (axisStart.x() - end.x());
        return new DimensionElement(id, layerId, new Point2D(newStartX, start.y()), new Point2D(newEndX, end.y()), type, offsetMm, customLabel);
    }

    @Override
    public CADElement scale(Point2D center, double factor) {
        return new DimensionElement(id, layerId, center.lerp(start, factor), center.lerp(end, factor), type, offsetMm * factor, customLabel);
    }

    @Override
    public CADElement copyWithNewId() {
        return new DimensionElement(UUID.randomUUID().toString(), layerId, start, end, type, offsetMm, customLabel);
    }
}
