package com.leathercad.core.components;

import com.leathercad.core.geometry.Circle2D;
import com.leathercad.core.geometry.LineSegment;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.model.CADElement;
import com.leathercad.core.model.CircleElement;
import com.leathercad.core.model.LineElement;
import com.leathercad.core.model.RectElement;

import java.util.ArrayList;
import java.util.List;

public class HardwareComponent {

    public static List<CADElement> createMagneticSnap(String layerId, Point2D center, double diameterMm) {
        List<CADElement> elements = new ArrayList<>();
        double radius = diameterMm / 2.0;

        // Círculo Principal do Fecho (14mm ou 18mm)
        elements.add(new CircleElement(layerId, new Circle2D(center, radius)));

        // Círculo Interno do Imã
        elements.add(new CircleElement(layerId, new Circle2D(center, radius * 0.5)));

        // Furos das abas dobráveis de metal (slots laterais)
        elements.add(new LineElement(layerId, new LineSegment(new Point2D(center.x() - radius - 2, center.y()), new Point2D(center.x() - radius + 2, center.y()))));
        elements.add(new LineElement(layerId, new LineSegment(new Point2D(center.x() + radius - 2, center.y()), new Point2D(center.x() + radius + 2, center.y()))));

        return elements;
    }

    public static List<CADElement> createPressStud(String layerId, Point2D center, double diameterMm) {
        List<CADElement> elements = new ArrayList<>();
        double radius = diameterMm / 2.0;

        elements.add(new CircleElement(layerId, new Circle2D(center, radius)));
        elements.add(new CircleElement(layerId, new Circle2D(center, radius * 0.4)));

        return elements;
    }

    public static List<CADElement> createZipper(String layerId, Point2D origin, double lengthMm, double tapeWidthMm) {
        List<CADElement> elements = new ArrayList<>();

        // Fita do Zíper
        elements.add(new RectElement(layerId, new Rect2D(origin, lengthMm, tapeWidthMm)));

        // Linha Central dos Dentes
        double midY = origin.y() + tapeWidthMm / 2.0;
        elements.add(new LineElement(layerId, new LineSegment(new Point2D(origin.x(), midY), new Point2D(origin.x() + lengthMm, midY))));

        return elements;
    }

    public static List<CADElement> createElasticBand(String layerId, Point2D origin, double widthMm, double lengthMm) {
        List<CADElement> elements = new ArrayList<>();
        elements.add(new RectElement(layerId, new Rect2D(origin, lengthMm, widthMm)));
        return elements;
    }
}
