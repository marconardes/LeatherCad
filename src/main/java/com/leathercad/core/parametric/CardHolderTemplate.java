package com.leathercad.core.parametric;

import com.leathercad.core.components.CardSlotComponent;
import com.leathercad.core.geometry.LineSegment;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.leather.StitchConfig;
import com.leathercad.core.leather.StitchElement;
import com.leathercad.core.leather.StitchType;
import com.leathercad.core.model.CADAssembler;
import com.leathercad.core.model.CADElement;
import com.leathercad.core.model.DimensionElement;
import com.leathercad.core.model.Document;
import com.leathercad.core.model.RectElement;

import java.util.ArrayList;
import java.util.List;

public class CardHolderTemplate implements ParametricTemplate {

    @Override
    public String getId() {
        return "card_holder";
    }

    @Override
    public String getName() {
        return "Porta-Cartões Minimalista";
    }

    @Override
    public String getDescription() {
        return "Porta-cartões compacto e ultra-fino em couro com blocos frontal/traseiro e furação perimetral em U.";
    }

    @Override
    public VariableTable createDefaultVariables() {
        VariableTable vt = new VariableTable();
        vt.addVariable(new Variable("width", 100.0, "mm", "Largura do porta-cartões"));
        vt.addVariable(new Variable("height", 70.0, "mm", "Altura do porta-cartões"));
        vt.addVariable(new Variable("leather_thickness", 1.2, "mm", "Espessura do couro"));
        vt.addVariable(new Variable("card_slots", 4.0, "qtd", "Quantidade total de cartões"));
        vt.addVariable(new Variable("margin", 3.0, "mm", "Margem de costura"));
        vt.addVariable(new Variable("pitch", 3.85, "mm", "Passo de garfo francês (#7)"));
        vt.recalculateAll();
        return vt;
    }

    @Override
    public void generate(Document doc, VariableTable vars, boolean showDimensions) {
        doc.clearElements();

        String leatherLayerId = doc.getLeatherLayerId();
        String stitchLayerId = doc.getStitchLayerId();
        String annotationLayerId = doc.getAnnotationLayerId();

        double width = vars.getValue("width");
        double baseHeight = vars.getValue("height");
        int slotsCount = (int) Math.max(1, vars.getValue("card_slots"));
        double margin = vars.getValue("margin");
        double pitch = vars.getValue("pitch");

        double slotH = 45.0;
        double stepY = 14.0;
        int slotsPerSide = Math.max(1, (slotsCount + 1) / 2);
        double requiredMinHeight = 5.0 + ((slotsPerSide - 1) * stepY) + slotH + 15.0;
        double height = Math.max(baseHeight, requiredMinHeight);

        StitchConfig stitchCfg = new StitchConfig(margin, pitch, 1.0, 45.0, StitchType.FRENCH_SLANT);

        // 1. Corpo Frontal Principal
        List<CADElement> frontElems = new ArrayList<>();
        Point2D frontOrigin = new Point2D(20, 20);
        Rect2D frontRect = new Rect2D(frontOrigin, width, height, 5.0);
        frontElems.add(new RectElement(leatherLayerId, frontRect));

        // Costura Perimetral em U
        Point2D p1 = new Point2D(frontOrigin.x() + margin, frontOrigin.y() + margin);
        Point2D p2 = new Point2D(frontOrigin.x() + margin, frontOrigin.y() + height - margin);
        Point2D p3 = new Point2D(frontOrigin.x() + width - margin, frontOrigin.y() + height - margin);
        Point2D p4 = new Point2D(frontOrigin.x() + width - margin, frontOrigin.y() + margin);

        frontElems.add(new StitchElement(stitchLayerId, new LineSegment(p1, p2), stitchCfg));
        frontElems.add(new StitchElement(stitchLayerId, new LineSegment(p2, p3), stitchCfg));
        frontElems.add(new StitchElement(stitchLayerId, new LineSegment(p3, p4), stitchCfg));

        doc.addElement(CADAssembler.createAssemblyBlock(leatherLayerId, "Corpo Frontal Principal", frontElems));

        // 2. Porta-Cartões Frontais
        double cardW = (width / 2.0) - 8.0;
        int slotsPlaced = 0;

        for (int i = 0; i < slotsPerSide; i++) {
            if (slotsPlaced < slotsCount) {
                Point2D leftOrigin = new Point2D(frontOrigin.x() + 4.0, frontOrigin.y() + 5.0 + (i * stepY));
                List<CADElement> leftSlot = CardSlotComponent.createCardSlot(leatherLayerId, stitchLayerId, leftOrigin, cardW, slotH);
                leftSlot.forEach(doc::addElement);
                slotsPlaced++;
            }

            if (slotsPlaced < slotsCount) {
                Point2D rightOrigin = new Point2D(frontOrigin.x() + (width / 2.0) + 4.0, frontOrigin.y() + 5.0 + (i * stepY));
                List<CADElement> rightSlot = CardSlotComponent.createCardSlot(leatherLayerId, stitchLayerId, rightOrigin, cardW, slotH);
                rightSlot.forEach(doc::addElement);
                slotsPlaced++;
            }
        }

        // 3. Corpo Traseiro
        List<CADElement> backElems = new ArrayList<>();
        Point2D backOrigin = new Point2D(20 + width + 30, 20);
        Rect2D backRect = new Rect2D(backOrigin, width, height, 5.0);
        backElems.add(new RectElement(leatherLayerId, backRect));

        Point2D b1 = new Point2D(backOrigin.x() + margin, backOrigin.y() + margin);
        Point2D b2 = new Point2D(backOrigin.x() + margin, backOrigin.y() + height - margin);
        Point2D b3 = new Point2D(backOrigin.x() + width - margin, backOrigin.y() + height - margin);
        Point2D b4 = new Point2D(backOrigin.x() + width - margin, backOrigin.y() + margin);

        backElems.add(new StitchElement(stitchLayerId, new LineSegment(b1, b2), stitchCfg));
        backElems.add(new StitchElement(stitchLayerId, new LineSegment(b2, b3), stitchCfg));
        backElems.add(new StitchElement(stitchLayerId, new LineSegment(b3, b4), stitchCfg));

        doc.addElement(CADAssembler.createAssemblyBlock(leatherLayerId, "Corpo Traseiro", backElems));

        // 4. Cotas Técnicas Reativas
        if (showDimensions) {
            doc.addElement(new DimensionElement(
                annotationLayerId,
                new Point2D(frontOrigin.x(), frontOrigin.y() - 10),
                new Point2D(frontOrigin.x() + width, frontOrigin.y() - 10),
                DimensionElement.DimensionType.HORIZONTAL,
                10.0
            ));

            doc.addElement(new DimensionElement(
                annotationLayerId,
                new Point2D(frontOrigin.x() - 10, frontOrigin.y()),
                new Point2D(frontOrigin.x() - 10, frontOrigin.y() + height),
                DimensionElement.DimensionType.VERTICAL,
                10.0
            ));
        }
    }

    public static void generateCardHolder(Document doc, ProjectVariables vars) {
        VariableTable vt = new CardHolderTemplate().createDefaultVariables();
        vt.updateVariableValue("width", vars.widthMm());
        vt.updateVariableValue("height", vars.heightMm());
        vt.updateVariableValue("leather_thickness", vars.leatherThicknessMm());
        vt.updateVariableValue("card_slots", vars.cardSlotsCount());
        vt.updateVariableValue("margin", vars.marginMm());
        vt.updateVariableValue("pitch", vars.stitchPitchMm());

        new CardHolderTemplate().generate(doc, vt, true);
    }
}
