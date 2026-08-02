package com.leathercad.core.render;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.leather.StitchElement;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;

public class RealisticStitchRenderer {

    public static void render3DStitches(GraphicsContext gc, StitchElement stitch, double scale, double offsetX, double offsetY) {
        // Renderiza furações (chisel hole / slant slot depth)
        var cfg = stitch.config();
        if (cfg.type() == com.leathercad.core.leather.StitchType.ROUND_PUNCH || cfg.type() == com.leathercad.core.leather.StitchType.ROUND) {
            for (Point2D hole : stitch.holePoints()) {
                double hx = hole.x() * scale + offsetX;
                double hy = hole.y() * scale + offsetY;
                double r = Math.max(2.0, (cfg.holeDiameterMm() * scale) / 2.0);

                gc.setFill(Color.web("#111111", 0.85));
                gc.fillOval(hx - r, hy - r, r * 2.0, r * 2.0);
                gc.setStroke(Color.web("#333333"));
                gc.setLineWidth(0.8);
                gc.strokeOval(hx - r, hy - r, r * 2.0, r * 2.0);
            }
        } else if (cfg.type() != com.leathercad.core.leather.StitchType.MACHINE_STITCH) {
            // FRENCH_SLANT / DEFAULT
            var slots = stitch.calculateSlantSlots();
            for (var slot : slots) {
                double s1x = slot.start().x() * scale + offsetX;
                double s1y = slot.start().y() * scale + offsetY;
                double s2x = slot.end().x() * scale + offsetX;
                double s2y = slot.end().y() * scale + offsetY;

                gc.setStroke(Color.web("#111111", 0.90));
                gc.setLineWidth(Math.max(2.0, 2.5 * scale));
                gc.strokeLine(s1x, s1y, s2x, s2y);

                gc.setStroke(Color.web("#333333", 0.70));
                gc.setLineWidth(0.8);
                gc.strokeLine(s1x, s1y, s2x, s2y);
            }
        }

        // Renderiza cada ponto de costura 3D com brilho especular de fio encerado
        var holes = stitch.holePoints();
        for (int i = 0; i < holes.size() - 1; i++) {
            Point2D p1 = holes.get(i);
            Point2D p2 = holes.get(i + 1);

            double h1x = p1.x() * scale + offsetX;
            double h1y = p1.y() * scale + offsetY;
            double h2x = p2.x() * scale + offsetX;
            double h2y = p2.y() * scale + offsetY;

            LinearGradient threadGrad = new LinearGradient(
                h1x, h1y, h2x, h2y, false, CycleMethod.NO_CYCLE,
                new Stop(0.0, Color.web("#FFEAA7")),
                new Stop(0.5, Color.web("#FFD700")),
                new Stop(1.0, Color.web("#D4AC0D"))
            );

            gc.setStroke(threadGrad);
            gc.setLineWidth(Math.max(1.8, 2.2 * Math.min(scale, 1.5)));
            gc.strokeLine(h1x, h1y, h2x, h2y);

            gc.setStroke(Color.web("#FFFFFF", 0.6));
            gc.setLineWidth(0.8);
            gc.strokeLine(h1x + 0.3, h1y - 0.3, h2x + 0.3, h2y - 0.3);
        }
    }
}
