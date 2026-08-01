package com.leathercad.ui.tools;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.model.Document;
import com.leathercad.core.model.RectElement;
import com.leathercad.core.snap.SnapEngine;
import com.leathercad.ui.viewport.CameraTransform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class RectTool implements CADTool {
    private Point2D startPoint = null;
    private Point2D currentHover = null;
    private Point2D currentEffective = null;
    private boolean isShiftPressed = false;
    private boolean isSnapActive = false;

    @Override
    public String getName() { return "Retângulo"; }

    @Override
    public Point2D getEffectivePoint(Point2D basePoint, Point2D targetPoint, boolean isShiftDown, boolean isGeometricSnap) {
        if (isGeometricSnap || basePoint == null) {
            return targetPoint;
        }
        if (isShiftDown) {
            double dx = targetPoint.x() - basePoint.x();
            double dy = targetPoint.y() - basePoint.y();
            double maxDist = Math.max(Math.abs(dx), Math.abs(dy));
            double signX = dx >= 0 ? 1.0 : -1.0;
            double signY = dy >= 0 ? 1.0 : -1.0;
            return new Point2D(basePoint.x() + signX * maxDist, basePoint.y() + signY * maxDist);
        }
        return targetPoint;
    }

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

            double x = Math.min(startPoint.x(), effectivePoint.x());
            double y = Math.min(startPoint.y(), effectivePoint.y());
            double w = Math.abs(effectivePoint.x() - startPoint.x());
            double h = Math.abs(effectivePoint.y() - startPoint.y());

            if (w > 0.1 && h > 0.1) {
                Rect2D rect = new Rect2D(x, y, w, h);
                RectElement element = new RectElement(document.getActiveLayer().getId(), rect);
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
        currentEffective = getEffectivePoint(startPoint, worldPoint, isShiftPressed, isSnapActive);
    }

    @Override
    public void renderOverlay(GraphicsContext gc, CameraTransform camera) {
        if (startPoint != null && currentHover != null) {
            Point2D previewPoint = currentEffective != null ? currentEffective : currentHover;

            Point2D s = camera.worldToScreen(startPoint);
            Point2D e = camera.worldToScreen(previewPoint);

            double x = Math.min(s.x(), e.x());
            double y = Math.min(s.y(), e.y());
            double w = Math.abs(e.x() - s.x());
            double h = Math.abs(e.y() - s.y());

            // Preview Retângulo
            gc.setStroke(Color.web("#00FF88"));
            gc.setLineWidth(1.5);
            gc.setLineDashes(4.0);
            gc.strokeRect(x, y, w, h);
            gc.setLineDashes(null);

            // Cotas dinâmicas no HUD (Largura x Altura + Tag Ortho Quadrado)
            double realW = Math.abs(previewPoint.x() - startPoint.x());
            double realH = Math.abs(previewPoint.y() - startPoint.y());
            String orthoTag = (isShiftPressed && !isSnapActive) ? " [QUADRADO 🔒]" : "";
            String dimText = String.format("L: %.2f mm | A: %.2f mm%s", realW, realH, orthoTag);

            // Renderização do Badge HUD escuro com borda verde neon
            Point2D rawMouseSc = camera.worldToScreen(currentHover);
            double badgeX = rawMouseSc.x() + 15;
            double badgeY = rawMouseSc.y() - 25;

            Font font = Font.font("Consolas", 12);
            gc.setFont(font);
            double textWidth = dimText.length() * 7.2 + 16;
            double textHeight = 22;

            gc.setFill(Color.rgb(20, 24, 30, 0.85));
            gc.fillRoundRect(badgeX, badgeY - 14, textWidth, textHeight, 6, 6);

            gc.setStroke(Color.web("#00FF88"));
            gc.setLineWidth(1.0);
            gc.strokeRoundRect(badgeX, badgeY - 14, textWidth, textHeight, 6, 6);

            gc.setFill(Color.WHITE);
            gc.fillText(dimText, badgeX + 8, badgeY + 1);
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
