package com.leathercad.ui.tools;

import com.leathercad.core.geometry.LineSegment;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.model.*;
import com.leathercad.ui.CalloutNoteDialog;
import com.leathercad.ui.viewport.CameraTransform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.List;

public class DimensionTool implements CADTool {
    private DimensionElement.DimensionType currentType = DimensionElement.DimensionType.LINEAR;

    private int stage = 0; // 0: aguardando P1/aresta, 1: aguardando P2, 2: arrastando offset da cota
    private Point2D p1 = null;
    private Point2D p2 = null;
    private Point2D currentHover = null;

    private LineSegment hoveredEdge = null;
    private Point2D hoveredArcCenter = null;
    private double hoveredArcRadius = 0.0;

    public DimensionTool() {
        this(DimensionElement.DimensionType.LINEAR);
    }

    public DimensionTool(DimensionElement.DimensionType type) {
        this.currentType = (type != null) ? type : DimensionElement.DimensionType.LINEAR;
    }

    public DimensionElement.DimensionType getCurrentType() {
        return currentType;
    }

    public void setCurrentType(DimensionElement.DimensionType type) {
        this.currentType = (type != null) ? type : DimensionElement.DimensionType.LINEAR;
        reset();
    }

    @Override
    public String getName() {
        return "Cotagem e Ficha Técnica";
    }

