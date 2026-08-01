package com.leathercad.core.geometry;

/**
 * Retângulo 2D em milímetros, com suporte a raios individuais por canto (TL, TR, BR, BL).
 */
public record Rect2D(Point2D minPoint, double width, double height, double rTopLeft, double rTopRight, double rBottomRight, double rBottomLeft) {

    public Rect2D(Point2D minPoint, double width, double height, double cornerRadius) {
        this(minPoint, width, height, cornerRadius, cornerRadius, cornerRadius, cornerRadius);
    }

    public Rect2D(double x, double y, double width, double height) {
        this(new Point2D(x, y), width, height, 0.0, 0.0, 0.0, 0.0);
    }

    public Rect2D(double x, double y, double width, double height, double cornerRadius) {
        this(new Point2D(x, y), width, height, cornerRadius, cornerRadius, cornerRadius, cornerRadius);
    }

    public Rect2D(Point2D minPoint, double width, double height) {
        this(minPoint, width, height, 0.0, 0.0, 0.0, 0.0);
    }

    public double cornerRadius() {
        return Math.max(Math.max(rTopLeft, rTopRight), Math.max(rBottomRight, rBottomLeft));
    }

    public Point2D maxPoint() {
        return new Point2D(minPoint.x() + width, minPoint.y() + height);
    }

    public Point2D center() {
        return new Point2D(minPoint.x() + width / 2.0, minPoint.y() + height / 2.0);
    }

    public boolean contains(Point2D p) {
        return p.x() >= minPoint.x() && p.x() <= minPoint.x() + width &&
               p.y() >= minPoint.y() && p.y() <= minPoint.y() + height;
    }
}
