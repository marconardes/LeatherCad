package com.leathercad.ui.tools;

import com.leathercad.core.geometry.LineSegment;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.leather.StitchConfig;
import com.leathercad.core.leather.StitchElement;
import com.leathercad.core.leather.StitchType;
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
import java.util.stream.Collectors;

public class StitchTool implements CADTool {
    private Point2D startPoint = null;
    private Point2D currentHover = null;
    private StitchConfig config = StitchConfig.DEFAULT;
    private double offsetMm = 3.85;

    // Estado para seleções prévias no Document
    private final List<LineSegment> selectedStitchBaseLines = new ArrayList<>();
    private final List<LineSegment> selectedSourceSegments = new ArrayList<>();

    // Estado para hover individual
    private CADElement hoveredElement = null;
    private CADElement previewOffsetElement = null;

    public void setStitchType(StitchType newType) {
        this.config = new StitchConfig(config.marginMm(), config.pitchMm(), config.holeDiameterMm(), config.slotLengthMm(), config.angleDegrees(), newType);
    }

    public void setPitchMm(double pitchMm) {
        this.config = new StitchConfig(config.marginMm(), pitchMm, config.holeDiameterMm(), config.slotLengthMm(), config.angleDegrees(), config.type());
    }

    private static final double[] PITCH_PRESETS = {2.70, 3.00, 3.38, 3.85, 4.00, 5.00};

    public void incrementPitch() {
        double current = config.pitchMm();
        for (double preset : PITCH_PRESETS) {
            if (preset > current + 0.01) {
                setPitchMm(preset);
                return;
            }
        }
        setPitchMm(PITCH_PRESETS[PITCH_PRESETS.length - 1]);
    }

    public void decrementPitch() {
        double current = config.pitchMm();
        for (int i = PITCH_PRESETS.length - 1; i >= 0; i--) {
            if (PITCH_PRESETS[i] < current - 0.01) {
                setPitchMm(PITCH_PRESETS[i]);
                return;
            }
        }
        setPitchMm(PITCH_PRESETS[0]);
    }

    public void setConfig(StitchConfig config) {
        this.config = config;
    }

    public StitchConfig getConfig() {
        return config;
    }

    public void toggleAngle() {
        double newAngle = (config.angleDegrees() < 0) ? 45.0 : -45.0;
        this.config = new StitchConfig(config.marginMm(), config.pitchMm(), config.holeDiameterMm(), config.slotLengthMm(), newAngle, config.type());
    }

    public double getOffsetMm() {
        return offsetMm;
    }

    public void setOffsetMm(double offsetMm) {
        if (offsetMm >= 0) {
            this.offsetMm = offsetMm;
        }
    }

    @Override
    public String getName() { return "Costura Parametrizada"; }

