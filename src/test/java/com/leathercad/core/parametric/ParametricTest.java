package com.leathercad.core.parametric;

import com.leathercad.core.leather.StitchElement;
import com.leathercad.core.leather.StitchType;
import com.leathercad.core.model.DimensionElement;
import com.leathercad.core.model.Document;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ParametricTest {

    @Test
    public void testFoldAllowanceCalculator() {
        double thickness = 1.4; // 1.4mm leather
        double foldAllowance = LeatherFoldCalculator.calculateFoldAllowance(thickness);

        assertTrue(foldAllowance > 5.0); // π * 1.4 * 1.2 = ~5.27mm
        assertEquals(5.2778, foldAllowance, 1e-2);

        double outerWidth = LeatherFoldCalculator.calculateOuterShellWidth(115.0, thickness);
        assertEquals(235.2778, outerWidth, 1e-2);
    }

    @Test
    public void testParametricRegistryAndBifoldTemplate() {
        ParametricRegistry registry = ParametricRegistry.getInstance();
        List<ParametricTemplate> templates = registry.getAllTemplates();
        assertFalse(templates.isEmpty(), "ParametricRegistry deve conter templates registrados");

        ParametricTemplate bifold = registry.getTemplate("bifold_wallet");
        assertNotNull(bifold, "Template bifold_wallet deve estar registrado");

        Document doc = new Document();
        VariableTable vt = bifold.createDefaultVariables();
        bifold.generate(doc, vt, true);

        assertNotNull(doc.getElements());
        assertTrue(doc.getElements().size() >= 10);

        boolean hasFrenchStitch = doc.getElements().stream()
            .anyMatch(e -> e instanceof StitchElement s && s.config().type() == StitchType.FRENCH_SLANT);
        assertTrue(hasFrenchStitch, "Costura deve utilizar Furação Francesa (FRENCH_SLANT)");

        boolean hasDimensions = doc.getElements().stream()
            .anyMatch(e -> e instanceof DimensionElement);
        assertTrue(hasDimensions, "Cotas Técnicas devem ser geradas na camada de cotas quando ativadas");
    }

    @Test
    public void testCardHolderTemplateWithFormulaEngine() {
        ParametricTemplate cardHolder = ParametricRegistry.getInstance().getTemplate("card_holder");
        assertNotNull(cardHolder);

        Document doc = new Document();
        VariableTable vt = cardHolder.createDefaultVariables();
        vt.updateVariableValue("width", 110.0);
        vt.updateVariableValue("height", 75.0);

        cardHolder.generate(doc, vt, true);

        assertTrue(doc.getElements().size() >= 5);
    }
}
