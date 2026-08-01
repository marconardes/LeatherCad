package com.leathercad.core.parametric;

import com.leathercad.core.model.Document;

import org.junit.jupiter.api.Test;

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
    public void testBifoldWalletTemplateGeneration() {
        Document doc = new Document();
        ProjectVariables vars = ProjectVariables.DEFAULT_BIFOLD; // 6 cards, 1.4mm leather, 115x85mm

        BifoldWalletTemplate.generateBifoldWallet(doc, vars);

        assertNotNull(doc.getElements());
        assertTrue(doc.getElements().size() >= 10); // Outer shell, inner shell, crease, stitches, 6 card slots
    }

    @Test
    public void testCardHolderTemplateGeneration() {
        Document doc = new Document();
        ProjectVariables vars = ProjectVariables.DEFAULT_CARD_HOLDER;

        CardHolderTemplate.generateCardHolder(doc, vars);

        assertNotNull(doc.getElements());
        assertTrue(doc.getElements().size() >= 5);
    }
}
