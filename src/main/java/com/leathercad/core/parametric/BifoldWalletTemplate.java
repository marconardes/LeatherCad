package com.leathercad.core.parametric;

import com.leathercad.core.components.CardSlotComponent;
import com.leathercad.core.geometry.LineSegment;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.leather.CreaseElement;
import com.leathercad.core.leather.StitchConfig;
import com.leathercad.core.leather.StitchElement;
import com.leathercad.core.model.CADElement;
import com.leathercad.core.model.Document;
import com.leathercad.core.model.RectElement;

import java.util.List;

public class BifoldWalletTemplate {

    public static void generateBifoldWallet(Document doc, ProjectVariables vars) {
        doc.clear();
        String leatherLayerId = doc.getLeatherLayerId();
        String stitchLayerId = doc.getStitchLayerId();
        String creaseLayerId = doc.getCreaseLayerId();

        // 1. Recálculo exato da altura mínima requerida para acomodar todos os cartões
        int cardsPerSide = (int) Math.ceil(vars.cardSlotsCount() / 2.0);
        double slotH = 45.0;
        double stepY = 14.0;
        double requiredMinHeight = (5.0 + ((cardsPerSide - 1) * stepY) + slotH + 15.0);
        double height = Math.max(vars.heightMm(), requiredMinHeight);

        double foldAllowance = LeatherFoldCalculator.calculateFoldAllowance(vars.leatherThicknessMm());
        double outerWidth = (vars.widthMm() * 2.0) + foldAllowance;

        // 2. PEÇA 1: CORPO EXTERNO (Outer Shell)
        Point2D outerOrigin = new Point2D(20, 20);
        Rect2D outerRect = new Rect2D(outerOrigin, outerWidth, height, 5.0);
        doc.addElement(new RectElement(leatherLayerId, outerRect));

        // Vinco Central de Dobra no Corpo Externo (Camada de Vincos)
        double centerX = outerOrigin.x() + outerWidth / 2.0;
        doc.addElement(new CreaseElement(creaseLayerId, new LineSegment(new Point2D(centerX, outerOrigin.y()), new Point2D(centerX, outerOrigin.y() + height)), 0.0));

        // Costura Perimetral do Corpo Externo (Camada de Costura)
        Point2D m1 = new Point2D(outerOrigin.x() + vars.marginMm(), outerOrigin.y() + vars.marginMm());
        Point2D m2 = new Point2D(outerOrigin.x() + outerWidth - vars.marginMm(), outerOrigin.y() + vars.marginMm());
        Point2D m3 = new Point2D(outerOrigin.x() + outerWidth - vars.marginMm(), outerOrigin.y() + height - vars.marginMm());
        Point2D m4 = new Point2D(outerOrigin.x() + vars.marginMm(), outerOrigin.y() + height - vars.marginMm());

        StitchConfig stitchCfg = new StitchConfig(vars.marginMm(), vars.stitchPitchMm(), 1.0, 45.0, com.leathercad.core.leather.StitchType.FRENCH);
        doc.addElement(new StitchElement(stitchLayerId, new LineSegment(m1, m2), stitchCfg));
        doc.addElement(new StitchElement(stitchLayerId, new LineSegment(m2, m3), stitchCfg));
        doc.addElement(new StitchElement(stitchLayerId, new LineSegment(m3, m4), stitchCfg));
        doc.addElement(new StitchElement(stitchLayerId, new LineSegment(m4, m1), stitchCfg));

        // 3. PEÇA 2: CORPO INTERNO (Inner Shell) - Posicionado abaixo do corpo externo
        double innerWidth = vars.widthMm() * 2.0;
        double innerHeight = height - 4.0;
        Point2D innerOrigin = new Point2D(20, 20 + height + 30);
        Rect2D innerRect = new Rect2D(innerOrigin, innerWidth, innerHeight, 3.0);
        doc.addElement(new RectElement(leatherLayerId, innerRect));

        // 4. PAINÉIS DE PORTA-CARTÕES (Lado Esquerdo e Lado Direito)
        double slotW = vars.widthMm() - 10.0;

        // Lado Esquerdo
        for (int i = 0; i < cardsPerSide; i++) {
            Point2D slotOrigin = new Point2D(innerOrigin.x() + 5.0, innerOrigin.y() + 5.0 + (i * stepY));
            List<CADElement> slotElems = CardSlotComponent.createCardSlot(leatherLayerId, stitchLayerId, slotOrigin, slotW, slotH);
            for (CADElement e : slotElems) doc.addElement(e);
        }

        // Lado Direito
        double rightSideX = innerOrigin.x() + vars.widthMm() + 5.0;
        for (int i = 0; i < cardsPerSide; i++) {
            Point2D slotOrigin = new Point2D(rightSideX, innerOrigin.y() + 5.0 + (i * stepY));
            List<CADElement> slotElems = CardSlotComponent.createCardSlot(leatherLayerId, stitchLayerId, slotOrigin, slotW, slotH);
            for (CADElement e : slotElems) doc.addElement(e);
        }
    }
}
