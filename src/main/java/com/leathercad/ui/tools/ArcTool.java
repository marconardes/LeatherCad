package com.leathercad.ui.tools;

import com.leathercad.core.geometry.Arc2D;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.model.ArcElement;
import com.leathercad.core.model.Document;
import com.leathercad.core.snap.SnapEngine;
import com.leathercad.ui.viewport.CameraTransform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.Font;

import java.util.Optional;

public class ArcTool implements CADTool {
    private Point2D startPoint = null; // P0
    private Point2D endPoint = null;   // P1
    private Point2D currentHover = null;
    private Point2D currentEffective = null;
    private boolean isShiftPressed = false;
    private boolean isSnapActive = false;

    @Override
    public String getName() {
        return "Arco";
    }

    @Override
    public void onMousePressed(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {
        if (event.getButton() == MouseButton.SECONDARY) {
            reset();
            return;
        }

        boolean isSnap = SnapEngine.isGeometricSnap(worldPoint, document, 8.0 / camera.getZoom());
        boolean useOrtho = event.isShiftDown() || isShiftPressed;

        if (startPoint == null) {
            startPoint = getEffectivePoint(null, worldPoint, false, isSnap);
        } else if (endPoint == null) {
            Point2D p1 = getEffectivePoint(startPoint, worldPoint, useOrtho, isSnap);
            if (startPoint.distanceTo(p1) > 0.1) {
                endPoint = p1;
            }
        } else {
            Point2D p2 = calculateEffectiveP2(worldPoint, useOrtho, isSnap);
            Optional<Arc2D> optArc = Arc2D.fromThreePoints(startPoint, endPoint, p2);
            if (optArc.isPresent()) {
                Arc2D arc = optArc.get();
                ArcElement element = new ArcElement(document.getActiveLayer().getId(), arc);
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

        if (startPoint != null && endPoint == null) {
            currentEffective = getEffectivePoint(startPoint, worldPoint, isShiftPressed, isSnapActive);
        } else if (startPoint != null && endPoint != null) {
            currentEffective = calculateEffectiveP2(worldPoint, isShiftPressed, isSnapActive);
        } else {
            currentEffective = getEffectivePoint(null, worldPoint, false, isSnapActive);
        }
    }

    private Point2D calculateEffectiveP2(Point2D worldPoint, boolean useOrtho, boolean isSnap) {
        if (isSnap || !useOrtho || startPoint == null || endPoint == null) {
            return worldPoint;
        }

        double dx = endPoint.x() - startPoint.x();
        double dy = endPoint.y() - startPoint.y();
        double len = Math.hypot(dx, dy);
        if (len < 1e-4) {
            return worldPoint;
        }

        // Vetor normal à corda P0-P1
        double nx = -dy / len;
        double ny = dx / len;

        // Ponto médio da corda P0-P1
        double mx = (startPoint.x() + endPoint.x()) / 2.0;
        double my = (startPoint.y() + endPoint.y()) / 2.0;

        // Projeção ortogonal do mouse sobre a reta mediatriz
        double t = (worldPoint.x() - mx) * nx + (worldPoint.y() - my) * ny;
        return new Point2D(mx + t * nx, my + t * ny);
    }

    @Override
    public void renderOverlay(GraphicsContext gc, CameraTransform camera) {
        if (startPoint == null || currentHover == null) return;

        Point2D previewPoint = currentEffective != null ? currentEffective : currentHover;
        Point2D mouseSc = camera.worldToScreen(currentHover);

        if (endPoint == null) {
            // Etapa 1: P0 fixado, definindo o ponto final P1 (linha de corda)
            Point2D p0Sc = camera.worldToScreen(startPoint);
            Point2D p1Sc = camera.worldToScreen(previewPoint);

            // Marcador P0
            drawPointMarker(gc, p0Sc, "#00A8FF");

            // Linha guia de corda pontilhada
            gc.setStroke(Color.web("#00A8FF"));
            gc.setLineWidth(1.5);
            gc.setLineDashes(4.0);
            gc.strokeLine(p0Sc.x(), p0Sc.y(), p1Sc.x(), p1Sc.y());
            gc.setLineDashes(null);

            double chordLength = startPoint.distanceTo(previewPoint);
            double angleDeg = Math.toDegrees(Math.atan2(previewPoint.y() - startPoint.y(), previewPoint.x() - startPoint.x()));
            if (angleDeg < 0) angleDeg += 360;

            String orthoTag = (isShiftPressed && !isSnapActive) ? " [ORTHO 🔒]" : "";
            String dimText = String.format("Corda: %.2f mm | Ângulo: %.2f°%s", chordLength, angleDeg, orthoTag);

            gc.setFill(Color.web("#00A8FF"));
            gc.setFont(Font.font("Consolas", 12));
            gc.fillText(dimText, mouseSc.x() + 12, mouseSc.y() - 6);
        } else {
            // Etapa 2: P0 e P1 fixados, definindo P2 (ponto no arco)
            Point2D p0Sc = camera.worldToScreen(startPoint);
            Point2D p1Sc = camera.worldToScreen(endPoint);
            Point2D p2Sc = camera.worldToScreen(previewPoint);

            drawPointMarker(gc, p0Sc, "#00A8FF");
            drawPointMarker(gc, p1Sc, "#00A8FF");

            Optional<Arc2D> optArc = Arc2D.fromThreePoints(startPoint, endPoint, previewPoint);

            if (optArc.isEmpty()) {
                // Pontos colineares: renderiza linha reta P0-P1
                gc.setStroke(Color.web("#FF5555"));
                gc.setLineWidth(1.5);
                gc.setLineDashes(4.0);
                gc.strokeLine(p0Sc.x(), p0Sc.y(), p1Sc.x(), p1Sc.y());
                gc.setLineDashes(null);

                gc.setFill(Color.web("#FF5555"));
                gc.setFont(Font.font("Consolas", 12));
                gc.fillText("[Pontos Colineares]", mouseSc.x() + 12, mouseSc.y() - 6);
            } else {
                Arc2D arc = optArc.get();
                Point2D centerSc = camera.worldToScreen(arc.center());
                double screenRadius = camera.worldToScreenLength(arc.radius());

                // Marcador de Centro (+)
                gc.setStroke(Color.web("#00A8FF"));
                gc.setLineWidth(1.0);
                gc.strokeLine(centerSc.x() - 5, centerSc.y(), centerSc.x() + 5, centerSc.y());
                gc.strokeLine(centerSc.x(), centerSc.y() - 5, centerSc.x(), centerSc.y() + 5);

                // Linhas radiais sutis do centro aos pontos
                gc.setStroke(Color.web("#00A8FF", 0.4));
                gc.setLineDashes(2.0);
                gc.strokeLine(centerSc.x(), centerSc.y(), p0Sc.x(), p0Sc.y());
                gc.strokeLine(centerSc.x(), centerSc.y(), p1Sc.x(), p1Sc.y());
                gc.strokeLine(centerSc.x(), centerSc.y(), p2Sc.x(), p2Sc.y());

                // Curva do Arco Pontilhada
                gc.setStroke(Color.web("#00A8FF"));
                gc.setLineWidth(2.0);
                gc.setLineDashes(4.0);
                gc.strokeArc(
                    centerSc.x() - screenRadius,
                    centerSc.y() - screenRadius,
                    screenRadius * 2,
                    screenRadius * 2,
                    -arc.startAngleDegrees(),
                    -arc.sweepAngleDegrees(),
                    ArcType.OPEN
                );
                gc.setLineDashes(null);

                String orthoTag = (isShiftPressed && !isSnapActive) ? " [ORTHO SIMÉTRICO 🔒]" : "";
                String dimText = String.format("R: %.2f mm | Varredura: %.2f° | Comp: %.2f mm%s",
                    arc.radius(), Math.abs(arc.sweepAngleDegrees()), arc.arcLength(), orthoTag);

                gc.setFill(Color.web("#00A8FF"));
                gc.setFont(Font.font("Consolas", 12));
                gc.fillText(dimText, mouseSc.x() + 12, mouseSc.y() - 6);
            }
        }
    }

    private void drawPointMarker(GraphicsContext gc, Point2D sc, String hexColor) {
        gc.setFill(Color.web(hexColor));
        gc.fillOval(sc.x() - 3, sc.y() - 3, 6, 6);
    }

    @Override
    public void reset() {
        startPoint = null;
        endPoint = null;
        currentHover = null;
        currentEffective = null;
        isShiftPressed = false;
        isSnapActive = false;
    }
}
