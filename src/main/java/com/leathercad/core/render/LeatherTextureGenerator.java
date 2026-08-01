package com.leathercad.core.render;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.Stop;

import java.util.Random;

public class LeatherTextureGenerator {

    /**
     * Aplica brilho de estúdio, cor do couro e textura procedural de grão sobre uma peça de couro com cantos arredondados.
     */
    public static void applyProceduralGrain(GraphicsContext gc, double x, double y, double w, double h, double cornerRadius, Color baseColor) {
        // 1. Cor base com iluminação gradiente de estúdio (top-left light source)
        LinearGradient studioLighting = new LinearGradient(
            x, y, x + w, y + h, false, CycleMethod.NO_CYCLE,
            new Stop(0.0, baseColor.brighter()),
            new Stop(0.6, baseColor),
            new Stop(1.0, baseColor.darker())
        );

        gc.setFill(studioLighting);
        if (cornerRadius > 0) {
            gc.fillRoundRect(x, y, w, h, cornerRadius * 2.0, cornerRadius * 2.0);
        } else {
            gc.fillRect(x, y, w, h);
        }

        // 2. Ruído orgânico procedural de textura de couro
        Random random = new Random((long) (x * 31 + y * 17));
        int numPores = Math.min(2500, (int) ((w * h) / 15.0));

        for (int i = 0; i < numPores; i++) {
            double px = x + random.nextDouble() * w;
            double py = y + random.nextDouble() * h;
            double poreSize = 0.8 + random.nextDouble() * 0.9;

            double factor = 0.80 + random.nextDouble() * 0.4;
            Color poreColor = Color.color(
                Math.clamp(baseColor.getRed() * factor, 0.0, 1.0),
                Math.clamp(baseColor.getGreen() * factor, 0.0, 1.0),
                Math.clamp(baseColor.getBlue() * factor, 0.0, 1.0),
                0.22
            );

            gc.setFill(poreColor);
            gc.fillOval(px, py, poreSize, poreSize);
        }
    }

    /**
     * Aplica sombra projetada suave (soft drop shadow) sob a peça de couro.
     */
    public static void applyDropShadow(GraphicsContext gc, double x, double y, double w, double h, double cornerRadius, double offset) {
        // Sombra suave multicamadas
        for (int i = 3; i >= 1; i--) {
            double curOffset = offset * (i / 3.0);
            gc.setFill(Color.web("#000000", 0.12 * i));
            if (cornerRadius > 0) {
                gc.fillRoundRect(x + curOffset, y + curOffset, w, h, cornerRadius * 2.0, cornerRadius * 2.0);
            } else {
                gc.fillRect(x + curOffset, y + curOffset, w, h);
            }
        }
    }
}
