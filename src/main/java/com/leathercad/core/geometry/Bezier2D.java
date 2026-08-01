package com.leathercad.core.geometry;

/**
 * Curva Cúbica de Bézier definida por 4 pontos (Start, Control1, Control2, End).
 */
public record Bezier2D(Point2D start, Point2D control1, Point2D control2, Point2D end) {
    public Point2D evaluate(double t) {
        double u = 1.0 - t;
        double tt = t * t;
        double uu = u * u;
        double uuu = uu * u;
        double ttt = tt * t;

        double x = uuu * start.x() +
                   3 * uu * t * control1.x() +
                   3 * u * tt * control2.x() +
                   ttt * end.x();

        double y = uuu * start.y() +
                   3 * uu * t * control1.y() +
                   3 * u * tt * control2.y() +
                   ttt * end.y();

        return new Point2D(x, y);
    }
}
