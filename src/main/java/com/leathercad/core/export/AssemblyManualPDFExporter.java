package com.leathercad.core.export;

import com.leathercad.core.production.AssemblyManual;
import com.leathercad.core.production.AssemblyStep;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;

public class AssemblyManualPDFExporter {

    public static void exportManualToTextReport(AssemblyManual manual, File targetFile) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(targetFile))) {
            writer.println("==========================================================================");
            writer.println("         LEATHERCAD — MANUAL TÉCNICO DE MONTAGEM & GUIA DE OFICINA        ");
            writer.println("==========================================================================");
            writer.println("Projeto: " + manual.projectTitle());
            writer.println("Data de Emissão: " + LocalDate.now());
            writer.println("Total de Peças de Corte: " + manual.totalPiecesCount() + " unidades");
            writer.println("Furações de Garfo Chisel: " + manual.totalStitchHolesCount() + " furos");
            writer.println("Trajeto Total de Costura: " + String.format("%.2f metros", manual.totalStitchLengthMeters()));
            writer.println("--------------------------------------------------------------------------");
            writer.println();

            writer.println("🛠️ FERRAMENTAS NECESSÁRIAS NA BANCADA:");
            for (String tool : manual.masterToolsList()) {
                writer.println("  [ ] " + tool);
            }
            writer.println();

            writer.println("📦 INSUMOS & MATERIAIS DE FABRICAÇÃO:");
            for (String supply : manual.masterSuppliesList()) {
                writer.println("  [ ] " + supply);
            }
            writer.println();
            writer.println("==========================================================================");
            writer.println("                  SEQUÊNCIA DE FABRICAÇÃO PASSO A PASSO                   ");
            writer.println("==========================================================================");
            writer.println();

            for (AssemblyStep step : manual.steps()) {
                writer.println(String.format("ETAPA %d: [%s] — %s", step.stepNumber(), step.stageName().toUpperCase(), step.title()));
                writer.println("  Descrição: " + step.description());
                writer.println("  Ferramentas da Etapa: " + String.join(", ", step.requiredTools()));
                writer.println("  Insumos da Etapa:     " + String.join(", ", step.requiredSupplies()));
                writer.println("  Peças Alvo:           " + String.join(", ", step.targetElementNames()));
                writer.println("--------------------------------------------------------------------------");
                writer.println();
            }

            writer.println("==========================================================================");
            writer.println("               FIM DO MANUAL DE MONTAGEM — LEATHERCAD CAD               ");
            writer.println("==========================================================================");
        }
    }
}
