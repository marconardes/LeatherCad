package com.leathercad.ui.tools;

import com.leathercad.core.geometry.Circle2D;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.model.CircleElement;
import com.leathercad.core.model.DimensionElement;
import com.leathercad.core.model.Document;
import com.leathercad.ui.viewport.CameraTransform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class CircleTool implements CADTool {
    private Point2D centerPoint = null;
    private Point2D currentHover = null;

    @Override
    public String getName() { return "Círculo"; }

    @Override
    public void onMousePressed(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {
        if (centerPoint == null) {
            centerPoint = worldPoint;
        } else {
            double radius = centerPoint.distanceTo(worldPoint);
            if (radius > 0.1) {
                // 1. Elemento Círculo
                Circle2D circle = new Circle2D(centerPoint, radius);
                CircleElement element = new CircleElement(document.getActiveLayer().getId(), circle);
                document.addElement(element);

                // 2. Cota Automática de Raio
                Point2D rimPoint = new Point2D(centerPoint.x() + radius, centerPoint.y());
                DimensionElement dimR = new DimensionElement(
                    document.getActiveLayer().getId(),
                    centerPoint, rimPoint,
                    DimensionElement.DimensionType.RADIUS,
                    2.0
                );
                document.addElement(dimR);
            }
            centerPoint = null;
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
            double r = camera.worldToScreenLength(centerPoint.distanceTo(currentHover));

            gc.setStroke(Color.web("#FFD700"));
            gc.setLineWidth(1.5);
            gc.setLineDashes(4.0);
            gc.strokeOval(c.x() - r, c.y() - r, r * 2, r * 2);
            gc.setLineDashes(null);

            double realRadius = centerPoint.distanceTo(currentHover);
            String dimText = String.format("R: %.2f mm (Ø: %.2f mm)", realRadius, realRadius * 2);

            gc.setFill(Color.web("#FFD700"));
            gc.setFont(Font.font("Consolas", 12));
            gc.fillText(dimText, c.x() - 40, c.y() - r - 10);
        }
    }
}
