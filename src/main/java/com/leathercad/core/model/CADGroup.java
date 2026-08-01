package com.leathercad.core.model;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record CADGroup(
    String id,
    String layerId,
    String name,
    List<CADElement> children
) implements CADElement {

    public CADGroup {
        children = Collections.unmodifiableList(new ArrayList<>(children));
    }

    public CADGroup(String layerId, String name, List<CADElement> children) {
        this(UUID.randomUUID().toString(), layerId, name, children);
    }

    @Override
    public boolean containsPoint(Point2D p, double toleranceMm) {
        for (CADElement child : children) {
            if (child.containsPoint(p, toleranceMm)) {
                return true;
            }
        }
        return boundingBox().contains(p);
    }

    @Override
    public Rect2D boundingBox() {
        if (children.isEmpty()) return new Rect2D(0, 0, 0, 0);

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

        for (CADElement child : children) {
            Rect2D b = child.boundingBox();
            minX = Math.min(minX, b.minPoint().x());
            minY = Math.min(minY, b.minPoint().y());
            maxX = Math.max(maxX, b.minPoint().x() + b.width());
            maxY = Math.max(maxY, b.minPoint().y() + b.height());
        }

        return new Rect2D(new Point2D(minX, minY), Math.max(1.0, maxX - minX), Math.max(1.0, maxY - minY));
    }

    @Override
    public CADElement translate(double dx, double dy) {
        List<CADElement> movedChildren = new ArrayList<>();
        for (CADElement child : children) {
            movedChildren.add(child.translate(dx, dy));
        }
        return new CADGroup(id, layerId, name, movedChildren);
    }

    @Override
    public CADElement rotate(Point2D center, double angleDegrees) {
        List<CADElement> rotatedChildren = new ArrayList<>();
        for (CADElement child : children) {
            rotatedChildren.add(child.rotate(center, angleDegrees));
        }
        return new CADGroup(id, layerId, name, rotatedChildren);
    }

    @Override
    public CADElement mirror(Point2D axisStart, Point2D axisEnd) {
        List<CADElement> mirroredChildren = new ArrayList<>();
        for (CADElement child : children) {
            mirroredChildren.add(child.mirror(axisStart, axisEnd));
        }
        return new CADGroup(id, layerId, name, mirroredChildren);
    }

    @Override
    public CADElement scale(Point2D center, double factor) {
        List<CADElement> scaledChildren = new ArrayList<>();
        for (CADElement child : children) {
            scaledChildren.add(child.scale(center, factor));
        }
        return new CADGroup(id, layerId, name, scaledChildren);
    }

    @Override
    public CADElement copyWithNewId() {
        List<CADElement> copiedChildren = new ArrayList<>();
        for (CADElement child : children) {
            copiedChildren.add(child.copyWithNewId());
        }
        return new CADGroup(UUID.randomUUID().toString(), layerId, name, copiedChildren);
    }
}