    @Override
    public void onMousePressed(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {
        if (currentType == DimensionElement.DimensionType.CALLOUT_NOTE) {
            if (stage == 0) {
                p1 = worldPoint; // Alvo do apontador
                stage = 1;
            } else if (stage == 1) {
                p2 = worldPoint; // Posição do texto
                var noteOpt = CalloutNoteDialog.showDialog(null, "Couro Bovino 1.5mm");
                if (noteOpt.isPresent()) {
                    String noteText = noteOpt.get();
                    DimensionElement dim = new DimensionElement(
                        UUIDRandom(),
                        document.getActiveLayer().getId(),
                        p1,
                        p2,
                        DimensionElement.DimensionType.CALLOUT_NOTE,
                        0.0,
                        noteText
                    );
                    document.addElement(dim);
                }
                reset();
            }
            return;
        }

        if (currentType == DimensionElement.DimensionType.RADIUS) {
            if (hoveredArcCenter != null && hoveredArcRadius > 0) {
                Point2D borderPoint = worldPoint;
                if (borderPoint.distanceTo(hoveredArcCenter) > 0.01) {
                    double angle = Math.atan2(borderPoint.y() - hoveredArcCenter.y(), borderPoint.x() - hoveredArcCenter.x());
                    borderPoint = new Point2D(
                        hoveredArcCenter.x() + Math.cos(angle) * hoveredArcRadius,
                        hoveredArcCenter.y() + Math.sin(angle) * hoveredArcRadius
                    );
                }
                DimensionElement dim = new DimensionElement(
                    document.getActiveLayer().getId(),
                    hoveredArcCenter,
                    borderPoint,
                    DimensionElement.DimensionType.RADIUS,
                    hoveredArcRadius
                );
                document.addElement(dim);
                reset();
                return;
            } else {
                if (stage == 0) {
                    p1 = worldPoint;
                    stage = 1;
                } else if (stage == 1) {
                    p2 = worldPoint;
                    double r = p1.distanceTo(p2);
                    DimensionElement dim = new DimensionElement(
                        document.getActiveLayer().getId(),
                        p1,
                        p2,
                        DimensionElement.DimensionType.RADIUS,
                        r
                    );
                    document.addElement(dim);
                    reset();
                }
                return;
            }
        }

        // Fluxo de 3 passos para Cota Linear / Angular
        if (stage == 0) {
            if (hoveredEdge != null) {
                p1 = hoveredEdge.start();
                p2 = hoveredEdge.end();
                stage = 2; // Pula direto para o ajuste de offset!
            } else {
                p1 = worldPoint;
                stage = 1;
            }
        } else if (stage == 1) {
            p2 = worldPoint;
            stage = 2;
        } else if (stage == 2) {
            if (p1 != null && p2 != null) {
                double offsetMm = calculateOffsetDistance(p1, p2, worldPoint);
                DimensionElement dim = new DimensionElement(
                    document.getActiveLayer().getId(),
                    p1,
                    p2,
                    currentType,
                    offsetMm
                );
                document.addElement(dim);
            }
            reset();
        }
    }

    private String UUIDRandom() {
        return java.util.UUID.randomUUID().toString();
    }

    private double calculateOffsetDistance(Point2D s, Point2D e, Point2D cursor) {
        LineSegment base = new LineSegment(s, e);
        return base.distanceToPoint(cursor);
    }

    @Override
    public void onMouseMoved(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {
        currentHover = worldPoint;
        hoveredEdge = null;
        hoveredArcCenter = null;
        hoveredArcRadius = 0.0;

        if (stage == 0 && document != null) {
            double tol = 10.0 / camera.getZoom();
            for (CADElement elem : document.getElements()) {
                Layer layer = document.findLayerById(elem.layerId());
                if (layer != null && (layer.isLocked() || !layer.isVisible())) continue;

                if (elem instanceof ArcElement arcElem) {
                    if (arcElem.containsPoint(worldPoint, tol)) {
                        hoveredArcCenter = arcElem.arc().center();
                        hoveredArcRadius = arcElem.arc().radius();
                        break;
                    }
                } else if (elem instanceof RectElement rectElem) {
                    Rect2D r = rectElem.rect();
                    if (r.cornerRadius() > 0 && rectElem.containsPoint(worldPoint, tol)) {
                        hoveredArcRadius = r.cornerRadius();
                        hoveredArcCenter = findNearestRectFilletCenter(r, worldPoint);
                        if (hoveredArcCenter != null) break;
                    }

                    LineSegment edge = findNearestEdge(rectElem.explodeToLines().stream().map(LineElement::line).toList(), worldPoint, tol);
                    if (edge != null) {
                        hoveredEdge = edge;
                        break;
                    }
                } else if (elem instanceof LineElement lineElem) {
                    if (lineElem.containsPoint(worldPoint, tol)) {
                        hoveredEdge = lineElem.line();
                        break;
                    }
                } else if (elem instanceof PolylineElement polyElem) {
                    LineSegment edge = findNearestEdge(polyElem.explodeToLines().stream().map(LineElement::line).toList(), worldPoint, tol);
                    if (edge != null) {
                        hoveredEdge = edge;
                        break;
                    }
                }
            }
        }
    }

    private Point2D findNearestRectFilletCenter(Rect2D r, Point2D p) {
        Point2D min = r.minPoint();
        double cr = r.cornerRadius();
        Point2D cTL = new Point2D(min.x() + cr, min.y() + cr);
        Point2D cTR = new Point2D(min.x() + r.width() - cr, min.y() + cr);
        Point2D cBR = new Point2D(min.x() + r.width() - cr, min.y() + r.height() - cr);
        Point2D cBL = new Point2D(min.x() + cr, min.y() + r.height() - cr);

        List<Point2D> centers = List.of(cTL, cTR, cBR, cBL);
        Point2D best = null;
        double minDist = Double.MAX_VALUE;
        for (Point2D c : centers) {
            double d = Math.abs(c.distanceTo(p) - cr);
            if (d < minDist) {
                minDist = d;
                best = c;
            }
        }
        return best;
    }

    private LineSegment findNearestEdge(List<LineSegment> segments, Point2D p, double tol) {
        LineSegment best = null;
        double minDist = tol;
        for (LineSegment seg : segments) {
            double d = seg.distanceToPoint(p);
            if (d <= minDist) {
                minDist = d;
                best = seg;
            }
        }
        return best;
    }

    @Override
    public void onMouseDragged(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {
        currentHover = worldPoint;
    }

    @Override
    public void onMouseReleased(MouseEvent event, Point2D worldPoint, Document document, CameraTransform camera) {}

    @Override
    public void renderOverlay(GraphicsContext gc, CameraTransform camera) {
        if (currentHover == null) return;
        Point2D mouseScreen = camera.worldToScreen(currentHover);

        // Highlight de Aresta em Hover (Stage 0)
        if (stage == 0 && hoveredEdge != null) {
            Point2D s = camera.worldToScreen(hoveredEdge.start());
            Point2D e = camera.worldToScreen(hoveredEdge.end());
            gc.setStroke(Color.web("#00E676"));
            gc.setLineWidth(3.0);
            gc.strokeLine(s.x(), s.y(), e.x(), e.y());

            gc.setFont(Font.font("Segoe UI", 12));
            gc.setFill(Color.web("#1E1E1E", 0.85));
            gc.fillRoundRect(mouseScreen.x() + 14, mouseScreen.y() - 22, 170, 24, 6, 6);
            gc.setFill(Color.web("#00E676"));
            gc.fillText("Clique para Cotar Aresta", mouseScreen.x() + 20, mouseScreen.y() - 6);
            return;
        }

        // Highlight de Curva/Fillet para Cota de Raio
        if (stage == 0 && hoveredArcCenter != null && hoveredArcRadius > 0) {
            Point2D c = camera.worldToScreen(hoveredArcCenter);
            double r = camera.worldToScreenLength(hoveredArcRadius);

            gc.setStroke(Color.web("#00E676"));
            gc.setLineWidth(2.5);
            gc.setLineDashes(4.0, 2.0);
            gc.strokeOval(c.x() - r, c.y() - r, r * 2, r * 2);
            gc.setLineDashes(null);

            String text = String.format("Cotar Raio: R %.2f mm", hoveredArcRadius);
            gc.setFont(Font.font("Segoe UI", 12));
            gc.setFill(Color.web("#1E1E1E", 0.85));
            gc.fillRoundRect(mouseScreen.x() + 14, mouseScreen.y() - 22, 180, 24, 6, 6);
            gc.setFill(Color.web("#00E676"));
            gc.fillText(text, mouseScreen.x() + 20, mouseScreen.y() - 6);
            return;
        }

        // Preview do Estágio 1 ou Estágio 2 da Cota Linear
        if (p1 != null) {
            Point2D s = camera.worldToScreen(p1);
            Point2D target = (p2 != null) ? p2 : currentHover;
            Point2D e = camera.worldToScreen(target);

            gc.setStroke(Color.web("#00E676"));
            gc.setLineWidth(1.2);
            gc.setLineDashes(4.0, 3.0);
            gc.strokeLine(s.x(), s.y(), e.x(), e.y());
            gc.setLineDashes(null);

            if (stage == 2 && p1 != null && p2 != null) {
                // Desenhar preview das extension lines
                Point2D offsetPt = camera.worldToScreen(currentHover);
                gc.strokeLine(e.x(), e.y(), offsetPt.x(), offsetPt.y());
            }

            double dist = p1.distanceTo(currentHover);
            String label = String.format("%.2f mm", dist);
            gc.setFont(Font.font("Consolas", 12));
            gc.setFill(Color.web("#1E1E1E", 0.85));
            gc.fillRoundRect(mouseScreen.x() + 14, mouseScreen.y() - 22, 120, 24, 6, 6);
            gc.setFill(Color.web("#00E676"));
            gc.fillText(label, mouseScreen.x() + 20, mouseScreen.y() - 6);
        }
    }

    @Override
    public void reset() {
        stage = 0;
        p1 = null;
        p2 = null;
        currentHover = null;
        hoveredEdge = null;
        hoveredArcCenter = null;
        hoveredArcRadius = 0.0;
    }
}
