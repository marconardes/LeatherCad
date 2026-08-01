package com.leathercad.core.leather;

import com.leathercad.core.geometry.LineSegment;
import com.leathercad.core.geometry.Point2D;

import java.util.ArrayList;
import java.util.List;

public class StitchEngine {

    /**
     * Calcula os pontos exatos de furação de costura ao longo de um segmento de linha.
     * Garante travamento de furos nos pontos inicial e final.
     */
    public static List<Point2D> calculateStitchHoles(LineSegment line, StitchConfig config) {
        List<Point2D> holes = new ArrayList<>();
        double totalLength = line.length();

        if (totalLength < config.pitchMm() / 2.0) {
            holes.add(line.start());
            holes.add(line.end());
            return holes;
        }

        int numSteps = (int) Math.round(totalLength / config.pitchMm());
        if (numSteps < 1) numSteps = 1;

        double actualStep = totalLength / numSteps;

        for (int i = 0; i <= numSteps; i++) {
            double t = (double) i / numSteps;
            holes.add(line.pointAt(t));
        }

        return holes;
    }
}
