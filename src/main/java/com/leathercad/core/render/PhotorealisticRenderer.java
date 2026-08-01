package com.leathercad.core.render;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.model.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;

public class PhotorealisticRenderer {

    public static void renderPhotorealistic(Document doc, Canvas canvas) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        // 1. Fundo estilo estúdio fotográfico profissional com iluminação vinheta (Spotlight Studio)
        RadialGradient studioBackground = new RadialGradient(
            0, 0, w / 2.0, h / 2.0, Math.max(w, h) * 0.7, false, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.web("#282C34")),
            new Stop(0.6, Color.web("#1A1D24")),
            new Stop(1.0, Color.web("#0D0E12"))
        );
        gc.setFill(studioBackground);
        gc.fillRect(0, 0, w, h);

        // 2. Cálculo do Bounding Box automático do projeto para Auto-Fit (Sem espaço em branco desnecessário)
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

        for (CADElement elem : doc.getElements()) {
            if (elem instanceof RectElement rectElem) {
                var rect = rectElem.rect();
                minX = Math.min(minX, rect.minPoint().x());
                minY = Math.min(minY, rect.minPoint().y());
                maxX = Math.max(maxX, rect.minPoint().x() + rect.width());
                maxY = Math.max(maxY, rect.minPoint().y() + rect.height());
            } else if (elem instanceof LineElement lineElem) {
                minX = Math.min(minX, Math.min(lineElem.line().start().x(), lineElem.line().end().x()));
                minY = Math.min(minY, Math.min(lineElem.line().start().y(), lineElem.line().end().y()));
                maxX = Math.max(maxX, Math.max(lineElem.line().start().x(), lineElem.line().end().x()));
                maxY = Math.max(maxY, Math.max(lineElem.line().start().y(), lineElem.line().end().y()));
            } else if (elem instanceof CircleElement circleElem) {
                var c = circleElem.circle();
                minX = Math.min(minX, c.center().x() - c.radius());
                minY = Math.min(minY, c.center().y() - c.radius());
                maxX = Math.max(maxX, c.center().x() + c.radius());
                maxY = Math.max(maxY, c.center().y() + c.radius());
            }
        }

        double docW = maxX - minX;
        double docH = maxY - minY;

        double scale = 1.0;
        double offsetX = 40.0;
        double offsetY = 40.0;

        if (docW > 0 && docH > 0) {
            double margin = 60.0;
            double targetW = w - margin * 2.0;
            double targetH = h - margin * 2.0;

            scale = Math.min(targetW / docW, targetH / docH);
            offsetX = (w - (docW * scale)) / 2.0 - (minX * scale);
            offsetY = (h - (docH * scale)) / 2.0 - (minY * scale);
        }

        // 3. Renderização em camadas (Bottom to Top) escaladas e centralizadas
        for (Layer layer : doc.getLayers()) {
            if (!layer.isVisible()) continue;

            Color baseColor = Color.web(layer.getColorHex());

            for (CADElement elem : doc.getElements()) {
                if (!elem.layerId().equals(layer.getId())) continue;

                if (elem instanceof RectElement rectElem) {
                    var rect = rectElem.rect();
                    double rx = rect.minPoint().x() * scale + offsetX;
                    double ry = rect.minPoint().y() * scale + offsetY;
                    double rw = rect.width() * scale;
                    double rh = rect.height() * scale;
                    double cr = rect.cornerRadius() * scale;

                    // Sombra projetada suave (Drop Shadow)
                    LeatherTextureGenerator.applyDropShadow(gc, rx, ry, rw, rh, cr, 8.0);

                    // Textura procedural de grão de couro + iluminação especular
                    LeatherTextureGenerator.applyProceduralGrain(gc, rx, ry, rw, rh, cr, baseColor);

                    // Acabamento de bordas polidas (Burnished Edge)
                    EdgeFinishRenderer.renderBurnishedEdge(gc, rx, ry, rw, rh, cr, baseColor);

                } else if (elem instanceof LineElement lineElem) {
                    Point2D s = lineElem.line().start();
                    Point2D e = lineElem.line().end();
                    gc.setStroke(baseColor);
                    gc.setLineWidth(Math.max(1.5, 2.0 * scale));
                    gc.strokeLine(s.x() * scale + offsetX, s.y() * scale + offsetY, e.x() * scale + offsetX, e.y() * scale + offsetY);

                } else if (elem instanceof com.leathercad.core.leather.CreaseElement creaseElem) {
                    Point2D s = creaseElem.line().start();
                    Point2D e = creaseElem.line().end();
                    gc.setStroke(Color.web("#FF8C00"));
                    gc.setLineWidth(Math.max(1.2, 1.5 * scale));
                    gc.strokeLine(s.x() * scale + offsetX, s.y() * scale + offsetY, e.x() * scale + offsetX, e.y() * scale + offsetY);

                } else if (elem instanceof com.leathercad.core.leather.StitchElement stitchElem) {
                    // Costuras 3D enceradas fotorrealistas escaladas
                    RealisticStitchRenderer.render3DStitches(gc, stitchElem, scale, offsetX, offsetY);

                } else if (elem instanceof CircleElement circleElem) {
                    var circle = circleElem.circle();
                    double cx = circle.center().x() * scale + offsetX;
                    double cy = circle.center().y() * scale + offsetY;
                    double cr = circle.radius() * scale;
                    // Ferragem metálica com reflexo 3D
                    RealisticHardwareRenderer.renderMetallicSnap(
                        gc, cx, cy, cr, RealisticHardwareRenderer.MetalFinish.BRASS
                    );
                } else if (elem instanceof ArcElement arcElem) {
                    var arc = arcElem.arc();
                    double cx = arc.center().x() * scale + offsetX;
                    double cy = arc.center().y() * scale + offsetY;
                    double cr = arc.radius() * scale;
                    gc.setStroke(baseColor);
                    gc.setLineWidth(Math.max(1.5, 2.0 * scale));
                    gc.strokeArc(cx - cr, cy - cr, cr * 2, cr * 2, -arc.startAngleDegrees(), -arc.sweepAngleDegrees(), javafx.scene.shape.ArcType.OPEN);

                } else if (elem instanceof BezierElement bezierElem) {
                    var b = bezierElem.bezier();
                    gc.setStroke(baseColor);
                    gc.setLineWidth(Math.max(1.5, 2.0 * scale));
                    gc.beginPath();
                    gc.moveTo(b.start().x() * scale + offsetX, b.start().y() * scale + offsetY);
                    gc.bezierCurveTo(
                        b.control1().x() * scale + offsetX, b.control1().y() * scale + offsetY,
                        b.control2().x() * scale + offsetX, b.control2().y() * scale + offsetY,
                        b.end().x() * scale + offsetX, b.end().y() * scale + offsetY
                    );
                    gc.stroke();
                }
            }
        }
    }
}
