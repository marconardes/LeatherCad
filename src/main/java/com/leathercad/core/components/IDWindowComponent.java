package com.leathercad.core.components;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.model.CADAssembler;
import com.leathercad.core.model.CADElement;
import com.leathercad.core.model.RectElement;

import java.util.ArrayList;
import java.util.List;

public class IDWindowComponent {

    public static List<CADElement> createIDWindow(String layerId, Point2D origin) {
        List<CADElement> elements = new ArrayList<>();

        // Moldura Externa (100mm x 70mm)
        Rect2D outer = new Rect2D(origin, 100.0, 70.0, 5.0);
        elements.add(new RectElement(layerId, outer));

        // Janela Transparente Interna (80mm x 50mm)
        Point2D innerOrigin = new Point2D(origin.x() + 10.0, origin.y() + 10.0);
        Rect2D inner = new Rect2D(innerOrigin, 80.0, 50.0, 3.0);
        elements.add(new RectElement(layerId, inner));

        return List.of(CADAssembler.createAssemblyBlock(layerId, "Janela ID Transparente", elements));
    }
}
