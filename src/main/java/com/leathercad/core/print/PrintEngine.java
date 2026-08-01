package com.leathercad.core.print;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.model.CADElement;
import com.leathercad.core.model.Document;

import java.util.ArrayList;
import java.util.List;

public class PrintEngine {

    public static List<PrintTile> calculateTiles(Document doc, PrintPaperSize paperSize, double marginMm, double overlapMm) {
        List<PrintTile> tiles = new ArrayList<>();
        if (doc.getElements().isEmpty()) return tiles;

        // 1. Calcula o Bounding Box total do documento em milímetros reais
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

        for (CADElement elem : doc.getElements()) {
            Rect2D bbox = elem.boundingBox();
            minX = Math.min(minX, bbox.minPoint().x());
            minY = Math.min(minY, bbox.minPoint().y());
            maxX = Math.max(maxX, bbox.minPoint().x() + bbox.width());
            maxY = Math.max(maxY, bbox.minPoint().y() + bbox.height());
        }

        // Adiciona margem de segurança de 10mm ao redor das peças
        minX -= 10.0;
        minY -= 10.0;
        maxX += 10.0;
        maxY += 10.0;

        double docW = Math.max(1.0, maxX - minX);
        double docH = Math.max(1.0, maxY - minY);

        // 2. Tamanho útil imprimível por folha em mm
        double stepW = paperSize.printableWidth(marginMm, overlapMm);
        double stepH = paperSize.printableHeight(marginMm, overlapMm);

        int totalCols = (int) Math.ceil(docW / stepW);
        int totalRows = (int) Math.ceil(docH / stepH);
        int totalPages = totalCols * totalRows;

        int pageIndex = 1;
        for (int r = 0; r < totalRows; r++) {
            for (int c = 0; c < totalCols; c++) {
                double tileMinX = minX + c * stepW;
                double tileMinY = minY + r * stepH;

                Rect2D tileBounds = new Rect2D(new Point2D(tileMinX, tileMinY), stepW + overlapMm, stepH + overlapMm);

                tiles.add(new PrintTile(
                    pageIndex++,
                    totalPages,
                    c,
                    r,
                    totalCols,
                    totalRows,
                    tileBounds,
                    paperSize,
                    marginMm,
                    overlapMm
                ));
            }
        }

        return tiles;
    }
}
