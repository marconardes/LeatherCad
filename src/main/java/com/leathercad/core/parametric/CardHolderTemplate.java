package com.leathercad.core.parametric;

import com.leathercad.core.components.CardSlotComponent;
import com.leathercad.core.geometry.LineSegment;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.leather.StitchConfig;
import com.leathercad.core.leather.StitchElement;
import com.leathercad.core.model.CADAssembler;
import com.leathercad.core.model.CADElement;
import com.leathercad.core.model.Document;
import com.leathercad.core.model.RectElement;

import java.util.ArrayList;
import java.util.List;

public class CardHolderTemplate {

    public static void generateCardHolder(Document doc, ProjectVariables vars) {
        doc.clear();
        String leatherLayerId = doc.getLeatherLayerId();
        String stitchLayerId = doc.getStitchLayerId();

        int slots = Math.max(1, vars.cardSlotsCount());
        double slotH = 45.0;
        double stepY = 14.0;

        int slotsPerSide = Math.max(1, (slots + 1) / 2);

        // Recálculo dinâmico da altura necessária
        double requiredMinHeight = 5.0 + ((slotsPerSide - 1) * stepY) + slotH + 15.0;
        double height = Math.max(vars.heightMm(), requiredMinHeight);
        double width = vars.widthMm();

        // 1. Bloco Atômico do Corpo Principal Frontal
        List<CADElement> frontElems = new ArrayList<>();
        Point2D frontOrigin = new Point2D(20, 20);
        Rect2D frontRect = new Rect2D(frontOrigin, width, height, 5.0);
        frontElems.add(new RectElement(leatherLayerId, frontRect));

        // Costura Perimetral U (Esquerda, Baixo, Direita) na camada de Costura
        Point2D p1 = new Point2D(frontOrigin.x() + vars.marginMm(), frontOrigin.y() + vars.marginMm());
        Point2D p2 = new Point2D(frontOrigin.x() + vars.marginMm(), frontOrigin.y() + height - vars.marginMm());
        Point2D p3 = new Point2D(frontOrigin.x() + width - vars.marginMm(), frontOrigin.y() + height - vars.marginMm());
        Point2D p4 = new Point2D(frontOrigin.x() + width - vars.marginMm(), frontOrigin.y() + vars.marginMm());

        StitchConfig stitchCfg = new StitchConfig(vars.marginMm(), vars.stitchPitchMm(), 1.0, 45.0, com.leathercad.core.leather.StitchType.FRENCH);
        frontElems.add(new StitchElement(stitchLayerId, new LineSegment(p1, p2), stitchCfg));
        frontElems.add(new StitchElement(stitchLayerId, new LineSegment(p2, p3), stitchCfg));
        frontElems.add(new StitchElement(stitchLayerId, new LineSegment(p3, p4), stitchCfg));

        doc.addElement(CADAssembler.createAssemblyBlock(leatherLayerId, "Corpo Frontal Principal", frontElems));

        // 2. Porta-Cartões
        double cardW = (width / 2.0) - 8.0;
        int slotsPlaced = 0;

        for (int i = 0; i < slotsPerSide; i++) {
            // Lado Esquerdo
            if (slotsPlaced < slots) {
                Point2D leftSlotOrigin = new Point2D(frontOrigin.x() + 4.0, frontOrigin.y() + 5.0 + (i * stepY));
                List<CADElement> leftSlot = CardSlotComponent.createCardSlot(leatherLayerId, stitchLayerId, leftSlotOrigin, cardW, slotH);
                for (CADElement e : leftSlot) doc.addElement(e);
                slotsPlaced++;
            }

            // Lado Direito
            if (slotsPlaced < slots) {
                Point2D rightSlotOrigin = new Point2D(frontOrigin.x() + (width / 2.0) + 4.0, frontOrigin.y() + 5.0 + (i * stepY));
                List<CADElement> rightSlot = CardSlotComponent.createCardSlot(leatherLayerId, stitchLayerId, rightSlotOrigin, cardW, slotH);
                for (CADElement e : rightSlot) doc.addElement(e);
                slotsPlaced++;
            }
        }

        // 3. Bloco Atômico do Corpo Traseiro
        List<CADElement> backElems = new ArrayList<>();
        Point2D backOrigin = new Point2D(20 + width + 30, 20);
        Rect2D backRect = new Rect2D(backOrigin, width, height, 5.0);
        backElems.add(new RectElement(leatherLayerId, backRect));

        Point2D b1 = new Point2D(backOrigin.x() + vars.marginMm(), backOrigin.y() + vars.marginMm());
        Point2D b2 = new Point2D(backOrigin.x() + vars.marginMm(), backOrigin.y() + height - vars.marginMm());
        Point2D b3 = new Point2D(backOrigin.x() + width - vars.marginMm(), backOrigin.y() + height - vars.marginMm());
        Point2D b4 = new Point2D(backOrigin.x() + width - vars.marginMm(), backOrigin.y() + vars.marginMm());

        backElems.add(new StitchElement(stitchLayerId, new LineSegment(b1, b2), stitchCfg));
        backElems.add(new StitchElement(stitchLayerId, new LineSegment(b2, b3), stitchCfg));
        backElems.add(new StitchElement(stitchLayerId, new LineSegment(b3, b4), stitchCfg));

        doc.addElement(CADAssembler.createAssemblyBlock(leatherLayerId, "Corpo Traseiro", backElems));
    }
}