    @Override
    public void onMousePressed(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {
        if (!selectedStitchBaseLines.isEmpty()) {
            for (LineSegment seg : selectedStitchBaseLines) {
                if (seg.length() > 0.1) {
                    StitchElement stitch = new StitchElement(document.getStitchLayerId(), seg, config);
                    document.addElement(stitch);
                }
            }
            document.clearSelection();
            clearAll();
            startPoint = null;
            onMouseMoved(event, worldPoint, document, camera);
            return;
        }

        if (hoveredElement != null && previewOffsetElement != null) {
            List<LineSegment> segments = extractLineSegments(previewOffsetElement);
            for (LineSegment seg : segments) {
                if (seg.length() > 0.1) {
                    StitchElement stitch = new StitchElement(document.getStitchLayerId(), seg, config);
                    document.addElement(stitch);
                }
            }
            clearAll();
            startPoint = null;
            onMouseMoved(event, worldPoint, document, camera);
            return;
        }

        if (startPoint == null) {
            startPoint = worldPoint;
        } else {
            if (startPoint.distanceTo(worldPoint) > 0.1) {
                LineSegment baseLine = new LineSegment(startPoint, worldPoint);
                StitchElement stitch = new StitchElement(document.getStitchLayerId(), baseLine, config);
                document.addElement(stitch);
            }
            startPoint = null;
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
        clearAll();

        // 1. Verificar seleções prévias no Document
        Set<SubElementRef> selectedSubEdges = document.getSelectedSubElements().stream()
            .filter(sub -> sub.type() == SubElementRef.SubElementType.EDGE)
            .collect(Collectors.toSet());
        Set<String> selectedElementIds = document.getSelectedElementIds();

        if (!selectedSubEdges.isEmpty()) {
            for (SubElementRef ref : selectedSubEdges) {
                CADElement elem = document.findElementById(ref.elementId());
                if (elem != null) {
                    LineSegment edgeSeg = extractSingleEdge(elem, ref.index());
                    if (edgeSeg != null) {
                        selectedSourceSegments.add(edgeSeg);
                        LineSegment offsetSeg = com.leathercad.core.geometry.GeometryOffset.offset(edgeSeg, worldPoint, offsetMm);
                        selectedStitchBaseLines.add(offsetSeg);
                    }
                }
            }
            return;
        } else if (!selectedElementIds.isEmpty()) {
            for (String id : selectedElementIds) {
                CADElement elem = document.findElementById(id);
                if (elem != null) {
                    CADElement offsetElem = elem.createOffset(worldPoint, offsetMm, document.getStitchLayerId());
                    selectedStitchBaseLines.addAll(extractLineSegments(offsetElem));
                }
            }
            return;
        }

        // 2. Se nada estiver selecionado -> Modo hover individual sob o cursor
        if (startPoint == null && offsetMm > 0) {
            double toleranceMm = 12.0 / camera.getZoom();
            for (CADElement elem : document.getElements()) {
                Layer layer = document.findLayerById(elem.layerId());
                if (layer != null && (layer.isLocked() || !layer.isVisible())) continue;

                if (elem.containsPoint(worldPoint, toleranceMm)) {
                    hoveredElement = elem;
                    previewOffsetElement = elem.createOffset(worldPoint, offsetMm, document.getStitchLayerId());
                    break;
                }
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

    private List<LineSegment> extractLineSegments(CADElement elem) {
        List<LineSegment> segments = new ArrayList<>();
        if (elem instanceof LineElement lineElem) {
            segments.add(lineElem.line());
        } else if (elem instanceof RectElement rectElem) {
            for (LineElement l : rectElem.explodeToLines()) {
                segments.add(l.line());
            }
        } else if (elem instanceof PolylineElement polyElem) {
            for (LineElement l : polyElem.explodeToLines()) {
                segments.add(l.line());
            }
        }
        return segments;
    }

    private void clearAll() {
        selectedStitchBaseLines.clear();
        selectedSourceSegments.clear();
        hoveredElement = null;
        previewOffsetElement = null;
    }

    @Override
    public void renderOverlay(GraphicsContext gc, CameraTransform camera) {
        if (currentHover == null) return;
        Point2D mouseScreen = camera.worldToScreen(currentHover);

        // 1. Caso existam seleções prévias no Document
        if (!selectedStitchBaseLines.isEmpty()) {
            gc.save();

            // Destacar arestas de origem
            if (!selectedSourceSegments.isEmpty()) {
                gc.setStroke(Color.web("#FF9800"));
                gc.setLineWidth(3.0);
                for (LineSegment seg : selectedSourceSegments) {
                    Point2D s = camera.worldToScreen(seg.start());
                    Point2D e = camera.worldToScreen(seg.end());
                    gc.strokeLine(s.x(), s.y(), e.x(), e.y());
                }
            }

            // Preview das linhas de costura offsetadas com furos/fendas
            gc.setStroke(Color.web("#FFD700"));
            gc.setLineWidth(1.5);
            gc.setLineDashes(4.0, 2.0);
            for (LineSegment seg : selectedStitchBaseLines) {
                Point2D s = camera.worldToScreen(seg.start());
                Point2D e = camera.worldToScreen(seg.end());
                gc.strokeLine(s.x(), s.y(), e.x(), e.y());
            }
            gc.setLineDashes(null);

            for (LineSegment seg : selectedStitchBaseLines) {
                renderPreviewStitch(gc, camera, seg);
            }
            gc.restore();

            String text = String.format("Costura Offset (%d) | %s %.2fmm (Margem: %.1fmm)", 
                selectedStitchBaseLines.size(), config.type().name(), config.pitchMm(), offsetMm);
            gc.setFont(Font.font("Segoe UI", 12));
            gc.setFill(Color.web("#1E1E1E", 0.85));
            gc.fillRoundRect(mouseScreen.x() + 14, mouseScreen.y() - 22, 310, 24, 6, 6);
            gc.setFill(Color.web("#FFD700"));
            gc.fillText(text, mouseScreen.x() + 20, mouseScreen.y() - 6);
            return;
        }

        // 2. Modo hover individual
        if (hoveredElement != null && previewOffsetElement != null) {
            List<LineSegment> segs = extractLineSegments(previewOffsetElement);

            gc.setStroke(Color.web("#FFD700"));
            gc.setLineWidth(1.5);
            gc.setLineDashes(4.0, 2.0);
            for (LineSegment seg : segs) {
                Point2D s = camera.worldToScreen(seg.start());
                Point2D e = camera.worldToScreen(seg.end());
                gc.strokeLine(s.x(), s.y(), e.x(), e.y());
            }
            gc.setLineDashes(null);

            for (LineSegment seg : segs) {
                renderPreviewStitch(gc, camera, seg);
            }

            String text = String.format("Costura | %s %.2fmm (Margem: %.1fmm)", 
                config.type().name(), config.pitchMm(), offsetMm);
            gc.setFont(Font.font("Segoe UI", 12));
            gc.setFill(Color.web("#1E1E1E", 0.85));
            gc.fillRoundRect(mouseScreen.x() + 14, mouseScreen.y() - 22, 280, 24, 6, 6);
            gc.setFill(Color.web("#FFD700"));
            gc.fillText(text, mouseScreen.x() + 20, mouseScreen.y() - 6);
            return;
        }

        // 3. Desenho de costura tradicional por 2 pontos
        if (startPoint != null) {
            Point2D s = camera.worldToScreen(startPoint);
            Point2D e = camera.worldToScreen(currentHover);

            gc.setStroke(Color.web("#FFD700"));
            gc.setLineWidth(1.2);
            gc.setLineDashes(4.0);
            gc.strokeLine(s.x(), s.y(), e.x(), e.y());
            gc.setLineDashes(null);

            LineSegment seg = new LineSegment(startPoint, currentHover);
            renderPreviewStitch(gc, camera, seg);
        }
    }

    private void renderPreviewStitch(GraphicsContext gc, CameraTransform camera, LineSegment seg) {
        if (seg.length() < 0.1) return;
        gc.setStroke(Color.web("#FFD700"));
        gc.setFill(Color.web("#FFD700"));

        if (config.type() == com.leathercad.core.leather.StitchType.MACHINE_STITCH) {
            gc.setLineWidth(1.5);
            gc.setLineDashes(5.0, 3.0);
            Point2D s = camera.worldToScreen(seg.start());
            Point2D e = camera.worldToScreen(seg.end());
            gc.strokeLine(s.x(), s.y(), e.x(), e.y());
            gc.setLineDashes(null);
        } else if (config.type() == com.leathercad.core.leather.StitchType.ROUND_PUNCH || config.type() == com.leathercad.core.leather.StitchType.ROUND) {
            var holes = com.leathercad.core.leather.StitchEngine.calculateStitchHoles(seg, config);
            for (Point2D hole : holes) {
                Point2D p = camera.worldToScreen(hole);
                double hr = Math.max(1.5, camera.worldToScreenLength(config.holeDiameterMm() / 2.0));
                gc.strokeOval(p.x() - hr, p.y() - hr, hr * 2, hr * 2);
            }
        } else {
            // FRENCH_SLANT / DEFAULT
            var slots = com.leathercad.core.leather.StitchEngine.calculateSlantSlots(seg, config);
            gc.setLineWidth(2.0);
            for (var slot : slots) {
                Point2D s1 = camera.worldToScreen(slot.start());
                Point2D s2 = camera.worldToScreen(slot.end());
                gc.strokeLine(s1.x(), s1.y(), s2.x(), s2.y());
            }
        }
    }

    @Override
    public void reset() {
        startPoint = null;
        currentHover = null;
        clearAll();
    }
}
