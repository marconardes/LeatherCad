package com.leathercad.core.model;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public interface CADElement {
    String id();
    String layerId();
    boolean containsPoint(Point2D p, double toleranceMm);
    Rect2D boundingBox();
    CADElement translate(double dx, double dy);
    CADElement rotate(Point2D center, double angleDegrees);
    CADElement mirror(Point2D axisStart, Point2D axisEnd);
    CADElement scale(Point2D center, double factor);
    CADElement copyWithNewId();

    default List<SubElementRef> getSubElements() {
        return Collections.emptyList();
    }

    default Optional<SubElementRef> findSubElementAt(Point2D p, double toleranceMm) {
        return Optional.empty();
    }

    default CADElement createOffset(Point2D cursorPoint, double distanceMm, String targetLayerId) {
        return copyWithNewId();
    }
}
