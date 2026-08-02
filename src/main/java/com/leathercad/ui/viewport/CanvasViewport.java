package com.leathercad.ui.viewport;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.model.*;
import com.leathercad.ui.tools.ToolManager;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;

import java.util.function.BiConsumer;

public class CanvasViewport extends Canvas {
    private final CameraTransform camera = new CameraTransform();
    private final Document document;
    private final ToolManager toolManager;

    private double lastPanMouseX;
    private double lastPanMouseY;
    private boolean isPanning = false;

    private BiConsumer<Point2D, Double> cursorPositionListener;

    public CanvasViewport(Document document, ToolManager toolManager) {
        this.document = document;
        this.toolManager = toolManager;

        setFocusTraversable(true);
        setupEventListeners();
    }

    public void setCursorPositionListener(BiConsumer<Point2D, Double> listener) {
        this.cursorPositionListener = listener;
    }

    public CameraTransform getCamera() { return camera; }

    private double panStartMouseX = 0;
    private double panStartMouseY = 0;
    private double totalPanDist = 0;

    private void setupEventListeners() {
        setOnScroll((ScrollEvent event) -> {
            double delta = event.getDeltaY();
            if (Math.abs(delta) < 1e-3) return;
            double zoomFactor = delta > 0 ? 1.12 : 1.0 / 1.12;
            camera.zoomAt(event.getX(), event.getY(), zoomFactor);
            redraw();
            notifyCursor(event.getX(), event.getY());
        });

        setOnKeyPressed((KeyEvent event) -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                toolManager.resetActiveTool();
                document.clearSelection();
                redraw();
            } else if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
                document.deleteSelected();
                redraw();
            }
        });

        setOnMousePressed((MouseEvent event) -> {
            requestFocus();
            if (event.getButton() == MouseButton.MIDDLE || event.getButton() == MouseButton.SECONDARY) {
                isPanning = true;
                lastPanMouseX = event.getX();
                lastPanMouseY = event.getY();
                panStartMouseX = event.getX();
                panStartMouseY = event.getY();
                totalPanDist = 0.0;
            } else if (event.getButton() == MouseButton.PRIMARY) {
                Point2D worldPoint = getSnappedWorldPoint(event.getX(), event.getY());
                toolManager.getActiveTool().onMousePressed(event, worldPoint, document, camera);
                redraw();
            }
        });

        setOnMouseDragged((MouseEvent event) -> {
            if (isPanning) {
                double dx = event.getX() - lastPanMouseX;
                double dy = event.getY() - lastPanMouseY;
                totalPanDist += Math.hypot(event.getX() - panStartMouseX, event.getY() - panStartMouseY);
                camera.panBy(dx, dy);
                lastPanMouseX = event.getX();
                lastPanMouseY = event.getY();
                redraw();
            } else {
                Point2D worldPoint = getSnappedWorldPoint(event.getX(), event.getY());
                toolManager.getActiveTool().onMouseDragged(event, worldPoint, document, camera);
                redraw();
            }
            notifyCursor(event.getX(), event.getY());
        });

        setOnMouseReleased((MouseEvent event) -> {
            if (isPanning) {
                isPanning = false;
                // Padrão FreeCAD: Clique rápido com Botão Direito (sem arraste) cancela a ferramenta atual e volta para Seleção
                if (event.getButton() == MouseButton.SECONDARY && totalPanDist < 5.0) {
                    toolManager.resetActiveTool();
                    document.clearSelection();
                    redraw();
                }
            } else if (event.getButton() == MouseButton.PRIMARY) {
                Point2D worldPoint = getSnappedWorldPoint(event.getX(), event.getY());
                toolManager.getActiveTool().onMouseReleased(event, worldPoint, document, camera);
                redraw();
            }
        });

        setOnMouseMoved((MouseEvent event) -> {
            Point2D worldPoint = getSnappedWorldPoint(event.getX(), event.getY());
            toolManager.getActiveTool().onMouseMoved(event, worldPoint, document, camera);
            redraw();
            notifyCursor(event.getX(), event.getY());
        });
    }

    private void notifyCursor(double screenX, double screenY) {
        if (cursorPositionListener != null) {
            Point2D world = camera.screenToWorld(screenX, screenY);
            cursorPositionListener.accept(world, camera.getZoom());
        }
    }

    private com.leathercad.core.snap.SnapEngine.SnapResult activeSnap = null;

    private Point2D getSnappedWorldPoint(double screenX, double screenY) {
        Point2D rawWorld = camera.screenToWorld(screenX, screenY);
        double snapRadiusMm = 8.0 / camera.getZoom();
        var snap = com.leathercad.core.snap.SnapEngine.findSnap(rawWorld, document, snapRadiusMm, 1.0);
        if (snap.isPresent()) {
            activeSnap = snap.get();
            return activeSnap.point();
        } else {
            activeSnap = null;
            return rawWorld;
        }
    }

    private boolean isDarkMode = true;

    public boolean isDarkMode() { return true; }

    public void setDarkMode(boolean darkMode) {
        this.isDarkMode = true;
        redraw();
    }

    public void redraw() {
        GraphicsContext gc = getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();

        if (w <= 0 || h <= 0) return;

        // Fundo CAD (Escuro #1E1E1E / Claro #F5F5F7)
        gc.setFill(isDarkMode ? Color.web("#1E1E1E") : Color.web("#F5F5F7"));
        gc.fillRect(0, 0, w, h);

        // Desenhar Grid milimétrico
        drawGrid(gc, w, h);

        // Desenhar Elementos por Camada
        drawElements(gc);

        // Renderizar Overlay da Ferramenta Ativa
        toolManager.getActiveTool().renderOverlay(gc, camera);

        // Desenhar Indicador de Snap (Verde Neon)
        drawSnapIndicator(gc);

        // Desenhar Réguas
        drawRulers(gc, w, h);
    }

    private void drawSnapIndicator(GraphicsContext gc) {
        if (activeSnap == null) return;

        Point2D screen = camera.worldToScreen(activeSnap.point());
        double s = 6.0;

        gc.setStroke(Color.web("#00FF88"));
        gc.setLineWidth(2.0);
        gc.strokeRect(screen.x() - s, screen.y() - s, s * 2, s * 2);

        gc.setFill(Color.web("#00FF88"));
        gc.setFont(javafx.scene.text.Font.font(11));
        gc.fillText(activeSnap.label(), screen.x() + s + 4, screen.y() + 4);
    }

    private void drawGrid(GraphicsContext gc, double width, double height) {
        double zoom = camera.getZoom();
        double panX = camera.getPanX();
        double panY = camera.getPanY();

        double gridSpacingMm = 10.0;
        if (zoom > 12.0) gridSpacingMm = 1.0;
        else if (zoom > 4.0) gridSpacingMm = 5.0;
        else if (zoom > 0.8) gridSpacingMm = 10.0;
        else if (zoom > 0.2) gridSpacingMm = 50.0;
        else gridSpacingMm = 100.0;

        double screenSpacing = gridSpacingMm * zoom;
        if (screenSpacing < 4.0) return;

        gc.setLineWidth(1.0);
        gc.setStroke(isDarkMode ? Color.web("#2A2A2C") : Color.web("#E0E0E5"));

        double startX = (panX % screenSpacing);
        if (startX > 0) startX -= screenSpacing;
        for (double x = startX; x < width; x += screenSpacing) {
            gc.strokeLine(x, 0, x, height);
        }

        double startY = (panY % screenSpacing);
        if (startY > 0) startY -= screenSpacing;
        for (double y = startY; y < height; y += screenSpacing) {
            gc.strokeLine(0, y, width, y);
        }

        // Eixos Origem (0,0) em Verde/Vermelho sutis
        Point2D originScreen = camera.worldToScreen(Point2D.ZERO);
        gc.setLineWidth(1.5);
        gc.setStroke(Color.web("#E55039", 0.7)); // Eixo X em vermelho
        gc.strokeLine(0, originScreen.y(), width, originScreen.y());

        gc.setStroke(Color.web("#78E08F", 0.7)); // Eixo Y em verde
        gc.strokeLine(originScreen.x(), 0, originScreen.x(), height);
    }

    private String resolveEffectiveLayerId(CADElement elem) {
        if (elem.layerId() != null && document.findLayerById(elem.layerId()) != null) {
            return elem.layerId();
        }
        if (elem instanceof com.leathercad.core.leather.StitchElement) {
            return document.getStitchLayerId();
        }
        if (elem instanceof com.leathercad.core.leather.CreaseElement) {
            return document.getCreaseLayerId();
        }
        return document.getLeatherLayerId();
    }

    private void drawElements(GraphicsContext gc) {
        for (Layer layer : document.getLayers()) {
            if (!layer.isVisible()) continue;

            Color layerColor = Color.web(layer.getColorHex());
            gc.setStroke(layerColor);
            gc.setLineWidth(1.5);

            for (CADElement elem : document.getElements()) {
                if (elem instanceof CADGroup groupElem) {
                    for (CADElement child : groupElem.children()) {
                        String childLayerId = resolveEffectiveLayerId(child);
                        if (childLayerId.equals(layer.getId())) {
                            boolean isSelected = document.getSelectedElementIds().contains(groupElem.id()) || document.getSelectedElementIds().contains(child.id());
                            renderSingleElement(gc, layer, child, isSelected);
                        }
                    }
                } else {
                    String elemLayerId = resolveEffectiveLayerId(elem);
                    if (elemLayerId.equals(layer.getId())) {
                        boolean isSelected = document.getSelectedElementIds().contains(elem.id());
                        renderSingleElement(gc, layer, elem, isSelected);
                    }
                }
            }
        }
    }

    private void renderSingleElement(GraphicsContext gc, Layer layer, CADElement elem, boolean isSelected) {
        boolean isWholeElementSelected = isSelected && document.getSelectedSubElements().isEmpty();

        if (isWholeElementSelected) {
            // Halo de Brilho de Seleção Vibrante (quando a peça INTEIRA está selecionada)
            gc.save();
            gc.setStroke(isDarkMode ? Color.web("#FF0055", 0.50) : Color.web("#FF0055", 0.40));
            gc.setLineWidth(7.0);
            drawGeometryPathOnly(gc, layer, elem);
            gc.restore();
        }

        // Renderizar Destaque Estilo FreeCAD para Sub-Elementos Selecionados (Sub-Arestas e Vértices)
        if (!document.getSelectedSubElements().isEmpty()) {
            for (SubElementRef subRef : document.getSelectedSubElements()) {
                if (subRef.elementId().equals(elem.id())) {
                    renderSubElementHighlight(gc, elem, subRef);
                }
            }
        }

        Color strokeColor = isWholeElementSelected ? (isDarkMode ? Color.web("#FF0055") : Color.web("#D60000")) : Color.web(layer.getColorHex());
        double strokeWidth = isWholeElementSelected ? 3.0 : 1.5;

        gc.setStroke(strokeColor);
        gc.setLineWidth(strokeWidth);

        if (elem instanceof LineElement lineElem) {
            Point2D s = camera.worldToScreen(lineElem.line().start());
            Point2D e = camera.worldToScreen(lineElem.line().end());
            gc.strokeLine(s.x(), s.y(), e.x(), e.y());
        } else if (elem instanceof RectElement rectElem) {
            var rect = rectElem.rect();
            Point2D min = camera.worldToScreen(rect.minPoint());
            double w = camera.worldToScreenLength(rect.width());
            double h = camera.worldToScreenLength(rect.height());

            double rTL = camera.worldToScreenLength(rect.rTopLeft());
            double rTR = camera.worldToScreenLength(rect.rTopRight());
            double rBR = camera.worldToScreenLength(rect.rBottomRight());
            double rBL = camera.worldToScreenLength(rect.rBottomLeft());

            // Preenchimento interno vibrante em Tom de Azul Couro CAD (#0A84FF)
            Color fillBlue = Color.web("#0A84FF", isDarkMode ? 0.35 : 0.25);
            gc.setFill(fillBlue);
            if (rTL > 0 || rTR > 0 || rBR > 0 || rBL > 0) {
                drawRectPath(gc, min.x(), min.y(), w, h, rTL, rTR, rBR, rBL, true);
            } else {
                gc.fillRect(min.x(), min.y(), w, h);
            }

            gc.setStroke(strokeColor);
            gc.setLineWidth(strokeWidth);
            if (rTL > 0 || rTR > 0 || rBR > 0 || rBL > 0) {
                drawRectPath(gc, min.x(), min.y(), w, h, rTL, rTR, rBR, rBL, false);
            } else {
                gc.strokeRect(min.x(), min.y(), w, h);
            }
        } else if (elem instanceof com.leathercad.core.leather.CreaseElement creaseElem) {
            Point2D s = camera.worldToScreen(creaseElem.line().start());
            Point2D e = camera.worldToScreen(creaseElem.line().end());
            gc.setStroke(isSelected ? Color.web("#FF0055") : Color.web("#FF8C00"));
            gc.setLineWidth(isSelected ? 2.5 : 1.2);
            gc.strokeLine(s.x(), s.y(), e.x(), e.y());
        } else if (elem instanceof com.leathercad.core.leather.SkivingElement skiveElem) {
            Point2D min = camera.worldToScreen(skiveElem.area().minPoint());
            double w = camera.worldToScreenLength(skiveElem.area().width());
            double h = camera.worldToScreenLength(skiveElem.area().height());
            gc.setStroke(isSelected ? Color.web("#FF0055") : Color.web("#A0A0A0"));
            gc.setLineWidth(isSelected ? 2.5 : 1.0);
            gc.setLineDashes(2.0);
            gc.strokeRect(min.x(), min.y(), w, h);
            gc.setLineDashes(null);
        } else if (elem instanceof com.leathercad.core.leather.StitchElement stitchElem) {
            Color stitchColor = isSelected ? Color.web("#FF0055") : Color.web("#FFD700");
            gc.setStroke(stitchColor);
            gc.setFill(stitchColor);

            com.leathercad.core.leather.StitchType type = stitchElem.config().type();

            if (type == com.leathercad.core.leather.StitchType.MACHINE_STITCH) {
                gc.setLineWidth(isSelected ? 2.5 : 1.5);
                gc.setLineDashes(6.0, 4.0);
                Point2D s = camera.worldToScreen(stitchElem.baseLine().start());
                Point2D e = camera.worldToScreen(stitchElem.baseLine().end());
                gc.strokeLine(s.x(), s.y(), e.x(), e.y());
                gc.setLineDashes(null);
            } else if (type == com.leathercad.core.leather.StitchType.ROUND_PUNCH || type == com.leathercad.core.leather.StitchType.ROUND) {
                gc.setLineWidth(isSelected ? 2.0 : 1.0);
                gc.setLineDashes(3.0, 3.0);
                Point2D s = camera.worldToScreen(stitchElem.baseLine().start());
                Point2D e = camera.worldToScreen(stitchElem.baseLine().end());
                gc.strokeLine(s.x(), s.y(), e.x(), e.y());
                gc.setLineDashes(null);

                for (Point2D hole : stitchElem.holePoints()) {
                    Point2D p = camera.worldToScreen(hole);
                    double hr = Math.max(1.5, camera.worldToScreenLength(stitchElem.config().holeDiameterMm() / 2.0));
                    gc.strokeOval(p.x() - hr, p.y() - hr, hr * 2, hr * 2);
                }
            } else {
                // FRENCH_SLANT / DEFAULT
                gc.setLineWidth(isSelected ? 1.5 : 1.0);
                gc.setLineDashes(3.0, 3.0);
                Point2D s = camera.worldToScreen(stitchElem.baseLine().start());
                Point2D e = camera.worldToScreen(stitchElem.baseLine().end());
                gc.strokeLine(s.x(), s.y(), e.x(), e.y());
                gc.setLineDashes(null);

                gc.setLineWidth(isSelected ? 2.5 : 1.8);
                var slots = stitchElem.calculateSlantSlots();
                for (var slot : slots) {
                    Point2D s1 = camera.worldToScreen(slot.start());
                    Point2D s2 = camera.worldToScreen(slot.end());
                    gc.strokeLine(s1.x(), s1.y(), s2.x(), s2.y());
                }
            }
        } else if (elem instanceof CircleElement circleElem) {
            Point2D c = camera.worldToScreen(circleElem.circle().center());
            double r = camera.worldToScreenLength(circleElem.circle().radius());
            gc.setFill(Color.web(layer.getColorHex(), 0.40));
            gc.fillOval(c.x() - r, c.y() - r, r * 2, r * 2);
            gc.strokeOval(c.x() - r, c.y() - r, r * 2, r * 2);
        } else if (elem instanceof ArcElement arcElem) {
            Point2D c = camera.worldToScreen(arcElem.arc().center());
            double r = camera.worldToScreenLength(arcElem.arc().radius());
            gc.strokeArc(c.x() - r, c.y() - r, r * 2, r * 2, -arcElem.arc().startAngleDegrees(), -arcElem.arc().sweepAngleDegrees(), javafx.scene.shape.ArcType.OPEN);
        } else if (elem instanceof BezierElement bezierElem) {
            Point2D s0 = camera.worldToScreen(bezierElem.bezier().start());
            Point2D c1 = camera.worldToScreen(bezierElem.bezier().control1());
            Point2D c2 = camera.worldToScreen(bezierElem.bezier().control2());
            Point2D s1 = camera.worldToScreen(bezierElem.bezier().end());

            gc.beginPath();
            gc.moveTo(s0.x(), s0.y());
            gc.bezierCurveTo(c1.x(), c1.y(), c2.x(), c2.y(), s1.x(), s1.y());
            gc.stroke();
        } else if (elem instanceof PolylineElement polyElem) {
            var poly = polyElem.polyline();
            var pts = poly.points();
            if (pts.size() >= 2) {
                double[] xPoints = new double[pts.size()];
                double[] yPoints = new double[pts.size()];
                for (int i = 0; i < pts.size(); i++) {
                    Point2D p = camera.worldToScreen(pts.get(i));
                    xPoints[i] = p.x();
                    yPoints[i] = p.y();
                }
                if (poly.isClosed()) {
                    gc.setFill(Color.web(layer.getColorHex(), 0.40));
                    gc.fillPolygon(xPoints, yPoints, pts.size());
                    gc.strokePolygon(xPoints, yPoints, pts.size());
                } else {
                    gc.strokePolyline(xPoints, yPoints, pts.size());
                }

                // Renderizar Dimensões das Arestas e Ângulos dos Vértices na Tela (CAD Overlays)
                gc.save();
                gc.setFont(javafx.scene.text.Font.font("Consolas", 10));
                int numEdges = poly.isClosed() ? pts.size() : pts.size() - 1;

                for (int i = 0; i < numEdges; i++) {
                    Point2D p1 = pts.get(i);
                    Point2D p2 = pts.get((i + 1) % pts.size());
                    Point2D midWorld = p1.lerp(p2, 0.5);
                    Point2D midScreen = camera.worldToScreen(midWorld);

                    double len = poly.getSegmentLength(i);
                    String labelText = String.format("%.1f mm", len);

                    gc.setFill(isDarkMode ? Color.web("#00FF88") : Color.web("#007A3D"));
                    gc.fillText(labelText, midScreen.x() + 4, midScreen.y() - 4);
                }

                for (int i = 0; i < pts.size(); i++) {
                    double angle = poly.getVertexAngleDegrees(i);
                    if (angle > 0.01) {
                        Point2D vScreen = camera.worldToScreen(pts.get(i));
                        String angleText = String.format("%.1f°", angle);
                        gc.setFill(isDarkMode ? Color.web("#FFCC00") : Color.web("#B88600"));
                        gc.fillText(angleText, vScreen.x() + 6, vScreen.y() + 12);
                    }
                }
                gc.restore();
            }
        } else if (elem instanceof DimensionElement dimElem) {
            Point2D s = camera.worldToScreen(dimElem.start());
            Point2D e = camera.worldToScreen(dimElem.end());

            gc.setStroke(isSelected ? Color.web("#FF0055") : Color.web("#00FF88"));
            gc.setLineWidth(isSelected ? 2.5 : 1.2);
            gc.setLineDashes(3.0);
            gc.strokeLine(s.x(), s.y(), e.x(), e.y());
            gc.setLineDashes(null);

            gc.setFill(isSelected ? Color.web("#FF0055") : Color.web("#00FF88"));
            gc.setFont(javafx.scene.text.Font.font("Consolas", 11));
            double midX = (s.x() + e.x()) / 2.0;
            double midY = (s.y() + e.y()) / 2.0 - 4;
            gc.fillText(dimElem.formattedText(), midX, midY);
        }
    }

    private void drawGeometryPathOnly(GraphicsContext gc, Layer layer, CADElement elem) {
        if (elem instanceof LineElement lineElem) {
            Point2D s = camera.worldToScreen(lineElem.line().start());
            Point2D e = camera.worldToScreen(lineElem.line().end());
            gc.strokeLine(s.x(), s.y(), e.x(), e.y());
        } else if (elem instanceof RectElement rectElem) {
            var rect = rectElem.rect();
            Point2D min = camera.worldToScreen(rect.minPoint());
            double w = camera.worldToScreenLength(rect.width());
            double h = camera.worldToScreenLength(rect.height());
            double rTL = camera.worldToScreenLength(rect.rTopLeft());
            double rTR = camera.worldToScreenLength(rect.rTopRight());
            double rBR = camera.worldToScreenLength(rect.rBottomRight());
            double rBL = camera.worldToScreenLength(rect.rBottomLeft());
            if (rTL > 0 || rTR > 0 || rBR > 0 || rBL > 0) {
                drawRectPath(gc, min.x(), min.y(), w, h, rTL, rTR, rBR, rBL, false);
            } else {
                gc.strokeRect(min.x(), min.y(), w, h);
            }
        } else if (elem instanceof CircleElement circleElem) {
            Point2D c = camera.worldToScreen(circleElem.circle().center());
            double r = camera.worldToScreenLength(circleElem.circle().radius());
            gc.strokeOval(c.x() - r, c.y() - r, r * 2, r * 2);
        } else if (elem instanceof ArcElement arcElem) {
            Point2D c = camera.worldToScreen(arcElem.arc().center());
            double r = camera.worldToScreenLength(arcElem.arc().radius());
            gc.strokeArc(c.x() - r, c.y() - r, r * 2, r * 2, -arcElem.arc().startAngleDegrees(), -arcElem.arc().sweepAngleDegrees(), javafx.scene.shape.ArcType.OPEN);
        } else if (elem instanceof PolylineElement polyElem) {
            var pts = polyElem.polyline().points();
            if (pts.size() >= 2) {
                double[] xPoints = new double[pts.size()];
                double[] yPoints = new double[pts.size()];
                for (int i = 0; i < pts.size(); i++) {
                    Point2D p = camera.worldToScreen(pts.get(i));
                    xPoints[i] = p.x();
                    yPoints[i] = p.y();
                }
                if (polyElem.polyline().isClosed()) {
                    gc.setFill(Color.web("#0A84FF", isDarkMode ? 0.35 : 0.25));
                    gc.fillPolygon(xPoints, yPoints, pts.size());
                    gc.strokePolygon(xPoints, yPoints, pts.size());
                } else {
                    gc.strokePolyline(xPoints, yPoints, pts.size());
                }
            }
        }
    }

    private void drawRectPath(GraphicsContext gc, double x, double y, double w, double h, double rTL, double rTR, double rBR, double rBL, boolean fill) {
        gc.beginPath();
        gc.moveTo(x + rTL, y);
        gc.lineTo(x + w - rTR, y);
        if (rTR > 0) gc.arcTo(x + w, y, x + w, y + rTR, rTR);
        gc.lineTo(x + w, y + h - rBR);
        if (rBR > 0) gc.arcTo(x + w, y + h, x + w - rBR, y + h, rBR);
        gc.lineTo(x + rBL, y + h);
        if (rBL > 0) gc.arcTo(x, y + h, x, y + h - rBL, rBL);
        gc.lineTo(x, y + rTL);
        if (rTL > 0) gc.arcTo(x, y, x + rTL, y, rTL);
        gc.closePath();
        if (fill) gc.fill();
        gc.stroke();
    }

    private void renderSubElementHighlight(GraphicsContext gc, CADElement elem, SubElementRef subRef) {
        gc.save();
        // 1. Halo de brilho da sub-aresta (8px)
        gc.setStroke(Color.web("#FF0055", 0.45));
        gc.setLineWidth(8.0);
        drawSubElementSegment(gc, elem, subRef);

        // 2. Traço da sub-aresta (3.5px) em Rosa Magenta Vibrante (#FF0055)
        gc.setStroke(Color.web("#FF0055"));
        gc.setFill(Color.web("#00FF88"));
        gc.setLineWidth(3.5);
        drawSubElementSegment(gc, elem, subRef);
        gc.restore();
    }

    private void drawSubElementSegment(GraphicsContext gc, CADElement elem, SubElementRef subRef) {
        if (elem instanceof RectElement rectElem) {
            var rect = rectElem.rect();
            Point2D v0 = camera.worldToScreen(rect.minPoint());
            Point2D v1 = camera.worldToScreen(new Point2D(rect.minPoint().x() + rect.width(), rect.minPoint().y()));
            Point2D v2 = camera.worldToScreen(new Point2D(rect.minPoint().x() + rect.width(), rect.minPoint().y() + rect.height()));
            Point2D v3 = camera.worldToScreen(new Point2D(rect.minPoint().x(), rect.minPoint().y() + rect.height()));

            if (subRef.type() == SubElementRef.SubElementType.EDGE) {
                if (subRef.index() == 0) gc.strokeLine(v0.x(), v0.y(), v1.x(), v1.y());
                else if (subRef.index() == 1) gc.strokeLine(v1.x(), v1.y(), v2.x(), v2.y());
                else if (subRef.index() == 2) gc.strokeLine(v3.x(), v3.y(), v2.x(), v2.y());
                else if (subRef.index() == 3) gc.strokeLine(v0.x(), v0.y(), v3.x(), v3.y());
            } else if (subRef.type() == SubElementRef.SubElementType.VERTEX) {
                Point2D target = (subRef.index() == 0) ? v0 : (subRef.index() == 1) ? v1 : (subRef.index() == 2) ? v2 : v3;
                gc.fillRect(target.x() - 5, target.y() - 5, 10, 10);
            }
        } else if (elem instanceof PolylineElement polyElem) {
            var pts = polyElem.polyline().points();
            if (subRef.type() == SubElementRef.SubElementType.VERTEX && subRef.index() < pts.size()) {
                Point2D sp = camera.worldToScreen(pts.get(subRef.index()));
                gc.fillRect(sp.x() - 5, sp.y() - 5, 10, 10);
            } else if (subRef.type() == SubElementRef.SubElementType.EDGE) {
                int idx = subRef.index();
                if (idx < pts.size() - 1) {
                    Point2D p1 = camera.worldToScreen(pts.get(idx));
                    Point2D p2 = camera.worldToScreen(pts.get(idx + 1));
                    gc.strokeLine(p1.x(), p1.y(), p2.x(), p2.y());
                } else if (polyElem.polyline().isClosed() && idx == pts.size() - 1) {
                    Point2D p1 = camera.worldToScreen(pts.get(pts.size() - 1));
                    Point2D p2 = camera.worldToScreen(pts.get(0));
                    gc.strokeLine(p1.x(), p1.y(), p2.x(), p2.y());
                }
            }
        }
    }

    private void drawRulers(GraphicsContext gc, double width, double height) {
        double rulerSize = 24.0;
        gc.setFill(isDarkMode ? Color.web("#252526") : Color.web("#E8E8ED"));
        gc.fillRect(0, 0, width, rulerSize);
        gc.fillRect(0, 0, rulerSize, height);

        gc.setStroke(isDarkMode ? Color.web("#555555") : Color.web("#CCCCCC"));
        gc.setLineWidth(1.0);
        gc.strokeLine(0, rulerSize, width, rulerSize);
        gc.strokeLine(rulerSize, 0, rulerSize, height);

        // Canto superior esquerdo (0,0) das réguas
        gc.setFill(isDarkMode ? Color.web("#333333") : Color.web("#D1D1D6"));
        gc.fillRect(0, 0, rulerSize, rulerSize);
    }
}
