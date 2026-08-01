package com.leathercad.ui.tools;

import com.leathercad.core.geometry.Bezier2D;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.model.BezierElement;
import com.leathercad.core.model.Document;
import com.leathercad.core.snap.SnapEngine;
import com.leathercad.ui.viewport.CameraTransform;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class BezierTool implements CADTool {
    private Point2D p0 = null;
    private Point2D p3 = null;
    private Point2D p1 = null;
    private Point2D currentHover = null;
    private Point2D currentEffective = null;
    private boolean isShiftPressed = false;
    private boolean isSnapActive = false;

    @Override
    public String getName() {
        return "Curva Bézier";
    }

    @Override
    public void onMousePressed(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {
        if (event.getButton() == MouseButton.SECONDARY) {
            reset();
            return;
        }

        updateHoverState(event, worldPoint, document, camera);
        Point2D pt = currentEffective != null ? currentEffective : worldPoint;

        if (p0 == null) {
            p0 = pt;
        } else if (p3 == null) {
            if (p0.distanceTo(pt) > 0.1) {
                p3 = pt;
            }
        } else if (p1 == null) {
            p1 = pt;
        } else {
            Point2D p2 = pt;
            if (p0.distanceTo(p3) > 0.1) {
                Bezier2D bezier = new Bezier2D(p0, p1, p2, p3);
                BezierElement element = new BezierElement(document.getActiveLayer().getId(), bezier);
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

        Point2D basePoint = null;
        if (p0 == null) {
            basePoint = null;
        } else if (p3 == null) {
            basePoint = p0;
        } else if (p1 == null) {
            basePoint = p0;
        } else {
            basePoint = p3;
        }

        currentEffective = getEffectivePoint(basePoint, worldPoint, isShiftPressed, isSnapActive);
    }

    @Override
    public void renderOverlay(GraphicsContext gc, CameraTransform camera) {
        if (currentHover == null) return;
        Point2D effectiveTarget = currentEffective != null ? currentEffective : currentHover;
        Point2D rawMouseSc = camera.worldToScreen(currentHover);

        String orthoTag = (isShiftPressed && !isSnapActive && p0 != null) ? " [ORTHO 🔒]" : "";

        if (p0 == null) {
            // Etapa 1: Definir P0
            gc.setFill(Color.web("#E056FD"));
            gc.setFont(Font.font("Consolas", 11));
            gc.fillText("1/4: Clique no ponto inicial (P0)", rawMouseSc.x() + 10, rawMouseSc.y() - 5);
        } else if (p3 == null) {
            // Etapa 2: Definir P3 (Linha reta elástica P0 -> P3)
            Point2D s0 = camera.worldToScreen(p0);
            Point2D sTarget = camera.worldToScreen(effectiveTarget);

            gc.setStroke(Color.web("#00A8FF"));
            gc.setLineWidth(1.5);
            gc.setLineDashes(4.0);
            gc.strokeLine(s0.x(), s0.y(), sTarget.x(), sTarget.y());
            gc.setLineDashes(null);

            // Marcador de nó P0
            gc.setFill(Color.web("#00FF88"));
            gc.fillOval(s0.x() - 4, s0.y() - 4, 8, 8);

            gc.setFill(Color.web("#00A8FF"));
            gc.setFont(Font.font("Consolas", 11));
            gc.fillText("2/4: Clique no ponto final (P3)" + orthoTag, rawMouseSc.x() + 10, rawMouseSc.y() - 5);
        } else if (p1 == null) {
            // Etapa 3: Definir P1 (Alça 1). P2 provisório a 2/3 da reta P0-P3
            Point2D draftP1 = effectiveTarget;
            Point2D draftP2 = new Point2D(
                p0.x() + (p3.x() - p0.x()) * 2.0 / 3.0,
                p0.y() + (p3.y() - p0.y()) * 2.0 / 3.0
            );

            renderBezierPreview(gc, camera, p0, draftP1, draftP2, p3);

            gc.setFill(Color.web("#E056FD"));
            gc.setFont(Font.font("Consolas", 11));
            gc.fillText("3/4: Ajuste a 1ª alça de controle (P1)" + orthoTag, rawMouseSc.x() + 10, rawMouseSc.y() - 5);
        } else {
            // Etapa 4: Definir P2 (Alça 2)
            Point2D draftP2 = effectiveTarget;

            renderBezierPreview(gc, camera, p0, p1, draftP2, p3);

            gc.setFill(Color.web("#E056FD"));
            gc.setFont(Font.font("Consolas", 11));
            gc.fillText("4/4: Ajuste a 2ª alça de controle (P2)" + orthoTag, rawMouseSc.x() + 10, rawMouseSc.y() - 5);
        }
    }

    private void renderBezierPreview(GraphicsContext gc, CameraTransform camera, Point2D start, Point2D c1, Point2D c2, Point2D end) {
        Point2D s0 = camera.worldToScreen(start);
        Point2D s1 = camera.worldToScreen(c1);
        Point2D s2 = camera.worldToScreen(c2);
        Point2D s3 = camera.worldToScreen(end);

        // 1. Linhas de controle pontilhadas (P0 -> P1 e P3 -> P2)
        gc.setStroke(Color.web("#FF8C00"));
        gc.setLineWidth(1.2);
        gc.setLineDashes(3.0);
        gc.strokeLine(s0.x(), s0.y(), s1.x(), s1.y());
        gc.strokeLine(s3.x(), s3.y(), s2.x(), s2.y());
        gc.setLineDashes(null);

        // 2. Pontos de ancoragem (P0 e P3 em verde)
        gc.setFill(Color.web("#00FF88"));
        gc.fillOval(s0.x() - 4, s0.y() - 4, 8, 8);
        gc.fillOval(s3.x() - 4, s3.y() - 4, 8, 8);

        // 3. Alças de controle (P1 e P2 em laranja)
        gc.setFill(Color.web("#FF8C00"));
        gc.fillRect(s1.x() - 4, s1.y() - 4, 8, 8);
        gc.fillRect(s2.x() - 4, s2.y() - 4, 8, 8);

        // 4. Curva Bézier avaliada em tempo real (#E056FD)
        Bezier2D bezier = new Bezier2D(start, c1, c2, end);
        int steps = 30;
        double[] xPoints = new double[steps + 1];
        double[] yPoints = new double[steps + 1];

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Point2D pt = bezier.evaluate(t);
            Point2D sc = camera.worldToScreen(pt);
            xPoints[i] = sc.x();
            yPoints[i] = sc.y();
        }

        gc.setStroke(Color.web("#E056FD"));
        gc.setLineWidth(2.0);
        gc.strokePolyline(xPoints, yPoints, steps + 1);
    }

    @Override
    public void reset() {
        p0 = null;
        p3 = null;
        p1 = null;
        currentHover = null;
        currentEffective = null;
        isShiftPressed = false;
        isSnapActive = false;
    }
}

