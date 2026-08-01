package com.leathercad.core.components;

import com.leathercad.core.geometry.LineSegment;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.leather.CreaseElement;
import com.leathercad.core.leather.StitchConfig;
import com.leathercad.core.leather.StitchElement;
import com.leathercad.core.model.CADAssembler;
import com.leathercad.core.model.CADElement;
import com.leathercad.core.model.RectElement;

import java.util.ArrayList;
import java.util.List;

public class CoinPocketComponent {

    public static List<CADElement> createCoinPocket(String layerId, Point2D origin, double width, double height) {
        List<CADElement> elements = new ArrayList<>();

        // 1. Moldura Principal do Porta-Moedas (ex: 100mm x 80mm)
        Rect2D body = new Rect2D(origin, width, height, 5.0);
        elements.add(new RectElement(layerId, body));

        // 2. Lapela / Aba Superior de Fechamento (ex: 100mm x 35mm)
        Point2D flapOrigin = new Point2D(origin.x(), origin.y() - 35.0);
        Rect2D flap = new Rect2D(flapOrigin, width, 35.0, 8.0);
        elements.add(new RectElement(layerId, flap));

        // 3. Linha de Vinco de Dobra da Lapela
        LineSegment foldLine = new LineSegment(origin, new Point2D(origin.x() + width, origin.y()));
        elements.add(new CreaseElement(layerId, foldLine, 0.0));

        // 4. Costura Lateral e Inferior
        Point2D s1 = new Point2D(origin.x() + 3.85, origin.y() + 3.85);
        Point2D s2 = new Point2D(origin.x() + 3.85, origin.y() + height - 3.85);
        Point2D s3 = new Point2D(origin.x() + width - 3.85, origin.y() + height - 3.85);
        Point2D s4 = new Point2D(origin.x() + width - 3.85, origin.y() + 3.85);

        elements.add(new StitchElement(layerId, new LineSegment(s1, s2), StitchConfig.DEFAULT_FRENCH));
        elements.add(new StitchElement(layerId, new LineSegment(s2, s3), StitchConfig.DEFAULT_FRENCH));
        elements.add(new StitchElement(layerId, new LineSegment(s3, s4), StitchConfig.DEFAULT_FRENCH));

        // 5. Botão de Pressão de Fechamento no centro da lapela
        Point2D studCenter = new Point2D(origin.x() + width / 2.0, origin.y() - 17.5);
        elements.addAll(HardwareComponent.createPressStud(layerId, studCenter, 12.0));

        return List.of(CADAssembler.createAssemblyBlock(layerId, "Porta-Moedas com Lapela", elements));
    }

    public static List<CADElement> createHiddenPocket(String layerId, Point2D origin, double width, double height) {
        List<CADElement> elements = new ArrayList<>();
        Rect2D hidden = new Rect2D(origin, width, height, 0.0);
        elements.add(new RectElement(layerId, hidden));

        // Costura Oculta
        Point2D s1 = new Point2D(origin.x() + 3.85, origin.y() + 3.85);
        Point2D s2 = new Point2D(origin.x() + width - 3.85, origin.y() + 3.85);
        elements.add(new StitchElement(layerId, new LineSegment(s1, s2), StitchConfig.DEFAULT_FRENCH));

        return List.of(CADAssembler.createAssemblyBlock(layerId, "Bolso Oculto", elements));
    }
}
