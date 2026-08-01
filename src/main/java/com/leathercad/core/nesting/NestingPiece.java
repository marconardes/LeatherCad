package com.leathercad.core.nesting;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;

import java.util.ArrayList;
import java.util.List;

public class NestingPiece {
    private final String id;
    private final String layerId;
    private final String materialName;
    private final double widthMm;
    private final double heightMm;
    private Point2D origMinPoint;
    private double placedX;
    private double placedY;
    private double rotationAngle; // 0 ou 90 graus
    private boolean isPlaced;
    private final List<String> childElementIds = new ArrayList<>();

    public NestingPiece(String id, String layerId, String materialName, double widthMm, double heightMm) {
        this.id = id;
        this.layerId = layerId;
        this.materialName = materialName;
        this.widthMm = widthMm;
        this.heightMm = heightMm;
        this.isPlaced = false;
        this.rotationAngle = 0.0;
        this.origMinPoint = new Point2D(0, 0);
    }

    public String id() { return id; }
    public String layerId() { return layerId; }
    public String materialName() { return materialName; }
    public double widthMm() { return widthMm; }
    public double heightMm() { return heightMm; }
    public Point2D origMinPoint() { return origMinPoint; }
    public void setOrigMinPoint(Point2D origMinPoint) { this.origMinPoint = origMinPoint; }
    public double placedX() { return placedX; }
    public double placedY() { return placedY; }
    public double rotationAngle() { return rotationAngle; }
    public boolean isPlaced() { return isPlaced; }
    public List<String> childElementIds() { return childElementIds; }

    public double currentWidth() {
        return (rotationAngle == 90.0 || rotationAngle == 270.0) ? heightMm : widthMm;
    }

    public double currentHeight() {
        return (rotationAngle == 90.0 || rotationAngle == 270.0) ? widthMm : heightMm;
    }

    public double areaCm2() {
        return (widthMm * heightMm) / 100.0;
    }

    public void place(double x, double y, double rotationAngle) {
        this.placedX = x;
        this.placedY = y;
        this.rotationAngle = rotationAngle;
        this.isPlaced = true;
    }

    public Rect2D boundingBox() {
        return new Rect2D(placedX, placedY, currentWidth(), currentHeight());
    }
}
