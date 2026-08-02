package com.leathercad.core.parametric;

import com.leathercad.core.components.CardSlotComponent;
import com.leathercad.core.geometry.LineSegment;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.leather.CreaseElement;
import com.leathercad.core.leather.StitchConfig;
import com.leathercad.core.leather.StitchElement;
import com.leathercad.core.leather.StitchType;
import com.leathercad.core.model.CADElement;
import com.leathercad.core.model.DimensionElement;
import com.leathercad.core.model.Document;
import com.leathercad.core.model.RectElement;

import java.util.List;

public class BifoldWalletTemplate implements ParametricTemplate {

    @Override
    public String getId() {
        return "bifold_wallet";
    }

    @Override
    public String getName() {
        return "Carteira Bifold (2 Abas)";
    }

    @Override
    public String getDescription() {
        return "Carteira clássica de couro bifold com aba dupla, porta-cartões simétricos e compensação de dobra calculada.";
    }

    @Override
    public VariableTable createDefaultVariables() {
        VariableTable vt = new VariableTable();
        vt.addVariable(new Variable("width", 105.0, "mm", "Largura da carteira fechada"));
        vt.addVariable(new Variable("height", 85.0, "mm", "Altura da carteira"));
        vt.addVariable(new Variable("leather_thickness", 1.4, "mm", "Espessura do couro"));
        vt.addVariable(new Variable("card_slots", 6.0, "qtd", "Quantidade total de porta-cartões"));
        vt.addVariable(new Variable("margin", 3.85, "mm", "Margem de costura"));
        vt.addVariable(new Variable("pitch", 3.85, "mm", "Passo de garfo francês (#7)"));
        vt.addVariable(new Variable("fold_allowance", "PI * leather_thickness * 1.2", 0.0, "mm", "Folga técnica de dobra"));
        vt.addVariable(new Variable("outer_width", "(width * 2) + fold_allowance", 0.0, "mm", "Largura total aberta do corpo externo"));
        vt.recalculateAll();
        return vt;
    }

