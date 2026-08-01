package com.leathercad.core.simulation;

import com.leathercad.core.geometry.Point2D;

public class FoldSimulator {

    /**
     * Projeta um ponto 2D do molde no espaço 3D dobrado em torno do eixo do vinco (foldLineX),
     * isolando apenas os elementos que pertencem à peça dobrável (X <= maxFoldableX).
     */
    public static Point2D projectFold3D(
        Point2D original,
        double foldLineX,
        double maxFoldableX,
        double foldAngleDeg,
        double docCenterX,
        double docCenterY,
        double scale,
        double originX,
        double originY
    ) {
        double dx = original.x() - foldLineX;
        double closureRad = Math.toRadians(180.0 - foldAngleDeg);

        double x3d;
        double y3d = original.y();
        double z3d;

        if (dx <= 0 || original.x() > maxFoldableX) {
            // Aba Esquerda OU Peças de corte separadas fora da bifold: Permanece fixa no plano da mesa (Z = 0)
            x3d = original.x();
            z3d = 0.0;
        } else {
            // Aba Direita da Bifold: Rotaciona 3D em torno do eixo do vinco central (foldLineX)
            x3d = foldLineX + dx * Math.cos(closureRad);
            z3d = dx * Math.sin(closureRad);
        }

        // Normalização em relação ao centro do documento
        double nx = (x3d - docCenterX) * scale;
        double ny = (y3d - docCenterY) * scale;
        double nz = z3d * scale;

        // Projeção isométrica 3D com inclinação de câmera de estúdio (30° yaw, 25° pitch)
        double yaw = Math.toRadians(30.0);
        double pitch = Math.toRadians(25.0);

        double screenX = originX + (nx * Math.cos(yaw) - ny * Math.sin(yaw));
        double screenY = originY + (nx * Math.sin(yaw) * Math.sin(pitch) 
                                   + ny * Math.cos(yaw) * Math.sin(pitch) 
                                   - nz * Math.cos(pitch));

        return new Point2D(screenX, screenY);
    }

    public static Point2D projectFold3D(
        Point2D original,
        double foldLineX,
        double foldAngleDeg,
        double docCenterX,
        double docCenterY,
        double scale,
        double originX,
        double originY
    ) {
        return projectFold3D(original, foldLineX, foldLineX + 100.0, foldAngleDeg, docCenterX, docCenterY, scale, originX, originY);
    }

    public static Point2D projectIsometric(Point2D original, double foldAngleDeg, double originX, double originY) {
        return projectFold3D(original, originX, foldAngleDeg, originX, originY, 1.0, originX, originY);
    }
}
