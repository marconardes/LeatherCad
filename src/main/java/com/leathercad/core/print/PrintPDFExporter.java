package com.leathercad.core.print;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.model.Document;

import java.io.File;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;

public class PrintPDFExporter {

    public static void exportMultiPageReport(Document doc, PrintPaperSize paperSize, double marginMm, double overlapMm, File file) throws Exception {
        List<PrintTile> tiles = PrintEngine.calculateTiles(doc, paperSize, marginMm, overlapMm);

        try (PrintWriter out = new PrintWriter(file)) {
            out.println("%PDF-1.4 - LeatherCAD 1:1 Scale Print & Tiling Report");
            out.println("=========================================================");
            out.printf(Locale.US, "DOCUMENTO: %d elementos | FORMADO PAPEL: %s (%.1fx%.1f mm)\n",
                doc.getElements().size(), paperSize.name(), paperSize.currentWidth(), paperSize.currentHeight());
            out.printf(Locale.US, "TOTAL PÁGINAS A4/A3: %d páginas (%d linhas x %d colunas)\n",
                tiles.size(), tiles.isEmpty() ? 0 : tiles.getFirst().totalRows(), tiles.isEmpty() ? 0 : tiles.getFirst().totalCols());
            out.printf(Locale.US, "MARGEM SEGURANÇA: %.1f mm | SOBREPOSIÇÃO: %.1f mm\n", marginMm, overlapMm);
            out.println("=========================================================\n");

            for (PrintTile tile : tiles) {
                out.printf(Locale.US, "--- %s ---\n", tile.formattedLabel());
                out.printf(Locale.US, "Região do Desenho (mm): X=[%.1f a %.1f], Y=[%.1f a %.1f]\n",
                    tile.worldBounds().minPoint().x(), tile.worldBounds().minPoint().x() + tile.worldBounds().width(),
                    tile.worldBounds().minPoint().y(), tile.worldBounds().minPoint().y() + tile.worldBounds().height());

                out.println("Marcas de Corte (Crop Marks):");
                out.printf(Locale.US, "  - Canto Superior Esquerdo: (%.1f, %.1f)\n", tile.worldBounds().minPoint().x(), tile.worldBounds().minPoint().y());
                out.printf(Locale.US, "  - Canto Superior Direito: (%.1f, %.1f)\n", tile.worldBounds().minPoint().x() + tile.worldBounds().width(), tile.worldBounds().minPoint().y());
                out.printf(Locale.US, "  - Canto Inferior Esquerdo: (%.1f, %.1f)\n", tile.worldBounds().minPoint().x(), tile.worldBounds().minPoint().y() + tile.worldBounds().height());
                out.printf(Locale.US, "  - Canto Inferior Direito: (%.1f, %.1f)\n", tile.worldBounds().minPoint().x() + tile.worldBounds().width(), tile.worldBounds().minPoint().y() + tile.worldBounds().height());
                out.println();
            }
        }
    }
}
