package com.leathercad.core.production;

import com.leathercad.core.ai.AIDesignGenerator;
import com.leathercad.core.model.Document;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class AssemblyManualTest {

    @Test
    public void testAssemblyManualGenerationForBifoldWallet() {
        Document doc = new Document();
        AIDesignGenerator.generateDesign(doc, "Carteira Bifold 6 cartões", "http://localhost:11434", "gemma4:e2b", false);

        AssemblyManual manual = AssemblyManualEngine.generateManual(doc);

        assertNotNull(manual);
        assertNotNull(manual.projectTitle());
        assertTrue(manual.totalPiecesCount() > 0);
        assertTrue(manual.totalStitchHolesCount() > 0);
        assertTrue(manual.totalStitchLengthMeters() > 0.0);
        assertEquals(8, manual.steps().size());

        assertFalse(manual.masterToolsList().isEmpty());
        assertFalse(manual.masterSuppliesList().isEmpty());

        // Validação de ferramentas de marcenaria/artesanato em couro obrigatórias
        assertTrue(manual.masterToolsList().stream().anyMatch(t -> t.contains("Garfo") || t.contains("3.85mm")));
        assertTrue(manual.masterToolsList().stream().anyMatch(t -> t.contains("Agulha") || t.contains("Stitching Pony")));
    }

    @Test
    public void testAssemblyManualTextReportExport() throws IOException {
        Document doc = new Document();
        AIDesignGenerator.generateDesign(doc, "Capa de Passaporte", "http://localhost:11434", "gemma4:e2b", false);

        AssemblyManual manual = AssemblyManualEngine.generateManual(doc);
        File tempFile = File.createTempFile("manual_test", ".txt");
        tempFile.deleteOnExit();

        com.leathercad.core.export.AssemblyManualPDFExporter.exportManualToTextReport(manual, tempFile);

        assertTrue(tempFile.exists());
        assertTrue(tempFile.length() > 200);
    }
}
