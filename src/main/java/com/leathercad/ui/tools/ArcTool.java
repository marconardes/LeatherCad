package com.leathercad.ui.tools;

import com.leathercad.core.geometry.Arc2D;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.model.ArcElement;
import com.leathercad.core.model.Document;
import com.leathercad.ui.viewport.CameraTransform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

public class ArcTool implements CADTool {
    private Point2D centerPoint = null;
    private Point2D startPoint = null;
    private Point2D currentHover = null;

    @Override
    public String getName() { return "Arco"; }

    @Override
    public void onMousePressed(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {
        if (centerPoint == null) {
            centerPoint = worldPoint;
        } else if (startPoint == null) {
            startPoint = worldPoint;
        } else {
            double radius = centerPoint.distanceTo(startPoint);
            double startAngle = Math.toDegrees(Math.atan2(startPoint.y() - centerPoint.y(), startPoint.x() - centerPoint.x()));
            double endAngle = Math.toDegrees(Math.atan2(worldPoint.y() - centerPoint.y(), worldPoint.x() - centerPoint.x()));
            double sweep = endAngle - startAngle;
            if (sweep < 0) sweep += 360;

            if (radius > 0.1) {
                Arc2D arc = new Arc2D(centerPoint, radius, startAngle, sweep);
                ArcElement element = new ArcElement(document.getActiveLayer().getId(), arc);
                document.addElement(element);
            }
            centerPoint = null;
            startPoint = null;
            currentHover = null;
        }
    }

    @Override
    public void onMouseDragged(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {
        currentHover = worldPoint;
    }

    @Override
    public void onMouseReleased(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {}

    @Override
    public void onMouseMoved(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {
        currentHover = worldPoint;
    }

    @Override
    public void renderOverlay(GraphicsContext gc, CameraTransform camera) {
        if (centerPoint != null && currentHover != null) {
            Point2D c = camera.worldToScreen(centerPoint);
            if (startPoint == null) {
                double r = camera.worldToScreenLength(centerPoint.distanceTo(currentHover));
                gc.setStroke(Color.web("#FF8C00"));
                gc.setLineWidth(1.5);
                gc.setLineDashes(4.0);
                gc.strokeOval(c.x() - r, c.y() - r, r * 2, r * 2);
                gc.setLineDashes(null);
            } else {
                double r = camera.worldToScreenLength(centerPoint.distanceTo(startPoint));
                gc.setStroke(Color.web("#FF8C00"));
                gc.setLineWidth(2.0);
                gc.strokeOval(c.x() - r, c.y() - r, r * 2, r * 2);
            }
        }
    }
}
