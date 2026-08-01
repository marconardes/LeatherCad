package com.leathercad.ui.tools;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.model.Document;
import com.leathercad.ui.viewport.CameraTransform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;

public interface CADTool {
    String getName();
    void onMousePressed(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera);
    void onMouseDragged(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera);
    void onMouseReleased(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera);
    void onMouseMoved(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera);
    void renderOverlay(GraphicsContext gc, CameraTransform camera);
    default void reset() {}

    default Point2D getEffectivePoint(Point2D basePoint, Point2D targetPoint, boolean isShiftDown, boolean isGeometricSnap) {
        if (isGeometricSnap || basePoint == null) {
            return targetPoint;
        }
        if (isShiftDown) {
            double dx = Math.abs(targetPoint.x() - basePoint.x());
            double dy = Math.abs(targetPoint.y() - basePoint.y());
            if (dx > dy) {
                return new Point2D(targetPoint.x(), basePoint.y());
            } else {
                return new Point2D(basePoint.x(), targetPoint.y());
            }
        }
        return targetPoint;
    }
}

