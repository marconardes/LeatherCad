package com.leathercad.core.components;

import com.leathercad.core.geometry.Arc2D;
import com.leathercad.core.geometry.LineSegment;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.leather.StitchConfig;
import com.leathercad.core.leather.StitchElement;
import com.leathercad.core.model.ArcElement;
import com.leathercad.core.model.CADAssembler;
import com.leathercad.core.model.CADElement;
import com.leathercad.core.model.RectElement;

import java.util.ArrayList;
import java.util.List;

public class CardSlotComponent {

    public static List<CADElement> createCardSlot(String leatherLayerId, String stitchLayerId, Point2D origin, double width, double height) {
        List<CADElement> elements = new ArrayList<>();

        // 1. Moldura do Porta-Cartão (ex: 95mm x 55mm)
        Rect2D rect = new Rect2D(origin, width, height, 3.0);
        elements.add(new RectElement(leatherLayerId, rect));

        // 2. Recorte de Polegar (Thumb Cut) no centro da borda superior
        Point2D centerTop = new Point2D(origin.x() + width / 2.0, origin.y());
        Arc2D thumbCut = new Arc2D(centerTop, 12.0, 0, 180);
        elements.add(new ArcElement(leatherLayerId, thumbCut));

        // 3. Linha de Costura Inferior (3.85mm da borda inferior) na camada de Costura
        Point2D stitchStart = new Point2D(origin.x() + 3.85, origin.y() + height - 3.85);
        Point2D stitchEnd = new Point2D(origin.x() + width - 3.85, origin.y() + height - 3.85);
        StitchElement bottomStitch = new StitchElement(stitchLayerId, new LineSegment(stitchStart, stitchEnd), StitchConfig.DEFAULT_FRENCH);
        elements.add(bottomStitch);

        // Une todos os sub-elementos em um bloco atômico (CADGroup)
        return List.of(CADAssembler.createAssemblyBlock(leatherLayerId, "Porta-Cartão (" + (int)width + "x" + (int)height + "mm)", elements));
    }

    public static List<CADElement> createCardSlot(String layerId, Point2D origin, double width, double height) {
        return createCardSlot(layerId, layerId, origin, width, height);
    }
}
