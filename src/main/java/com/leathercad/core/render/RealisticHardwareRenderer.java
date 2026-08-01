package com.leathercad.core.render;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;

public class RealisticHardwareRenderer {

    public enum MetalFinish {
        BRASS("Latão Polido"),
        ANTIQUE_BRASS("Latão Envelhecido"),
        SILVER("Prata Inox"),
        GUNMETAL("Gunmetal Escuro");

        private final String label;
        MetalFinish(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public static void renderMetallicSnap(GraphicsContext gc, double cx, double cy, double radius, MetalFinish finish) {
        Color baseColor = switch (finish) {
            case BRASS -> Color.web("#D4AC0D");
            case ANTIQUE_BRASS -> Color.web("#9A7D0A");
            case SILVER -> Color.web("#BDC3C7");
            case GUNMETAL -> Color.web("#34495E");
        };

        // Gradiente radial especular metálico 3D
        RadialGradient metalGrad = new RadialGradient(
            0, 0, cx - radius * 0.3, cy - radius * 0.3, radius * 1.2, false, CycleMethod.NO_CYCLE,
            new Stop(0.0, Color.WHITE),
            new Stop(0.3, baseColor.brighter()),
            new Stop(0.8, baseColor),
            new Stop(1.0, baseColor.darker())
        );

        gc.setFill(metalGrad);
        gc.fillOval(cx - radius, cy - radius, radius * 2.0, radius * 2.0);

        gc.setStroke(baseColor.darker());
        gc.setLineWidth(1.0);
        gc.strokeOval(cx - radius, cy - radius, radius * 2.0, radius * 2.0);

        // Anel interno metálico
        double rInner = radius * 0.55;
        gc.strokeOval(cx - rInner, cy - rInner, rInner * 2.0, rInner * 2.0);
    }
}
