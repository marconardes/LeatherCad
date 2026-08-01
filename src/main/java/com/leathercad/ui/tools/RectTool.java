package com.leathercad.ui.tools;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.model.DimensionElement;
import com.leathercad.core.model.Document;
import com.leathercad.core.model.RectElement;
import com.leathercad.ui.viewport.CameraTransform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class RectTool implements CADTool {
    private Point2D startPoint = null;
    private Point2D currentHover = null;

    @Override
    public String getName() { return "Retângulo"; }

    @Override
    public void onMousePressed(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {
        if (startPoint == null) {
            startPoint = worldPoint;
        } else {
            double x = Math.min(startPoint.x(), worldPoint.x());
            double y = Math.min(startPoint.y(), worldPoint.y());
            double w = Math.abs(worldPoint.x() - startPoint.x());
            double h = Math.abs(worldPoint.y() - startPoint.y());

            if (w > 0.1 && h > 0.1) {
                // 1. Elemento Retângulo
                Rect2D rect = new Rect2D(x, y, w, h);
                RectElement element = new RectElement(document.getActiveLayer().getId(), rect);
                document.addElement(element);

                // 2. Cotas Automáticas (Largura e Altura em mm)
                Point2D topLeft = new Point2D(x, y);
                Point2D topRight = new Point2D(x + w, y);
                Point2D bottomRight = new Point2D(x + w, y + h);

                DimensionElement dimH = new DimensionElement(
                    document.getActiveLayer().getId(),
                    topLeft, topRight,
                    DimensionElement.DimensionType.HORIZONTAL,
                    5.0
                );

                DimensionElement dimV = new DimensionElement(
                    document.getActiveLayer().getId(),
                    topRight, bottomRight,
                    DimensionElement.DimensionType.VERTICAL,
                    5.0
                );

                document.addElement(dimH);
                document.addElement(dimV);
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

            // Rótulo Dinâmico de Dimensões no Canvas
            double realW = Math.abs(currentHover.x() - startPoint.x());
            double realH = Math.abs(currentHover.y() - startPoint.y());
            String dimText = String.format("L: %.2f mm  x  A: %.2f mm", realW, realH);

            gc.setFill(Color.web("#00FF88"));
            gc.setFont(Font.font("Consolas", 12));
            gc.fillText(dimText, x + w / 2.0 - 50, y - 10);
        }
    }
}
