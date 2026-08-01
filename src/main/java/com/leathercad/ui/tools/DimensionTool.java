package com.leathercad.ui.tools;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.model.DimensionElement;
import com.leathercad.core.model.Document;
import com.leathercad.ui.viewport.CameraTransform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

public class DimensionTool implements CADTool {
    private final DimensionElement.DimensionType type;
    private Point2D startPoint = null;
    private Point2D currentHover = null;

    public DimensionTool(DimensionElement.DimensionType type) {
        this.type = type;
    }

    @Override
    public String getName() {
        return switch (type) {
            case HORIZONTAL -> "Cota Horizontal";
            case VERTICAL -> "Cota Vertical";
            case RADIUS -> "Cota Raio";
            case DIAMETER -> "Cota Diâmetro";
            case ANGULAR -> "Cota Angular";
        };
    }

    @Override
    public void onMousePressed(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {
        if (startPoint == null) {
            startPoint = worldPoint;
        } else {
            DimensionElement dim = new DimensionElement(document.getActiveLayer().getId(), startPoint, worldPoint, type, 5.0);
            document.addElement(dim);
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
        if (startPoint != null && currentHover != null) {
            Point2D s = camera.worldToScreen(startPoint);
            Point2D e = camera.worldToScreen(currentHover);

            gc.setStroke(Color.web("#00A8FF"));
            gc.setLineWidth(1.0);
            gc.setLineDashes(2.0);
            gc.strokeLine(s.x(), s.y(), e.x(), e.y());
            gc.setLineDashes(null);
        }
    }
}
