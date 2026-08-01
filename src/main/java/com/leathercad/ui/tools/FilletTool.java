package com.leathercad.ui.tools;

import com.leathercad.core.geometry.Arc2D;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.model.*;
import com.leathercad.ui.viewport.CameraTransform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.Optional;

public class FilletTool implements CADTool {

    private double radiusMm = 5.0;

    private CADElement hoveredElement = null;
    private int hoveredVertexIndex = -1;
    private Point2D hoveredVertexPoint = null;
    private Arc2D.FilletResult currentFilletResult = null;

    public FilletTool() {}

    public FilletTool(double defaultRadiusMm) {
        this.radiusMm = defaultRadiusMm;
    }

    public double getRadiusMm() {
        return radiusMm;
    }

    public void setRadiusMm(double radiusMm) {
        if (radiusMm > 0) {
            this.radiusMm = radiusMm;
        }
    }

    @Override
    public String getName() {
        return "Arredondar Cantos (Fillet)";
    }

    @Override
    public void onMousePressed(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {
        if (hoveredElement != null && hoveredVertexIndex >= 0) {
            boolean success = document.applyFilletAtVertex(hoveredElement, hoveredVertexIndex, radiusMm);
            if (success) {
                clearHover();
                onMouseMoved(event, worldPoint, document, camera);
            }
        }
    }

    @Override
    public void onMouseDragged(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {
        onMouseMoved(event, worldPoint, document, camera);
    }

    @Override
    public void onMouseReleased(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {}

    @Override
    public void onMouseMoved(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {
        clearHover();

        double toleranceMm = 12.0 / camera.getZoom();
        for (CADElement elem : document.getElements()) {
            Layer layer = document.findLayerById(elem.layerId());
            if (layer != null && (layer.isLocked() || !layer.isVisible())) continue;

            Optional<SubElementRef> subRef = elem.findSubElementAt(worldPoint, toleranceMm);
            if (subRef.isPresent() && subRef.get().type() == SubElementRef.SubElementType.VERTEX) {
                hoveredElement = elem;
                hoveredVertexIndex = subRef.get().index();
                hoveredVertexPoint = getVertexPoint(elem, hoveredVertexIndex);
                computePreviewFillet();
                break;
            }
        }
    }

    private Point2D getVertexPoint(CADElement elem, int index) {
        if (elem instanceof RectElement rectElem) {
            Rect2D r = rectElem.rect();
            Point2D min = r.minPoint();
            return switch (index) {
                case 0 -> min; // TL
                case 1 -> new Point2D(min.x() + r.width(), min.y()); // TR
                case 2 -> new Point2D(min.x() + r.width(), min.y() + r.height()); // BR
                case 3 -> new Point2D(min.x(), min.y() + r.height()); // BL
                default -> min;
            };
        } else if (elem instanceof PolylineElement polyElem) {
            var pts = polyElem.polyline().points();
            if (index >= 0 && index < pts.size()) return pts.get(index);
        } else if (elem instanceof LineElement lineElem) {
            return (index == 0) ? lineElem.line().start() : lineElem.line().end();
        }
        return null;
    }

    private void computePreviewFillet() {
        if (hoveredElement == null || hoveredVertexIndex < 0 || hoveredVertexPoint == null) {
            currentFilletResult = null;
            return;
        }

        if (hoveredElement instanceof RectElement rectElem) {
            Rect2D r = rectElem.rect();
            Point2D min = r.minPoint();
            Point2D pA, pB;
            switch (hoveredVertexIndex) {
                case 0 -> { // TL
                    pA = new Point2D(min.x(), min.y() + r.height());
                    pB = new Point2D(min.x() + r.width(), min.y());
                }
                case 1 -> { // TR
                    pA = new Point2D(min.x(), min.y());
                    pB = new Point2D(min.x() + r.width(), min.y() + r.height());
                }
                case 2 -> { // BR
                    pA = new Point2D(min.x() + r.width(), min.y());
                    pB = new Point2D(min.x(), min.y() + r.height());
                }
                case 3 -> { // BL
                    pA = new Point2D(min.x() + r.width(), min.y() + r.height());
                    pB = new Point2D(min.x(), min.y());
                }
                default -> { pA = null; pB = null; }
            }
            if (pA != null && pB != null) {
                currentFilletResult = Arc2D.calculateFillet(pA, hoveredVertexPoint, pB, radiusMm).orElse(null);
            }
        } else if (hoveredElement instanceof PolylineElement polyElem) {
            var pts = polyElem.polyline().points();
            int numPts = pts.size();
            boolean isClosed = polyElem.polyline().isClosed();
            if (numPts >= 2 && (isClosed || (hoveredVertexIndex > 0 && hoveredVertexIndex < numPts - 1))) {
                Point2D pA = (hoveredVertexIndex > 0) ? pts.get(hoveredVertexIndex - 1) : pts.get(numPts - 1);
                Point2D pB = (hoveredVertexIndex < numPts - 1) ? pts.get(hoveredVertexIndex + 1) : pts.get(0);
                currentFilletResult = Arc2D.calculateFillet(pA, hoveredVertexPoint, pB, radiusMm).orElse(null);
            }
        }
    }

    private void clearHover() {
        hoveredElement = null;
        hoveredVertexIndex = -1;
        hoveredVertexPoint = null;
        currentFilletResult = null;
    }

    @Override
    public void renderOverlay(GraphicsContext gc, CameraTransform camera) {
        if (hoveredVertexPoint == null) return;

        Point2D vScreen = camera.worldToScreen(hoveredVertexPoint);

        // Círculo magnético do vértice selecionado
        gc.setStroke(Color.web("#FF9800"));
        gc.setLineWidth(2.0);
        gc.strokeOval(vScreen.x() - 9, vScreen.y() - 9, 18, 18);

        gc.setFill(Color.web("#FF9800", 0.35));
        gc.fillOval(vScreen.x() - 6, vScreen.y() - 6, 12, 12);

        if (currentFilletResult != null) {
            Point2D tAScreen = camera.worldToScreen(currentFilletResult.tA());
            Point2D tBScreen = camera.worldToScreen(currentFilletResult.tB());

            // Linhas de corte descartado
            gc.setStroke(Color.web("#FF5252"));
            gc.setLineWidth(1.5);
            gc.strokeLine(vScreen.x(), vScreen.y(), tAScreen.x(), tAScreen.y());
            gc.strokeLine(vScreen.x(), vScreen.y(), tBScreen.x(), tBScreen.y());

            // Arco de preview (verde vivo)
            Arc2D arc = currentFilletResult.arc();
            Point2D centerScreen = camera.worldToScreen(arc.center());
            double rScreen = arc.radius() * camera.getZoom();

            gc.setStroke(Color.web("#00E676"));
            gc.setLineWidth(3.0);
            gc.strokeArc(
                centerScreen.x() - rScreen,
                centerScreen.y() - rScreen,
                rScreen * 2,
                rScreen * 2,
                arc.startAngleDegrees(),
                arc.sweepAngleDegrees(),
                javafx.scene.shape.ArcType.OPEN
            );

            // Badge com informação do Raio R
            String text = String.format("R: %.1f mm", currentFilletResult.effectiveRadius());
            gc.setFont(Font.font("Segoe UI", 12));
            gc.setFill(Color.web("#1E1E1E", 0.85));
            gc.fillRoundRect(vScreen.x() + 14, vScreen.y() - 22, 75, 22, 6, 6);
            gc.setFill(Color.WHITE);
            gc.fillText(text, vScreen.x() + 20, vScreen.y() - 7);
        }
    }

    @Override
    public void reset() {
        clearHover();
    }
}
