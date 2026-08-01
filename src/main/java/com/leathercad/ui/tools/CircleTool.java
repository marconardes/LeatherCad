package com.leathercad.ui.tools;

import com.leathercad.core.geometry.Circle2D;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.model.CircleElement;
import com.leathercad.core.model.Document;
import com.leathercad.core.snap.SnapEngine;
import com.leathercad.ui.viewport.CameraTransform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class CircleTool implements CADTool {
    private Point2D centerPoint = null;
    private Point2D currentHover = null;
    private Point2D currentEffective = null;
    private boolean isShiftPressed = false;
    private boolean isSnapActive = false;

    @Override
    public String getName() {
        return "Círculo";
    }

    @Override
    public void onMousePressed(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {
        if (event.getButton() == MouseButton.SECONDARY) {
            reset();
            return;
        }

        boolean useOrtho = (event.isShiftDown() || isShiftPressed) && centerPoint != null;
        boolean isSnap = SnapEngine.isGeometricSnap(worldPoint, document, 8.0 / camera.getZoom());
        Point2D effectivePoint = getEffectivePoint(centerPoint, worldPoint, useOrtho, isSnap);

        if (centerPoint == null) {
            centerPoint = effectivePoint;
        } else {
            double radius = centerPoint.distanceTo(effectivePoint);
            if (radius > 0.1) {
                Circle2D circle = new Circle2D(centerPoint, radius);
                CircleElement element = new CircleElement(document.getActiveLayer().getId(), circle);
                document.addElement(element);
            }
            reset();
        }
    }

    @Override
    public void onMouseDragged(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {
        updateHoverState(event, worldPoint, document, camera);
    }

    @Override
    public void onMouseReleased(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {}

    @Override
    public void onMouseMoved(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {
        updateHoverState(event, worldPoint, document, camera);
    }

    private void updateHoverState(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {
        isShiftPressed = event.isShiftDown();
        currentHover = worldPoint;
        isSnapActive = SnapEngine.isGeometricSnap(worldPoint, document, 8.0 / camera.getZoom());
        currentEffective = getEffectivePoint(centerPoint, worldPoint, isShiftPressed, isSnapActive);
    }

    @Override
    public void renderOverlay(GraphicsContext gc, CameraTransform camera) {
        if (centerPoint != null && currentHover != null) {
            Point2D previewPoint = currentEffective != null ? currentEffective : currentHover;

            Point2D c = camera.worldToScreen(centerPoint);
            Point2D e = camera.worldToScreen(previewPoint);
            Point2D h = camera.worldToScreen(currentHover);

            double realRadius = centerPoint.distanceTo(previewPoint);
            double screenRadius = camera.worldToScreenLength(realRadius);

            // Marcador de Centro (+)
            gc.setStroke(Color.web("#FFD700"));
            gc.setLineWidth(1.5);
            gc.strokeLine(c.x() - 6, c.y(), c.x() + 6, c.y());
            gc.strokeLine(c.x(), c.y() - 6, c.x(), c.y() + 6);

            // Circunferência e Linha Guia pontilhadas
            gc.setLineDashes(4.0);
            gc.strokeOval(c.x() - screenRadius, c.y() - screenRadius, screenRadius * 2, screenRadius * 2);
            gc.strokeLine(c.x(), c.y(), e.x(), e.y());
            gc.setLineDashes(null);

            // Rótulo de Cota Dinâmica junto ao Cursor
            String orthoTag = (isShiftPressed && !isSnapActive) ? " [ORTHO 🔒]" : "";
            String dimText = String.format("R: %.2f mm (Ø: %.2f mm)%s", realRadius, realRadius * 2, orthoTag);

            gc.setFill(Color.web("#FFD700"));
            gc.setFont(Font.font("Consolas", 12));
            gc.fillText(dimText, h.x() + 12, h.y() - 6);
        }
    }

    @Override
    public void reset() {
        centerPoint = null;
        currentHover = null;
        currentEffective = null;
        isShiftPressed = false;
        isSnapActive = false;
    }
}
