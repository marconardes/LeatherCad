package com.leathercad.core.render;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;

public class EdgeFinishRenderer {

    /**
     * Renderiza o acabamento polido/pintado com chanfro nas bordas das peças de couro.
     */
    public static void renderBurnishedEdge(GraphicsContext gc, double x, double y, double w, double h, double cornerRadius, Color edgeColor) {
        double edgeWidth = 2.0;

        LinearGradient edgeGrad = new LinearGradient(
            x, y, x + w, y + h, false, CycleMethod.NO_CYCLE,
            new Stop(0.0, edgeColor.darker()),
            new Stop(0.5, edgeColor.brighter()),
            new Stop(1.0, edgeColor.darker())
        );

        gc.setStroke(edgeGrad);
        gc.setLineWidth(edgeWidth);
        if (cornerRadius > 0) {
            gc.strokeRoundRect(x, y, w, h, cornerRadius * 2.0, cornerRadius * 2.0);
        } else {
            gc.strokeRect(x, y, w, h);
        }

        // Linha interna de brilho do chanfro de borda
        gc.setStroke(Color.web("#FFFFFF", 0.30));
        gc.setLineWidth(0.8);
        if (cornerRadius > 0) {
            gc.strokeRoundRect(x + 1.0, y + 1.0, w - 2.0, h - 2.0, cornerRadius * 1.8, cornerRadius * 1.8);
        } else {
            gc.strokeRect(x + 1.0, y + 1.0, w - 2.0, h - 2.0);
        }
    }
}
