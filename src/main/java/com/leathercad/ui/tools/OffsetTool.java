package com.leathercad.ui.tools;

import com.leathercad.core.geometry.LineSegment;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.model.*;
import com.leathercad.ui.viewport.CameraTransform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class OffsetTool implements CADTool {

    private double distanceMm = 5.0;

    // Estado para seleções em lote via Document
    private final List<CADElement> selectedPreviewElements = new ArrayList<>();
    private final List<LineSegment> selectedSourceSegments = new ArrayList<>();
    private final List<CADElement> selectedSourceElements = new ArrayList<>();

    // Estado para modo interativo hover sob o cursor
    private CADElement hoveredElement = null;
    private SubElementRef hoveredSubRef = null;
    private LineSegment hoveredEdgeSegment = null;
    private CADElement previewOffsetElement = null;

    private Point2D lastMouseWorldPoint = null;

    public OffsetTool() {}

    public OffsetTool(double defaultDistanceMm) {
        this.distanceMm = defaultDistanceMm;
    }

    public double getDistanceMm() {
        return distanceMm;
    }

    public void setDistanceMm(double distanceMm) {
        if (distanceMm > 0) {
            this.distanceMm = distanceMm;
        }
    }

    @Override
    public String getName() {
        return "Margem de Costura / Offset";
    }

    @Override
    public void onMousePressed(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {
        if (!selectedPreviewElements.isEmpty()) {
            for (CADElement elem : selectedPreviewElements) {
                document.addElement(elem);
            }
            document.clearSelection();
            clearAll();
            onMouseMoved(event, worldPoint, document, camera);
        } else if (hoveredElement != null && previewOffsetElement != null) {
            document.addElement(previewOffsetElement);
            clearAll();
            onMouseMoved(event, worldPoint, document, camera);
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
        clearAll();
        lastMouseWorldPoint = worldPoint;

        // 1. Verificar seleções prévias no Document (sub-arestas ou elementos inteiros)
        Set<SubElementRef> selectedSubEdges = document.getSelectedSubElements().stream()
            .filter(sub -> sub.type() == SubElementRef.SubElementType.EDGE)
            .collect(Collectors.toSet());
        Set<String> selectedElementIds = document.getSelectedElementIds();

        if (!selectedSubEdges.isEmpty()) {
            // Offset APENAS sobre as sub-arestas previamente selecionadas
            for (SubElementRef ref : selectedSubEdges) {
                CADElement elem = document.findElementById(ref.elementId());
                if (elem != null) {
                    LineSegment edgeSeg = extractSingleEdge(elem, ref.index());
                    if (edgeSeg != null) {
                        selectedSourceSegments.add(edgeSeg);
                        LineSegment offsetSeg = com.leathercad.core.geometry.GeometryOffset.offset(edgeSeg, worldPoint, distanceMm);
                        selectedPreviewElements.add(new LineElement(UUID.randomUUID().toString(), document.getActiveLayer().getId(), offsetSeg));
                    }
                }
            }
            return;
        } else if (!selectedElementIds.isEmpty()) {
            // Offset sobre os elementos inteiros previamente selecionados
            for (String id : selectedElementIds) {
                CADElement elem = document.findElementById(id);
                if (elem != null) {
                    selectedSourceElements.add(elem);
                    CADElement offsetElem = elem.createOffset(worldPoint, distanceMm, document.getActiveLayer().getId());
                    selectedPreviewElements.add(offsetElem);
                }
            }
            return;
        }

        // 2. Se NADA estiver selecionado previamente -> Modo de clique/hover individual sob o cursor
        double toleranceMm = 12.0 / camera.getZoom();
        for (CADElement elem : document.getElements()) {
            Layer layer = document.findLayerById(elem.layerId());
            if (layer != null && (layer.isLocked() || !layer.isVisible())) continue;

            Optional<SubElementRef> subRefOpt = elem.findSubElementAt(worldPoint, toleranceMm);
            boolean isEdgeHover = subRefOpt.isPresent() && subRefOpt.get().type() == SubElementRef.SubElementType.EDGE;

            if (isEdgeHover && !event.isShiftDown()) {
                hoveredElement = elem;
                hoveredSubRef = subRefOpt.get();
                hoveredEdgeSegment = extractSingleEdge(elem, hoveredSubRef.index());
                if (hoveredEdgeSegment != null) {
                    LineSegment offsetSeg = com.leathercad.core.geometry.GeometryOffset.offset(hoveredEdgeSegment, worldPoint, distanceMm);
                    previewOffsetElement = new LineElement(UUID.randomUUID().toString(), document.getActiveLayer().getId(), offsetSeg);
                }
                break;
            } else if (elem.containsPoint(worldPoint, toleranceMm)) {
                hoveredElement = elem;
                previewOffsetElement = elem.createOffset(worldPoint, distanceMm, document.getActiveLayer().getId());
                break;
            }
        }
    }

    private LineSegment extractSingleEdge(CADElement elem, int index) {
        if (elem instanceof LineElement lineElem) {
            return lineElem.line();
        } else if (elem instanceof RectElement rectElem) {
            Rect2D r = rectElem.rect();
            Point2D v0 = r.minPoint();
            Point2D v1 = new Point2D(r.minPoint().x() + r.width(), r.minPoint().y());
            Point2D v2 = new Point2D(r.minPoint().x() + r.width(), r.minPoint().y() + r.height());
            Point2D v3 = new Point2D(r.minPoint().x(), r.minPoint().y() + r.height());

            return switch (index) {
                case 0 -> new LineSegment(v0, v1);
                case 1 -> new LineSegment(v1, v2);
                case 2 -> new LineSegment(v3, v2);
                case 3 -> new LineSegment(v0, v3);
                default -> null;
            };
        } else if (elem instanceof PolylineElement polyElem) {
            var pts = polyElem.polyline().points();
            int n = pts.size();
            if (n < 2) return null;
            if (index >= 0 && index < n - 1) {
                return new LineSegment(pts.get(index), pts.get(index + 1));
            } else if (polyElem.polyline().isClosed() && index == n - 1) {
                return new LineSegment(pts.get(n - 1), pts.get(0));
            }
        }
        return null;
    }

    @Override
    public void renderOverlay(GraphicsContext gc, CameraTransform camera) {
        if (lastMouseWorldPoint == null) return;
        Point2D mouseScreen = camera.worldToScreen(lastMouseWorldPoint);

        gc.save();

        // 1. Caso existam seleções prévias no Document
        if (!selectedPreviewElements.isEmpty()) {
            // Destacar arestas de origem selecionadas
            gc.setStroke(Color.web("#FF9800"));
            gc.setLineWidth(3.0);
            for (LineSegment seg : selectedSourceSegments) {
                Point2D s = camera.worldToScreen(seg.start());
                Point2D e = camera.worldToScreen(seg.end());
                gc.strokeLine(s.x(), s.y(), e.x(), e.y());
            }

            // Preview das linhas/contornos em offset
            gc.setStroke(Color.web("#FFD700"));
            gc.setLineWidth(2.0);
            gc.setLineDashes(6.0, 4.0);
            for (CADElement elem : selectedPreviewElements) {
                renderElementGeometry(gc, camera, elem);
            }
            gc.setLineDashes(null);
            gc.restore();

            String text = String.format("Offset Seleção (%d itens): %.2f mm", selectedPreviewElements.size(), distanceMm);
            gc.setFont(Font.font("Segoe UI", 12));
            gc.setFill(Color.web("#1E1E1E", 0.85));
            gc.fillRoundRect(mouseScreen.x() + 14, mouseScreen.y() - 22, 220, 24, 6, 6);
            gc.setFill(Color.web("#FFD700"));
            gc.fillText(text, mouseScreen.x() + 22, mouseScreen.y() - 6);
            return;
        }

        // 2. Modo hover interativo individual sob o cursor
        if (hoveredElement == null || previewOffsetElement == null) {
            gc.restore();
            return;
        }

        if (hoveredEdgeSegment != null) {
            Point2D s = camera.worldToScreen(hoveredEdgeSegment.start());
            Point2D e = camera.worldToScreen(hoveredEdgeSegment.end());
            gc.setStroke(Color.web("#FF9800"));
            gc.setLineWidth(3.0);
            gc.strokeLine(s.x(), s.y(), e.x(), e.y());
        }

        gc.setStroke(Color.web("#FFD700"));
        gc.setLineWidth(2.0);
        gc.setLineDashes(6.0, 4.0);
        renderElementGeometry(gc, camera, previewOffsetElement);
        gc.setLineDashes(null);
        gc.restore();

        String text = (hoveredEdgeSegment != null)
            ? String.format("Offset Lado: %.2f mm", distanceMm)
            : String.format("Offset Contorno: %.2f mm", distanceMm);

        gc.setFont(Font.font("Segoe UI", 12));
        gc.setFill(Color.web("#1E1E1E", 0.85));
        gc.fillRoundRect(mouseScreen.x() + 14, mouseScreen.y() - 22, 160, 24, 6, 6);
        gc.setFill(Color.web("#FFD700"));
        gc.fillText(text, mouseScreen.x() + 22, mouseScreen.y() - 6);
    }

    private void renderElementGeometry(GraphicsContext gc, CameraTransform camera, CADElement elem) {
        if (elem instanceof LineElement lineElem) {
            Point2D s = camera.worldToScreen(lineElem.line().start());
            Point2D e = camera.worldToScreen(lineElem.line().end());
            gc.strokeLine(s.x(), s.y(), e.x(), e.y());
        } else if (elem instanceof CircleElement circleElem) {
            Point2D center = camera.worldToScreen(circleElem.circle().center());
            double rScreen = circleElem.circle().radius() * camera.getZoom();
            gc.strokeOval(center.x() - rScreen, center.y() - rScreen, rScreen * 2, rScreen * 2);
        } else if (elem instanceof ArcElement arcElem) {
            var arc = arcElem.arc();
            Point2D center = camera.worldToScreen(arc.center());
            double rScreen = arc.radius() * camera.getZoom();
            gc.strokeArc(
                center.x() - rScreen,
                center.y() - rScreen,
                rScreen * 2,
                rScreen * 2,
                arc.startAngleDegrees(),
                arc.sweepAngleDegrees(),
                javafx.scene.shape.ArcType.OPEN
            );
        } else if (elem instanceof RectElement rectElem) {
            var r = rectElem.rect();
            Point2D min = camera.worldToScreen(r.minPoint());
            double w = r.width() * camera.getZoom();
            double h = r.height() * camera.getZoom();
            double arc = r.cornerRadius() * camera.getZoom() * 2;
            if (r.cornerRadius() > 0) {
                gc.strokeRoundRect(min.x(), min.y(), w, h, arc, arc);
            } else {
                gc.strokeRect(min.x(), min.y(), w, h);
            }
        } else if (elem instanceof PolylineElement polyElem) {
            var pts = polyElem.polyline().points();
            if (pts.size() >= 2) {
                double[] xPoints = new double[pts.size()];
                double[] yPoints = new double[pts.size()];
                for (int i = 0; i < pts.size(); i++) {
                    Point2D sp = camera.worldToScreen(pts.get(i));
                    xPoints[i] = sp.x();
                    yPoints[i] = sp.y();
                }
                if (polyElem.polyline().isClosed()) {
                    gc.strokePolygon(xPoints, yPoints, pts.size());
                } else {
                    gc.strokePolyline(xPoints, yPoints, pts.size());
                }
            }
        }
    }

    private void clearAll() {
        selectedPreviewElements.clear();
        selectedSourceSegments.clear();
        selectedSourceElements.clear();
        hoveredElement = null;
        hoveredSubRef = null;
        hoveredEdgeSegment = null;
        previewOffsetElement = null;
    }

    @Override
    public void reset() {
        clearAll();
        lastMouseWorldPoint = null;
    }
}
