package com.leathercad.ui.viewport;

import com.leathercad.core.geometry.Point2D;

public class CameraTransform {
    private double zoom = 3.0; // 1mm world = 3px screen (default zoom)
    private double panX = 150.0; // Screen X offset in px
    private double panY = 150.0; // Screen Y offset in px

    public double getZoom() { return zoom; }
    public double getPanX() { return panX; }
    public double getPanY() { return panY; }

    public void setPan(double panX, double panY) {
        this.panX = panX;
        this.panY = panY;
    }

    public void panBy(double dxPx, double dyPx) {
        this.panX += dxPx;
        this.panY += dyPx;
    }

    public void zoomAt(double screenX, double screenY, double zoomFactor) {
        double oldZoom = this.zoom;
        double newZoom = Math.max(0.05, Math.min(100.0, oldZoom * zoomFactor));
        if (Math.abs(newZoom - oldZoom) < 1e-6) return;

        this.zoom = newZoom;

        double worldX = (screenX - panX) / oldZoom;
        double worldY = (screenY - panY) / oldZoom;

        this.panX = screenX - worldX * this.zoom;
        this.panY = screenY - worldY * this.zoom;
    }

    public Point2D screenToWorld(double screenX, double screenY) {
        double worldX = (screenX - panX) / zoom;
        double worldY = (screenY - panY) / zoom;
        return new Point2D(worldX, worldY);
    }

    public Point2D worldToScreen(Point2D worldPoint) {
        double screenX = worldPoint.x() * zoom + panX;
        double screenY = worldPoint.y() * zoom + panY;
        return new Point2D(screenX, screenY);
    }

    public double worldToScreenLength(double mmLength) {
        return mmLength * zoom;
    }
}
