package com.leathercad.ui.tools;

import com.leathercad.core.geometry.LineSegment;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.leather.StitchConfig;
import com.leathercad.core.leather.StitchElement;
import com.leathercad.core.model.Document;
import com.leathercad.core.model.LineElement;
import com.leathercad.ui.viewport.CameraTransform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;

public class StitchTool implements CADTool {
    private Point2D startPoint = null;
    private Point2D currentHover = null;
    private StitchConfig config = StitchConfig.DEFAULT_FRENCH;

    public void setConfig(StitchConfig config) {
        this.config = config;
    }

    @Override
    public String getName() { return "Costura Parametrizada"; }

    @Override
    public void onMousePressed(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {
        if (startPoint == null) {
            startPoint = worldPoint;
        } else {
            if (startPoint.distanceTo(worldPoint) > 0.1) {
                LineSegment baseLine = new LineSegment(startPoint, worldPoint);
                StitchElement stitch = new StitchElement(document.getStitchLayerId(), baseLine, config);
                document.addElement(stitch);
            }
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

            gc.setStroke(javafx.scene.paint.Color.web("#FFD700"));
            gc.setLineWidth(1.5);
            gc.setLineDashes(4.0);
            gc.strokeLine(s.x(), s.y(), e.x(), e.y());
            gc.setLineDashes(null);
        }
    }
}
