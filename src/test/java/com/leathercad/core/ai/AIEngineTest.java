package com.leathercad.core.ai;

import com.leathercad.core.model.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AIEngineTest {

    @Test
    public void testAIPromptPresets() {
        AIPromptPreset bifold = AIPromptPreset.BIFOLD_6_CARDS;
        assertNotNull(bifold.title());
        assertTrue(bifold.prompt().contains("bifold"));

        AIPromptPreset cardholder = AIPromptPreset.MINIMALIST_CARDHOLDER;
        assertNotNull(cardholder.title());
        assertTrue(cardholder.prompt().contains("porta-cartões"));
    }

    @Test
    public void testAIDesignGenerationBifold() {
        Document doc = new Document();
        String result = AIDesignGenerator.generateDesign(
            doc,
            "Crie uma carteira bifold de 6 cartões com compartimento de notas",
            "http://localhost:11434",
            "gemma4:e2b",
            false
        );

        assertNotNull(result);
        assertTrue(result.contains("sucesso"));
        assertFalse(doc.getElements().isEmpty());
    }

    @Test
    public void testAIDesignGenerationPassport() {
        Document doc = new Document();
        String result = AIDesignGenerator.generateDesign(
            doc,
            "Crie uma capa de passaporte com bolso transparente ID",
            "http://localhost:11434",
            "gemma4:e2b",
            false
        );

        assertNotNull(result);
        assertTrue(result.contains("sucesso"));
        assertFalse(doc.getElements().isEmpty());
    }
}
