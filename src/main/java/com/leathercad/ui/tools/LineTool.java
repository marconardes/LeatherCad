package com.leathercad.ui.tools;

import com.leathercad.core.geometry.LineSegment;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.model.DimensionElement;
import com.leathercad.core.model.Document;
import com.leathercad.core.model.LineElement;
import com.leathercad.core.snap.SnapEngine;
import com.leathercad.ui.viewport.CameraTransform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class LineTool implements CADTool {
    private Point2D startPoint = null;
    private Point2D currentHover = null;
    private Point2D currentEffective = null;
    private boolean isShiftPressed = false;
    private boolean isSnapActive = false;

    @Override
    public String getName() { return "Linha"; }

    @Override
    public void onMousePressed(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {
        if (event.getButton() == MouseButton.SECONDARY) {
            reset();
            return;
        }

        if (startPoint == null) {
            startPoint = worldPoint;
        } else {
            boolean useOrtho = event.isShiftDown() || isShiftPressed;
            boolean isSnap = SnapEngine.isGeometricSnap(worldPoint, document, 8.0 / camera.getZoom());
            Point2D effectivePoint = getEffectivePoint(startPoint, worldPoint, useOrtho, isSnap);

            if (startPoint.distanceTo(effectivePoint) > 0.1) {
                LineSegment line = new LineSegment(startPoint, effectivePoint);
                LineElement element = new LineElement(document.getActiveLayer().getId(), line);
                document.addElement(element);

                DimensionElement dim = new DimensionElement(
                    document.getActiveLayer().getId(),
                    startPoint, effectivePoint,
                    DimensionElement.DimensionType.HORIZONTAL,
                    4.0
                );
                document.addElement(dim);
            }
            startPoint = null;
            currentHover = null;
            currentEffective = null;
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
        currentEffective = getEffectivePoint(startPoint, worldPoint, isShiftPressed, isSnapActive);
    }

    @Override
    public void renderOverlay(GraphicsContext gc, CameraTransform camera) {
        if (startPoint != null && currentHover != null) {
            Point2D previewPoint = currentEffective != null ? currentEffective : currentHover;

            Point2D s = camera.worldToScreen(startPoint);
            Point2D e = camera.worldToScreen(previewPoint);

            gc.setStroke(Color.web("#00A8FF"));
            gc.setLineWidth(1.5);
            gc.setLineDashes(4.0);
            gc.strokeLine(s.x(), s.y(), e.x(), e.y());
            gc.setLineDashes(null);

            double realLength = startPoint.distanceTo(previewPoint);
            String orthoTag = (isShiftPressed && !isSnapActive) ? " [ORTHO 🔒]" : "";
            String dimText = String.format("Comp: %.2f mm%s", realLength, orthoTag);

            Point2D rawMouseSc = camera.worldToScreen(currentHover);

            gc.setFill(Color.web("#00A8FF"));
            gc.setFont(Font.font("Consolas", 12));
            gc.fillText(dimText, rawMouseSc.x() + 10, rawMouseSc.y() - 5);
        }
    }

    @Override
    public void reset() {
        startPoint = null;
        currentHover = null;
        currentEffective = null;
        isShiftPressed = false;
        isSnapActive = false;
    }
}

