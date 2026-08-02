package com.leathercad.core.leather;

import com.leathercad.core.geometry.LineSegment;
import com.leathercad.core.geometry.Point2D;

import java.util.ArrayList;
import java.util.List;

public class StitchEngine {

    /**
     * Calcula os pontos exatos de furação de costura ao longo de um segmento de linha.
     * Implementa o Corner Auto-Alignment (furos travados em start e end) e
     * redistribuição simétrica com recálculo do pitch nominal.
     */
    public static List<Point2D> calculateStitchHoles(LineSegment line, StitchConfig config) {
        List<Point2D> holes = new ArrayList<>();
        double totalLength = line.length();

        if (totalLength <= 0.001) {
            holes.add(line.start());
            return holes;
        }

        if (totalLength < config.pitchMm() / 2.0) {
            holes.add(line.start());
            holes.add(line.end());
            return holes;
        }

        int numSteps = (int) Math.round(totalLength / config.pitchMm());
        if (numSteps < 1) numSteps = 1;

        for (int i = 0; i <= numSteps; i++) {
            double t = (double) i / numSteps;
            holes.add(line.pointAt(t));
        }

        return holes;
    }

    /**
     * Calcula furos para uma polilinha/contorno fechado garantindo o compartimento de
     * furos nos vértices (esquinas) sem duplicatas.
     */
    public static List<Point2D> calculateStitchHolesForPolyline(List<Point2D> vertices, boolean isClosed, StitchConfig config) {
        List<Point2D> allHoles = new ArrayList<>();
        if (vertices == null || vertices.size() < 2) return allHoles;

        int numSegments = isClosed ? vertices.size() : vertices.size() - 1;

        for (int i = 0; i < numSegments; i++) {
            Point2D p1 = vertices.get(i);
            Point2D p2 = vertices.get((i + 1) % vertices.size());
            LineSegment seg = new LineSegment(p1, p2);

            List<Point2D> segHoles = calculateStitchHoles(seg, config);
            if (segHoles.isEmpty()) continue;

            // Se não for a primeira aresta, remove o primeiro furo para não duplicar o vértice compartilhado
            if (!allHoles.isEmpty()) {
                segHoles.remove(0);
            }
            allHoles.addAll(segHoles);
        }

        // Para polilinhas fechadas, remover duplicata final caso coincida com o ponto inicial
        if (isClosed && allHoles.size() > 1) {
            Point2D first = allHoles.get(0);
            Point2D last = allHoles.get(allHoles.size() - 1);
            if (first.distanceTo(last) < 0.01) {
                allHoles.remove(allHoles.size() - 1);
            }
        }

        return allHoles;
    }

    /**
     * Calcula os segmentos de fenda inclinada para o modo FRENCH_SLANT.
     * O ângulo da fenda (+45° ou -45°) é relativo ao vetor tangente da linha de costura.
     */
    public static List<LineSegment> calculateSlantSlots(LineSegment line, StitchConfig config) {
        List<Point2D> holes = calculateStitchHoles(line, config);
        return calculateSlantSlotsForHoles(holes, line.start(), line.end(), config);
    }

    /**
     * Calcula os segmentos de fenda inclinada para uma lista de furos com base no vetor inicial e final da aresta.
     */
    public static List<LineSegment> calculateSlantSlotsForHoles(List<Point2D> holes, Point2D segStart, Point2D segEnd, StitchConfig config) {
        List<LineSegment> slots = new ArrayList<>();
        double dx = segEnd.x() - segStart.x();
        double dy = segEnd.y() - segStart.y();
        double tangentAngleRad = Math.atan2(dy, dx);
        double slantAngleRad = Math.toRadians(config.angleDegrees());
        double totalAngleRad = tangentAngleRad + slantAngleRad;

        double halfSlot = config.slotLengthMm() / 2.0;
        double offsetX = Math.cos(totalAngleRad) * halfSlot;
        double offsetY = Math.sin(totalAngleRad) * halfSlot;

        for (Point2D hole : holes) {
            Point2D slotStart = new Point2D(hole.x() - offsetX, hole.y() - offsetY);
            Point2D slotEnd = new Point2D(hole.x() + offsetX, hole.y() + offsetY);
            slots.add(new LineSegment(slotStart, slotEnd));
        }

        return slots;
    }
}
