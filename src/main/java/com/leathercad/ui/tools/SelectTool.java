package com.leathercad.ui.tools;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.model.*;
import com.leathercad.ui.viewport.CameraTransform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.util.Optional;

public class SelectTool implements CADTool {
    private Point2D boxStartPoint = null;
    private Point2D boxCurrentPoint = null;

    private boolean isDraggingSelection = false;
    private Point2D dragStartWorld = null;

    private boolean isDraggingNode = false;
    private String activeNodeElementId = null;
    private int activeHandleIndex = -1;
    private int nodeType = 0; // 1 = Bezier, 2 = Polyline, 3 = Line, 4 = Rect, 5 = Circle, 6 = Arc

    private Document currentDoc = null;

    @Override
    public String getName() { return "Seleção"; }

    @Override
    public void onMousePressed(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {
        this.currentDoc = document;
        double handleHitRadius = Math.max(2.5, 12.0 / camera.getZoom());
        double pickToleranceMm = Math.max(3.0, 14.0 / camera.getZoom());

        // 1. Hit-Testing de Grips de Controle (para qualquer elemento selecionado ou visível)
        for (CADElement elem : document.getElements()) {
            Layer layer = document.findLayerById(elem.layerId());
            if (layer != null && (!layer.isVisible() || layer.isLocked())) continue;

            if (elem instanceof BezierElement bezierElem) {
                var b = bezierElem.bezier();
                if (worldPoint.distanceTo(b.start()) <= handleHitRadius) {
                    document.selectElement(elem.id(), event.isControlDown() || event.isShiftDown());
                    startNodeDrag(elem.id(), 1, 1, worldPoint);
                    return;
                } else if (worldPoint.distanceTo(b.control1()) <= handleHitRadius) {
                    document.selectElement(elem.id(), event.isControlDown() || event.isShiftDown());
                    startNodeDrag(elem.id(), 2, 1, worldPoint);
                    return;
                } else if (worldPoint.distanceTo(b.control2()) <= handleHitRadius) {
                    document.selectElement(elem.id(), event.isControlDown() || event.isShiftDown());
                    startNodeDrag(elem.id(), 3, 1, worldPoint);
                    return;
                } else if (worldPoint.distanceTo(b.end()) <= handleHitRadius) {
                    document.selectElement(elem.id(), event.isControlDown() || event.isShiftDown());
                    startNodeDrag(elem.id(), 4, 1, worldPoint);
                    return;
                }
            } else if (elem instanceof PolylineElement polyElem) {
                var pts = polyElem.polyline().points();
                for (int i = 0; i < pts.size(); i++) {
                    if (worldPoint.distanceTo(pts.get(i)) <= handleHitRadius) {
                        document.selectElement(elem.id(), event.isControlDown() || event.isShiftDown());
                        startNodeDrag(elem.id(), i, 2, worldPoint);
                        return;
                    }
                }
            } else if (elem instanceof LineElement lineElem) {
                var line = lineElem.line();
                if (worldPoint.distanceTo(line.start()) <= handleHitRadius) {
                    document.selectElement(elem.id(), event.isControlDown() || event.isShiftDown());
                    startNodeDrag(elem.id(), 1, 3, worldPoint);
                    return;
                } else if (worldPoint.distanceTo(line.end()) <= handleHitRadius) {
                    document.selectElement(elem.id(), event.isControlDown() || event.isShiftDown());
                    startNodeDrag(elem.id(), 2, 3, worldPoint);
                    return;
                }
            } else if (elem instanceof RectElement rectElem) {
                var r = rectElem.rect();
                Point2D c0 = r.minPoint();
                Point2D c1 = new Point2D(r.minPoint().x() + r.width(), r.minPoint().y());
                Point2D c2 = new Point2D(r.minPoint().x() + r.width(), r.minPoint().y() + r.height());
                Point2D c3 = new Point2D(r.minPoint().x(), r.minPoint().y() + r.height());
                if (worldPoint.distanceTo(c0) <= handleHitRadius) {
                    document.selectElement(elem.id(), event.isControlDown() || event.isShiftDown());
                    startNodeDrag(elem.id(), 0, 4, worldPoint);
                    return;
                } else if (worldPoint.distanceTo(c1) <= handleHitRadius) {
                    document.selectElement(elem.id(), event.isControlDown() || event.isShiftDown());
                    startNodeDrag(elem.id(), 1, 4, worldPoint);
                    return;
                } else if (worldPoint.distanceTo(c2) <= handleHitRadius) {
                    document.selectElement(elem.id(), event.isControlDown() || event.isShiftDown());
                    startNodeDrag(elem.id(), 2, 4, worldPoint);
                    return;
                } else if (worldPoint.distanceTo(c3) <= handleHitRadius) {
                    document.selectElement(elem.id(), event.isControlDown() || event.isShiftDown());
                    startNodeDrag(elem.id(), 3, 4, worldPoint);
                    return;
                }
            } else if (elem instanceof CircleElement circleElem) {
                var c = circleElem.circle();
                Point2D center = c.center();
                double radius = c.radius();
                Point2D qTop = new Point2D(center.x(), center.y() - radius);
                Point2D qRight = new Point2D(center.x() + radius, center.y());
                Point2D qBottom = new Point2D(center.x(), center.y() + radius);
                Point2D qLeft = new Point2D(center.x() - radius, center.y());

                if (worldPoint.distanceTo(center) <= handleHitRadius) {
                    document.selectElement(elem.id(), event.isControlDown() || event.isShiftDown());
                    startNodeDrag(elem.id(), 0, 5, worldPoint);
                    return;
                } else if (worldPoint.distanceTo(qTop) <= handleHitRadius || worldPoint.distanceTo(qRight) <= handleHitRadius ||
                           worldPoint.distanceTo(qBottom) <= handleHitRadius || worldPoint.distanceTo(qLeft) <= handleHitRadius) {
                    document.selectElement(elem.id(), event.isControlDown() || event.isShiftDown());
                    startNodeDrag(elem.id(), 1, 5, worldPoint);
                    return;
                }
            } else if (elem instanceof ArcElement arcElem) {
                var a = arcElem.arc();
                Point2D center = a.center();
                Point2D startP = a.startPoint();
                Point2D endP = a.endPoint();

                if (worldPoint.distanceTo(center) <= handleHitRadius) {
                    document.selectElement(elem.id(), event.isControlDown() || event.isShiftDown());
                    startNodeDrag(elem.id(), 0, 6, worldPoint);
                    return;
                } else if (worldPoint.distanceTo(startP) <= handleHitRadius) {
                    document.selectElement(elem.id(), event.isControlDown() || event.isShiftDown());
                    startNodeDrag(elem.id(), 1, 6, worldPoint);
                    return;
                } else if (worldPoint.distanceTo(endP) <= handleHitRadius) {
                    document.selectElement(elem.id(), event.isControlDown() || event.isShiftDown());
                    startNodeDrag(elem.id(), 2, 6, worldPoint);
                    return;
                }
            }
        }

        double subPickToleranceMm = Math.max(8.0, 24.0 / camera.getZoom());

        // 2. Seleção padrão por corpo de elemento (1 clique direto)
        Optional<CADElement> clicked = document.findElementAt(worldPoint, pickToleranceMm);

        if (clicked.isPresent()) {
            CADElement elem = clicked.get();
            boolean isControlOrShift = event.isControlDown() || event.isShiftDown();

            var subRef = elem.findSubElementAt(worldPoint, subPickToleranceMm);
            if (subRef.isPresent()) {
                if (isControlOrShift) {
                    document.toggleSubElementSelection(subRef.get());
                } else {
                    document.selectSubElement(subRef.get(), false);
                }
            } else {
                if (isControlOrShift) {
                    document.toggleElementSelection(elem.id());
                } else {
                    if (!document.getSelectedElementIds().contains(elem.id())) {
                        document.selectElement(elem.id(), false);
                    }
                }
            }

            isDraggingSelection = true;
            dragStartWorld = worldPoint;
            boxStartPoint = null;
            boxCurrentPoint = null;
        } else {
            if (!event.isControlDown() && !event.isShiftDown()) {
                document.clearSelection();
            }
            isDraggingSelection = false;
            boxStartPoint = worldPoint;
            boxCurrentPoint = worldPoint;
        }
    }

    private void startNodeDrag(String elemId, int handleIdx, int type, Point2D worldPoint) {
        this.isDraggingNode = true;
        this.activeNodeElementId = elemId;
        this.activeHandleIndex = handleIdx;
        this.nodeType = type;
        this.isDraggingSelection = false;
        this.boxStartPoint = null;
        this.boxCurrentPoint = null;
    }

    @Override
    public void onMouseDragged(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {
        if (isDraggingNode && activeNodeElementId != null) {
            switch (nodeType) {
                case 1 -> document.updateBezierControlPoint(activeNodeElementId, activeHandleIndex, worldPoint);
                case 2 -> document.updatePolylineVertex(activeNodeElementId, activeHandleIndex, worldPoint);
                case 3 -> document.updateLineEndpoint(activeNodeElementId, activeHandleIndex, worldPoint);
                case 4 -> document.updateRectCorner(activeNodeElementId, activeHandleIndex, worldPoint);
                case 5 -> document.updateCircleGrip(activeNodeElementId, activeHandleIndex, worldPoint);
                case 6 -> document.updateArcGrip(activeNodeElementId, activeHandleIndex, worldPoint);
            }
        } else if (isDraggingSelection && dragStartWorld != null) {
            double dx = worldPoint.x() - dragStartWorld.x();
            double dy = worldPoint.y() - dragStartWorld.y();
            if (Math.abs(dx) > 0.001 || Math.abs(dy) > 0.001) {
                document.moveSelected(dx, dy);
                dragStartWorld = worldPoint;
            }
        } else if (boxStartPoint != null) {
            boxCurrentPoint = worldPoint;
        }
    }

    @Override
    public void onMouseReleased(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {
        if (isDraggingNode) {
            isDraggingNode = false;
            activeNodeElementId = null;
            activeHandleIndex = -1;
            return;
        }

        if (boxStartPoint != null) {
            if (worldPoint != null) {
                boxCurrentPoint = worldPoint;
            }
            double minX = Math.min(boxStartPoint.x(), boxCurrentPoint.x());
            double minY = Math.min(boxStartPoint.y(), boxCurrentPoint.y());
            double maxX = Math.max(boxStartPoint.x(), boxCurrentPoint.x());
            double maxY = Math.max(boxStartPoint.y(), boxCurrentPoint.y());
            double w = maxX - minX;
            double h = maxY - minY;

            if (w > 0.5 && h > 0.5) {
                Rect2D selectionRect = new Rect2D(minX, minY, w, h);
                boolean multiSelect = event.isControlDown() || event.isShiftDown();
                if (!multiSelect) document.clearSelection();

                // Padrão CAD Técnico:
                // Window (Esquerda -> Direita): apenas elementos 100% contidos
                // Crossing (Direita -> Esquerda): elementos contidos ou interceptados
                boolean isWindow = boxCurrentPoint.x() >= boxStartPoint.x();

                for (CADElement elem : document.getElements()) {
                    Layer layer = document.findLayerById(elem.layerId());
                    if (layer != null && (!layer.isVisible() || layer.isLocked())) continue;

                    Rect2D bbox = elem.boundingBox();
                    if (isWindow) {
                        // Totalmente contido na caixa
                        Point2D elemMin = bbox.minPoint();
                        Point2D elemMax = new Point2D(elemMin.x() + bbox.width(), elemMin.y() + bbox.height());
                        if (selectionRect.contains(elemMin) && selectionRect.contains(elemMax)) {
                            document.selectElement(elem.id(), true);
                        }
                    } else {
                        // Intercepta ou contido
                        if (intersects(selectionRect, bbox)) {
                            document.selectElement(elem.id(), true);
                        }
                    }
                }
            }
        }

        if (isDraggingSelection && dragStartWorld != null && !event.isControlDown() && !event.isShiftDown()) {
            double dragDist = (worldPoint != null) ? worldPoint.distanceTo(dragStartWorld) : 0.0;
            if (dragDist < 0.5) {
                double pickToleranceMm = Math.max(3.0, 14.0 / camera.getZoom());
                Optional<CADElement> clicked = document.findElementAt(worldPoint, pickToleranceMm);
                if (clicked.isPresent()) {
                    document.selectElement(clicked.get().id(), false);
                }
            }
        }

        isDraggingSelection = false;
        dragStartWorld = null;
        boxStartPoint = null;
        boxCurrentPoint = null;
    }

    private boolean intersects(Rect2D r1, Rect2D r2) {
        return !(r2.minPoint().x() > r1.minPoint().x() + r1.width() ||
                 r2.minPoint().x() + r2.width() < r1.minPoint().x() ||
                 r2.minPoint().y() > r1.minPoint().y() + r1.height() ||
                 r2.minPoint().y() + r2.height() < r1.minPoint().y());
    }

    @Override
    public void onMouseMoved(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {}

    @Override
    public void renderOverlay(GraphicsContext gc, CameraTransform camera) {
        // 1. Renderizar Caixa de Seleção Dinâmica (Rubber-Band Box)
        if (boxStartPoint != null && boxCurrentPoint != null) {
            Point2D s = camera.worldToScreen(boxStartPoint);
            Point2D e = camera.worldToScreen(boxCurrentPoint);

            double x = Math.min(s.x(), e.x());
            double y = Math.min(s.y(), e.y());
            double w = Math.abs(e.x() - s.x());
            double h = Math.abs(e.y() - s.y());

            boolean isWindow = boxCurrentPoint.x() >= boxStartPoint.x();

            if (isWindow) {
                // Window Selection (Esquerda -> Direita): Azul contínuo
                gc.setFill(Color.web("#00A8FF", 0.15));
                gc.fillRect(x, y, w, h);
                gc.setStroke(Color.web("#00A8FF"));
                gc.setLineWidth(1.0);
                gc.strokeRect(x, y, w, h);
            } else {
                // Crossing Selection (Direita -> Esquerda): Verde tracejado
                gc.setFill(Color.web("#00FF88", 0.15));
                gc.fillRect(x, y, w, h);
                gc.setStroke(Color.web("#00FF88"));
                gc.setLineWidth(1.0);
                gc.setLineDashes(4.0);
                gc.strokeRect(x, y, w, h);
                gc.setLineDashes(null);
            }
        }

        // 2. Renderizar Handles e Grips de Controle para os Elementos Selecionados
        if (currentDoc != null && !currentDoc.getSelectedElementIds().isEmpty()) {
            for (String selId : currentDoc.getSelectedElementIds()) {
                CADElement elem = currentDoc.findElementById(selId);
                if (elem instanceof BezierElement bezierElem) {
                    renderBezierHandles(gc, camera, bezierElem);
                } else if (elem instanceof PolylineElement polyElem) {
                    renderPolylineHandles(gc, camera, polyElem);
                } else if (elem instanceof LineElement lineElem) {
                    renderLineHandles(gc, camera, lineElem);
                } else if (elem instanceof RectElement rectElem) {
                    renderRectHandles(gc, camera, rectElem);
                } else if (elem instanceof CircleElement circleElem) {
                    renderCircleHandles(gc, camera, circleElem);
                } else if (elem instanceof ArcElement arcElem) {
                    renderArcHandles(gc, camera, arcElem);
                }
            }

            // 3. Renderizar Bounding Box Global & Handle de Rotação da Seleção
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
            boolean hasSelection = false;

            for (CADElement elem : currentDoc.getElements()) {
                if (currentDoc.getSelectedElementIds().contains(elem.id())) {
                    Rect2D box = elem.boundingBox();
                    minX = Math.min(minX, box.minPoint().x());
                    minY = Math.min(minY, box.minPoint().y());
                    maxX = Math.max(maxX, box.minPoint().x() + box.width());
                    maxY = Math.max(maxY, box.minPoint().y() + box.height());
                    hasSelection = true;
                }
            }

            if (hasSelection) {
                Point2D sMin = camera.worldToScreen(new Point2D(minX, minY));
                Point2D sMax = camera.worldToScreen(new Point2D(maxX, maxY));

                double x = sMin.x();
                double y = sMin.y();
                double w = sMax.x() - sMin.x();
                double h = sMax.y() - sMin.y();

                gc.setStroke(Color.web("#00A8FF", 0.6));
                gc.setLineWidth(1.0);
                gc.setLineDashes(3.0);
                gc.strokeRect(x, y, w, h);
                gc.setLineDashes(null);

                // Handle de Rotação no Topo
                double hs = 6.0;
                gc.setStroke(Color.web("#00A8FF"));
                gc.strokeLine(x + w / 2, y, x + w / 2, y - 18);
                gc.setFill(Color.web("#00A8FF"));
                gc.fillOval(x + w / 2 - hs / 2, y - 18 - hs / 2, hs, hs);
            }
        }
    }

    private void renderBezierHandles(GraphicsContext gc, CameraTransform camera, BezierElement elem) {
        var b = elem.bezier();
        Point2D s = camera.worldToScreen(b.start());
        Point2D c1 = camera.worldToScreen(b.control1());
        Point2D c2 = camera.worldToScreen(b.control2());
        Point2D e = camera.worldToScreen(b.end());

        gc.setStroke(Color.web("#FFD700"));
        gc.setLineWidth(1.2);
        gc.setLineDashes(3.0);
        gc.strokeLine(s.x(), s.y(), c1.x(), c1.y());
        gc.strokeLine(e.x(), e.y(), c2.x(), c2.y());
        gc.setLineDashes(null);

        double nodeSize = 8.0;
        gc.setFill(Color.web("#00FF88"));
        gc.fillRect(s.x() - nodeSize / 2, s.y() - nodeSize / 2, nodeSize, nodeSize);
        gc.fillRect(e.x() - nodeSize / 2, e.y() - nodeSize / 2, nodeSize, nodeSize);

        double ctrlSize = 8.0;
        gc.setFill(Color.web("#FFD700"));
        gc.fillOval(c1.x() - ctrlSize / 2, c1.y() - ctrlSize / 2, ctrlSize, ctrlSize);
        gc.fillOval(c2.x() - ctrlSize / 2, c2.y() - ctrlSize / 2, ctrlSize, ctrlSize);

        gc.setStroke(Color.web("#FFFFFF"));
        gc.setLineWidth(1.0);
        gc.strokeOval(c1.x() - ctrlSize / 2, c1.y() - ctrlSize / 2, ctrlSize, ctrlSize);
        gc.strokeOval(c2.x() - ctrlSize / 2, c2.y() - ctrlSize / 2, ctrlSize, ctrlSize);
    }

    private void renderPolylineHandles(GraphicsContext gc, CameraTransform camera, PolylineElement elem) {
        var pts = elem.polyline().points();
        double hs = 7.0;
        gc.setFill(Color.web("#00A8FF"));
        for (Point2D p : pts) {
            Point2D sp = camera.worldToScreen(p);
            gc.fillRect(sp.x() - hs / 2, sp.y() - hs / 2, hs, hs);
        }
    }

    private void renderLineHandles(GraphicsContext gc, CameraTransform camera, LineElement elem) {
        var line = elem.line();
        Point2D s = camera.worldToScreen(line.start());
        Point2D e = camera.worldToScreen(line.end());
        double hs = 7.0;
        gc.setFill(Color.web("#00FF88"));
        gc.fillRect(s.x() - hs / 2, s.y() - hs / 2, hs, hs);
        gc.fillRect(e.x() - hs / 2, e.y() - hs / 2, hs, hs);
    }

    private void renderRectHandles(GraphicsContext gc, CameraTransform camera, RectElement elem) {
        var r = elem.rect();
        Point2D c0 = camera.worldToScreen(r.minPoint());
        Point2D c1 = camera.worldToScreen(new Point2D(r.minPoint().x() + r.width(), r.minPoint().y()));
        Point2D c2 = camera.worldToScreen(new Point2D(r.minPoint().x() + r.width(), r.minPoint().y() + r.height()));
        Point2D c3 = camera.worldToScreen(new Point2D(r.minPoint().x(), r.minPoint().y() + r.height()));

        double hs = 7.0;
        gc.setFill(Color.web("#00FF88"));
        gc.fillRect(c0.x() - hs / 2, c0.y() - hs / 2, hs, hs);
        gc.fillRect(c1.x() - hs / 2, c1.y() - hs / 2, hs, hs);
        gc.fillRect(c2.x() - hs / 2, c2.y() - hs / 2, hs, hs);
        gc.fillRect(c3.x() - hs / 2, c3.y() - hs / 2, hs, hs);
    }

    private void renderCircleHandles(GraphicsContext gc, CameraTransform camera, CircleElement elem) {
        var c = elem.circle();
        Point2D centerSc = camera.worldToScreen(c.center());
        double r = c.radius();

        Point2D qTop = camera.worldToScreen(new Point2D(c.center().x(), c.center().y() - r));
        Point2D qRight = camera.worldToScreen(new Point2D(c.center().x() + r, c.center().y()));
        Point2D qBottom = camera.worldToScreen(new Point2D(c.center().x(), c.center().y() + r));
        Point2D qLeft = camera.worldToScreen(new Point2D(c.center().x() - r, c.center().y()));

        double hs = 7.0;
        gc.setFill(Color.web("#FFD700"));
        gc.fillOval(centerSc.x() - hs / 2, centerSc.y() - hs / 2, hs, hs);

        gc.setFill(Color.web("#00FF88"));
        gc.fillRect(qTop.x() - hs / 2, qTop.y() - hs / 2, hs, hs);
        gc.fillRect(qRight.x() - hs / 2, qRight.y() - hs / 2, hs, hs);
        gc.fillRect(qBottom.x() - hs / 2, qBottom.y() - hs / 2, hs, hs);
        gc.fillRect(qLeft.x() - hs / 2, qLeft.y() - hs / 2, hs, hs);
    }

    private void renderArcHandles(GraphicsContext gc, CameraTransform camera, ArcElement elem) {
        var a = elem.arc();
        Point2D centerSc = camera.worldToScreen(a.center());
        Point2D startSc = camera.worldToScreen(a.startPoint());
        Point2D endSc = camera.worldToScreen(a.endPoint());

        double hs = 7.0;
        gc.setFill(Color.web("#FFD700"));
        gc.fillOval(centerSc.x() - hs / 2, centerSc.y() - hs / 2, hs, hs);

        gc.setFill(Color.web("#00FF88"));
        gc.fillRect(startSc.x() - hs / 2, startSc.y() - hs / 2, hs, hs);
        gc.fillRect(endSc.x() - hs / 2, endSc.y() - hs / 2, hs, hs);
    }

    @Override
    public void reset() {
        boxStartPoint = null;
        boxCurrentPoint = null;
        isDraggingSelection = false;
        dragStartWorld = null;
        isDraggingNode = false;
        activeNodeElementId = null;
        activeHandleIndex = -1;
        nodeType = 0;
    }
}
