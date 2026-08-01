package com.leathercad.core.geometry;

/**
 * Círculo 2D com centro e raio em mm.
 */
public record Circle2D(Point2D center, double radius) {
    public double perimeter() {
        return 2 * Math.PI * radius;
    }

    public double area() {
        return Math.PI * radius * radius;
    }

    public boolean contains(Point2D p) {
        return center.distanceTo(p) <= radius;
    }
}
