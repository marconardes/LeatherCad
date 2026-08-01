package com.leathercad.ui.tools;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Polyline2D;
import com.leathercad.core.model.Document;
import com.leathercad.core.model.PolylineElement;
import com.leathercad.core.snap.SnapEngine;
import com.leathercad.ui.viewport.CameraTransform;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.List;

public class PolylineTool implements CADTool {

    private final List<Point2D> currentPoints = new ArrayList<>();
    private Point2D currentHover = null;
    private Point2D currentEffective = null;
    private boolean isShiftPressed = false;
    private boolean isSnapActive = false;

    @Override
    public String getName() {
        return "Polilinha";
    }

    @Override
    public void onMousePressed(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {
        if (event.getButton() == MouseButton.SECONDARY || event.getClickCount() >= 2) {
            finishPolyline(document);
            return;
        }

        if (!currentPoints.isEmpty()) {
            Point2D start = currentPoints.getFirst();

            // 1. Fechamento de Molde em Loop (Clicar próximo ao ponto inicial <= 4.0mm com 3+ pontos)
            if (start.distanceTo(worldPoint) <= 4.0 && currentPoints.size() >= 3) {
                List<Point2D> cleaned = cleanDuplicatePoints(currentPoints);
                if (cleaned.size() >= 3) {
                    Polyline2D poly = new Polyline2D(cleaned, true);
                    document.addElement(new PolylineElement(document.getActiveLayer().getId(), poly));
                }
                currentPoints.clear();
                currentHover = null;
                currentEffective = null;
                return;
            }
        }

        boolean useOrtho = (event.isShiftDown() || isShiftPressed) && !currentPoints.isEmpty();
        Point2D base = currentPoints.isEmpty() ? null : currentPoints.getLast();
        boolean isSnap = SnapEngine.isGeometricSnap(worldPoint, document, 8.0 / camera.getZoom());
        Point2D effectivePoint = getEffectivePoint(base, worldPoint, useOrtho, isSnap);

        currentPoints.add(effectivePoint);
    }

    private List<Point2D> cleanDuplicatePoints(List<Point2D> pts) {
        List<Point2D> cleaned = new ArrayList<>();
        for (Point2D p : pts) {
            if (cleaned.isEmpty() || cleaned.getLast().distanceTo(p) > 1e-3) {
                cleaned.add(p);
            }
        }
        return cleaned;
    }

    private void finishPolyline(Document document) {
        List<Point2D> cleaned = cleanDuplicatePoints(currentPoints);
        if (cleaned.size() >= 2) {
            Polyline2D poly = new Polyline2D(cleaned, false);
            document.addElement(new PolylineElement(document.getActiveLayer().getId(), poly));
        }
        currentPoints.clear();
        currentHover = null;
        currentEffective = null;
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
        Point2D base = currentPoints.isEmpty() ? null : currentPoints.getLast();
        currentEffective = getEffectivePoint(base, worldPoint, isShiftPressed, isSnapActive);
    }

    @Override
    public void renderOverlay(GraphicsContext gc, CameraTransform camera) {
        if (currentPoints.isEmpty() || currentHover == null) return;

        Point2D lastPoint = currentPoints.getLast();
        Point2D previewPoint = currentEffective != null ? currentEffective : currentHover;

        double[] xPoints = new double[currentPoints.size() + 1];
        double[] yPoints = new double[currentPoints.size() + 1];

        for (int i = 0; i < currentPoints.size(); i++) {
            Point2D p = camera.worldToScreen(currentPoints.get(i));
            xPoints[i] = p.x();
            yPoints[i] = p.y();
        }

        Point2D hoverSc = camera.worldToScreen(previewPoint);
        xPoints[xPoints.length - 1] = hoverSc.x();
        yPoints[yPoints.length - 1] = hoverSc.y();

        gc.setStroke(Color.web("#00A8FF"));
        gc.setLineWidth(1.5);
        gc.setLineDashes(4.0);
        gc.strokePolyline(xPoints, yPoints, xPoints.length);
        gc.setLineDashes(null);

        // Pontos de ancoragem
        gc.setFill(Color.web("#00FF88"));
        for (int i = 0; i < currentPoints.size(); i++) {
            Point2D p = camera.worldToScreen(currentPoints.get(i));
            gc.fillOval(p.x() - 3, p.y() - 3, 6, 6);
        }

        // Mira do mouse REAL (fluida e sem travar a mão do artesão)
        Point2D rawMouseSc = camera.worldToScreen(currentHover);
        gc.setStroke(Color.web("#FFFFFF", 0.60));
        gc.setLineWidth(1.0);
        gc.strokeLine(rawMouseSc.x() - 6, rawMouseSc.y(), rawMouseSc.x() + 6, rawMouseSc.y());
        gc.strokeLine(rawMouseSc.x(), rawMouseSc.y() - 6, rawMouseSc.x(), rawMouseSc.y() + 6);

        double dist = lastPoint.distanceTo(previewPoint);
        String orthoTag = (isShiftPressed && !isSnapActive && !currentPoints.isEmpty()) ? " [ORTHO 🔒]" : "";
        gc.setFill(Color.web("#00A8FF"));
        gc.setFont(Font.font("Consolas", 11));
        gc.fillText(String.format("%.2f mm%s", dist, orthoTag), rawMouseSc.x() + 10, rawMouseSc.y() - 5);
    }

    @Override
    public void reset() {
        currentPoints.clear();
        currentHover = null;
        currentEffective = null;
        isShiftPressed = false;
        isSnapActive = false;
    }
}