    @Override
    public void generate(Document doc, VariableTable vars, boolean showDimensions) {
        doc.clearElements();

        String leatherLayerId = doc.getLeatherLayerId();
        String stitchLayerId = doc.getStitchLayerId();
        String creaseLayerId = doc.getCreaseLayerId();
        String annotationLayerId = doc.getAnnotationLayerId();

        double width = vars.getValue("width");
        double baseHeight = vars.getValue("height");
        double leatherThickness = vars.getValue("leather_thickness");
        int cardSlotsCount = (int) Math.max(2, vars.getValue("card_slots"));
        double margin = vars.getValue("margin");
        double pitch = vars.getValue("pitch");

        int cardsPerSide = (int) Math.ceil(cardSlotsCount / 2.0);
        double slotH = 45.0;
        double stepY = 14.0;
        double requiredMinHeight = (5.0 + ((cardsPerSide - 1) * stepY) + slotH + 15.0);
        double height = Math.max(baseHeight, requiredMinHeight);

        double foldAllowance = LeatherFoldCalculator.calculateFoldAllowance(leatherThickness);
        double outerWidth = (width * 2.0) + foldAllowance;

        // 1. CORPO EXTERNO (Outer Shell)
        Point2D outerOrigin = new Point2D(20, 20);
        Rect2D outerRect = new Rect2D(outerOrigin, outerWidth, height, 5.0);
        doc.addElement(new RectElement(leatherLayerId, outerRect));

        // Vinco Central de Dobra
        double centerX = outerOrigin.x() + outerWidth / 2.0;
        doc.addElement(new CreaseElement(creaseLayerId, new LineSegment(new Point2D(centerX, outerOrigin.y()), new Point2D(centerX, outerOrigin.y() + height)), 0.0));

        // Costura Perimetral U / Retângulo com Furação Francesa (French Slant 45°, #7 - 3.85mm)
        Point2D m1 = new Point2D(outerOrigin.x() + margin, outerOrigin.y() + margin);
        Point2D m2 = new Point2D(outerOrigin.x() + outerWidth - margin, outerOrigin.y() + margin);
        Point2D m3 = new Point2D(outerOrigin.x() + outerWidth - margin, outerOrigin.y() + height - margin);
        Point2D m4 = new Point2D(outerOrigin.x() + margin, outerOrigin.y() + height - margin);

        StitchConfig stitchCfg = new StitchConfig(margin, pitch, 1.0, 45.0, StitchType.FRENCH_SLANT);
        doc.addElement(new StitchElement(stitchLayerId, new LineSegment(m1, m2), stitchCfg));
        doc.addElement(new StitchElement(stitchLayerId, new LineSegment(m2, m3), stitchCfg));
        doc.addElement(new StitchElement(stitchLayerId, new LineSegment(m3, m4), stitchCfg));
        doc.addElement(new StitchElement(stitchLayerId, new LineSegment(m4, m1), stitchCfg));

        // 2. CORPO INTERNO (Inner Shell)
        double innerWidth = width * 2.0;
        double innerHeight = height - 4.0;
        Point2D innerOrigin = new Point2D(20, 20 + height + 30);
        Rect2D innerRect = new Rect2D(innerOrigin, innerWidth, innerHeight, 3.0);
        doc.addElement(new RectElement(leatherLayerId, innerRect));

        // 3. PORTA-CARTÕES (Lado Esquerdo e Lado Direito)
        double slotW = width - 10.0;
        for (int i = 0; i < cardsPerSide; i++) {
            Point2D leftOrigin = new Point2D(innerOrigin.x() + 5.0, innerOrigin.y() + 5.0 + (i * stepY));
            List<CADElement> leftSlots = CardSlotComponent.createCardSlot(leatherLayerId, stitchLayerId, leftOrigin, slotW, slotH);
            leftSlots.forEach(doc::addElement);

            Point2D rightOrigin = new Point2D(innerOrigin.x() + width + 5.0, innerOrigin.y() + 5.0 + (i * stepY));
            List<CADElement> rightSlots = CardSlotComponent.createCardSlot(leatherLayerId, stitchLayerId, rightOrigin, slotW, slotH);
            rightSlots.forEach(doc::addElement);
        }

        // 4. COTAS TÉCNICAS REATIVAS (DimensionElement)
        if (showDimensions) {
            // Cota Largura Total Aberta
            doc.addElement(new DimensionElement(
                annotationLayerId,
                new Point2D(outerOrigin.x(), outerOrigin.y() - 10),
                new Point2D(outerOrigin.x() + outerWidth, outerOrigin.y() - 10),
                DimensionElement.DimensionType.HORIZONTAL,
                10.0
            ));

            // Cota Altura
            doc.addElement(new DimensionElement(
                annotationLayerId,
                new Point2D(outerOrigin.x() - 10, outerOrigin.y()),
                new Point2D(outerOrigin.x() - 10, outerOrigin.y() + height),
                DimensionElement.DimensionType.VERTICAL,
                10.0
            ));

            // Cota Margem de Costura
            doc.addElement(new DimensionElement(
                annotationLayerId,
                outerOrigin,
                m1,
                DimensionElement.DimensionType.LINEAR,
                5.0
            ));
        }
    }

    public static void generateBifoldWallet(Document doc, ProjectVariables vars) {
        VariableTable vt = new BifoldWalletTemplate().createDefaultVariables();
        vt.updateVariableValue("width", vars.widthMm());
        vt.updateVariableValue("height", vars.heightMm());
        vt.updateVariableValue("leather_thickness", vars.leatherThicknessMm());
        vt.updateVariableValue("card_slots", vars.cardSlotsCount());
        vt.updateVariableValue("margin", vars.marginMm());
        vt.updateVariableValue("pitch", vars.stitchPitchMm());

        new BifoldWalletTemplate().generate(doc, vt, true);
    }
}
