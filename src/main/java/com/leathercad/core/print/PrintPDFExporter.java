package com.leathercad.core.print;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.leather.CreaseElement;
import com.leathercad.core.leather.StitchElement;
import com.leathercad.core.leather.StitchType;
import com.leathercad.core.model.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PrintPDFExporter {

    private static final double MM_TO_POINTS = 72.0 / 25.4; // 2.83464567 points per mm

    public static void exportMultiPagePDF(Document doc, PrintPaperSize paperSize, double marginMm, double overlapMm, File file) throws IOException {
        exportMultiPagePDF(doc, paperSize, marginMm, overlapMm, null, file);
    }

    public static void exportMultiPagePDF(Document doc, PrintPaperSize paperSize, double marginMm, double overlapMm, Set<String> activeLayerIds, File file) throws IOException {
        List<PrintTile> tiles = PrintEngine.calculateTiles(doc, paperSize, marginMm, overlapMm);
        if (tiles.isEmpty()) {
            // Se documento estiver vazio, cria 1 página com aviso
            tiles = List.of(new PrintTile(1, 1, 0, 0, 1, 1, new Rect2D(0, 0, 100, 100), paperSize, marginMm, overlapMm));
        }

        byte[] pdfBytes = generatePDFBytes(doc, tiles, activeLayerIds);
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(pdfBytes);
        }
    }

    public static void exportMultiPageReport(Document doc, PrintPaperSize paperSize, double marginMm, double overlapMm, File file) throws Exception {
        // Redireciona para gerar o PDF vetorial 1:1 real
        exportMultiPagePDF(doc, paperSize, marginMm, overlapMm, null, file);
    }

    private static byte[] generatePDFBytes(Document doc, List<PrintTile> tiles, Set<String> activeLayerIds) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Long> offsets = new ArrayList<>();

        writeString(out, "%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n");

        int fontObjNum = 3;
        int pagesObjNum = 2;
        int totalPages = tiles.size();

        // 1. Catalog (Obj 1)
        offsets.add((long) out.size());
        writeString(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        // 2. Pages (Obj 2)
        offsets.add((long) out.size());
        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < totalPages; i++) {
            int pageObjId = 4 + (i * 2);
            kids.append(pageObjId).append(" 0 R ");
        }
        writeString(out, String.format(Locale.US, "2 0 obj\n<< /Type /Pages /Kids [ %s] /Count %d >>\nendobj\n", kids.toString(), totalPages));

        // 3. Font Helvetica (Obj 3)
        offsets.add((long) out.size());
        writeString(out, "3 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n");

        // 4. Páginas e seus Streams de Conteúdo
        for (int i = 0; i < totalPages; i++) {
            PrintTile tile = tiles.get(i);
            int pageObjId = 4 + (i * 2);
            int contentObjId = pageObjId + 1;

            double pageW_pt = tile.paperSize().currentWidth() * MM_TO_POINTS;
            double pageH_pt = tile.paperSize().currentHeight() * MM_TO_POINTS;

            // Stream da página
            byte[] contentStream = generatePageContentStream(doc, tile, activeLayerIds);

            // Objeto Página
            offsets.add((long) out.size());
            writeString(out, String.format(Locale.US,
                "%d 0 obj\n<< /Type /Page /Parent %d 0 R /MediaBox [0 0 %.2f %.2f] /Resources << /Font << /F1 %d 0 R >> >> /Contents %d 0 R >>\nendobj\n",
                pageObjId, pagesObjNum, pageW_pt, pageH_pt, fontObjNum, contentObjId));

            // Objeto Conteúdo
            offsets.add((long) out.size());
            writeString(out, String.format(Locale.US, "%d 0 obj\n<< /Length %d >>\nstream\n", contentObjId, contentStream.length));
            out.write(contentStream);
            writeString(out, "\nendstream\nendobj\n");
        }

        // Cross-Reference Table (xref)
        long startXref = out.size();
        int totalObjects = 3 + (totalPages * 2);
        writeString(out, String.format(Locale.US, "xref\n0 %d\n", totalObjects + 1));
        writeString(out, "0000000000 65535 f \n");

        for (Long offset : offsets) {
            writeString(out, String.format(Locale.US, "%010d 00000 n \n", offset));
        }

        // Trailer
        writeString(out, String.format(Locale.US,
            "trailer\n<< /Size %d /Root 1 0 R >>\nstartxref\n%d\n%%%%EOF\n",
            totalObjects + 1, startXref));

        return out.toByteArray();
    }

    private static byte[] generatePageContentStream(Document doc, PrintTile tile, Set<String> activeLayerIds) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(stream, true, StandardCharsets.US_ASCII);

        double sheetWMm = tile.paperSize().currentWidth();
        double sheetHMm = tile.paperSize().currentHeight();
        double sheetWPt = sheetWMm * MM_TO_POINTS;
        double sheetHPt = sheetHMm * MM_TO_POINTS;

        double marginMm = tile.marginMm();
        Rect2D tileBounds = tile.worldBounds();

        // 1. Desenhar Fundo e Borda da Folha (Margem externa)
        pw.println("0.5 w 0.8 0.8 0.8 RG");
        pw.printf(Locale.US, "%.2f %.2f %.2f %.2f re S\n",
            marginMm * MM_TO_POINTS, marginMm * MM_TO_POINTS,
            (sheetWMm - 2 * marginMm) * MM_TO_POINTS, (sheetHMm - 2 * marginMm) * MM_TO_POINTS);

        // 2. QUADRADO DE AFERIÇÃO DE ESCALA DE 50mm x 50mm NO CABEÇALHO DA PÁGINA
        double boxSizeMm = 50.0;
        double boxX_pt = marginMm * MM_TO_POINTS;
        double boxY_pt = (sheetHMm - marginMm - boxSizeMm) * MM_TO_POINTS;
        double boxSize_pt = boxSizeMm * MM_TO_POINTS;

        // Desenhar Quadrado 50x50mm
        pw.println("1.0 w 0.0 0.0 0.0 RG");
        pw.printf(Locale.US, "%.2f %.2f %.2f %.2f re S\n", boxX_pt, boxY_pt, boxSize_pt, boxSize_pt);

        // Hachura/Cruz interna de 50mm para precisão
        pw.printf(Locale.US, "%.2f %.2f m %.2f %.2f l S\n", boxX_pt, boxY_pt + boxSize_pt / 2, boxX_pt + boxSize_pt, boxY_pt + boxSize_pt / 2);
        pw.printf(Locale.US, "%.2f %.2f m %.2f %.2f l S\n", boxX_pt + boxSize_pt / 2, boxY_pt, boxX_pt + boxSize_pt / 2, boxY_pt + boxSize_pt);

        // Rótulo do Quadrado de Aferição
        pw.println("BT /F1 7 Tf 0 0 0 rg");
        pw.printf(Locale.US, "%.2f %.2f Td (AFERICAO DE ESCALA 50mm x 50mm) Tj ET\n", boxX_pt + 2 * MM_TO_POINTS, boxY_pt + boxSize_pt - 4 * MM_TO_POINTS);
        pw.println("BT /F1 6 Tf 0.3 0.3 0.3 rg");
        pw.printf(Locale.US, "%.2f %.2f Td ((CONFERIR COM REGUA SE MEDE EXACTAMENTE 50mm)) Tj ET\n", boxX_pt + 2 * MM_TO_POINTS, boxY_pt + 2 * MM_TO_POINTS);

        // 3. Cabeçalho de Informações da Folha e Instruções de Impressão
        double textX_pt = boxX_pt + boxSize_pt + (5.0 * MM_TO_POINTS);
        double textY_pt = (sheetHMm - marginMm - 8.0) * MM_TO_POINTS;

        pw.println("BT /F1 11 Tf 0 0 0 rg");
        pw.printf(Locale.US, "%.2f %.2f Td (%s) Tj ET\n", textX_pt, textY_pt, escapePdfString(tile.formattedLabel()));

        pw.println("BT /F1 8 Tf 0.2 0.2 0.2 rg");
        pw.printf(Locale.US, "%.2f %.2f Td (LeatherCAD 1:1 Scale Mold Export - Impressao Mosaico / Tiling) Tj ET\n", textX_pt, textY_pt - 12);
        pw.printf(Locale.US, "%.2f %.2f Td (ATENCAO: Imprimir sem redimensionar / 100%% Scale / Actual Size) Tj ET\n", textX_pt, textY_pt - 22);

        // 4. Cruzetas de Alinhamento / Registro nas 4 pontas da área de sobreposição
        double markLenPt = 8.0 * MM_TO_POINTS;
        double cornerMinX_pt = marginMm * MM_TO_POINTS;
        double cornerMinY_pt = marginMm * MM_TO_POINTS;
        double cornerMaxX_pt = (sheetWMm - marginMm) * MM_TO_POINTS;
        double cornerMaxY_pt = (sheetHMm - marginMm - boxSizeMm - 5.0) * MM_TO_POINTS;

        drawRegistrationCross(pw, cornerMinX_pt, cornerMinY_pt, markLenPt);
        drawRegistrationCross(pw, cornerMaxX_pt, cornerMinY_pt, markLenPt);
        drawRegistrationCross(pw, cornerMinX_pt, cornerMaxY_pt, markLenPt);
        drawRegistrationCross(pw, cornerMaxX_pt, cornerMaxY_pt, markLenPt);

        // 5. Desenhar Elementos Geométricos 1:1 do Documento
        // Mapeamento de coordenadas CAD (mm) -> PDF Pt:
        // pdfX = (marginMm + (cadX - tileMinX)) * MM_TO_POINTS
        // pdfY = (sheetHMm - marginMm - boxSizeMm - 5.0 - (cadY - tileMinY)) * MM_TO_POINTS

        double tileMinX = tileBounds.minPoint().x();
        double tileMinY = tileBounds.minPoint().y();
        double originX_pt = marginMm * MM_TO_POINTS;
        double originY_pt = cornerMaxY_pt;

        for (CADElement elem : doc.getElements()) {
            if (activeLayerIds != null && !activeLayerIds.contains(elem.layerId())) {
                continue; // Filtra camada se não estiver ativa
            }
            Layer layer = doc.findLayerById(elem.layerId());
            if (layer != null && !layer.isVisible()) {
                continue; // Pula camadas ocultas
            }

            renderElementToPdf(pw, elem, tileMinX, tileMinY, originX_pt, originY_pt);
        }

        pw.flush();
        return stream.toByteArray();
    }

    private static void drawRegistrationCross(PrintWriter pw, double xPt, double yPt, double lenPt) {
        pw.println("0.75 w 1.0 0.0 0.2 RG"); // Vermelho Vivo para registro
        pw.printf(Locale.US, "%.2f %.2f m %.2f %.2f l S\n", xPt - lenPt / 2, yPt, xPt + lenPt / 2, yPt);
        pw.printf(Locale.US, "%.2f %.2f m %.2f %.2f l S\n", xPt, yPt - lenPt / 2, xPt, yPt + lenPt / 2);
        // Circulo de registro r = 2.5mm
        double r = 2.5 * MM_TO_POINTS;
        double k = 0.55228475 * r;
        pw.printf(Locale.US, "%.2f %.2f m %.2f %.2f %.2f %.2f %.2f %.2f c ", xPt + r, yPt, xPt + r, yPt + k, xPt + k, yPt + r, xPt, yPt + r);
        pw.printf(Locale.US, "%.2f %.2f %.2f %.2f %.2f %.2f c ", xPt - k, yPt + r, xPt - r, yPt + k, xPt - r, yPt);
        pw.printf(Locale.US, "%.2f %.2f %.2f %.2f %.2f %.2f c ", xPt - r, yPt - k, xPt - k, yPt - r, xPt, yPt - r);
        pw.printf(Locale.US, "%.2f %.2f %.2f %.2f %.2f %.2f c S\n", xPt + k, yPt - r, xPt + r, yPt - k, xPt + r, yPt);
    }

    private static void renderElementToPdf(PrintWriter pw, CADElement elem, double tileMinX, double tileMinY, double originX_pt, double originY_pt) {
        pw.println("[] 0 d"); // Reseta estampa tracejada
        pw.println("0.75 w 0 0 0 RG"); // Padrão corte preto 0.75pt

        if (elem instanceof LineElement lineElem) {
            Point2D s = toPdfPt(lineElem.line().start(), tileMinX, tileMinY, originX_pt, originY_pt);
            Point2D e = toPdfPt(lineElem.line().end(), tileMinX, tileMinY, originX_pt, originY_pt);
            pw.printf(Locale.US, "%.2f %.2f m %.2f %.2f l S\n", s.x(), s.y(), e.x(), e.y());
        } else if (elem instanceof RectElement rectElem) {
            Rect2D r = rectElem.rect();
            Point2D p1 = toPdfPt(r.minPoint(), tileMinX, tileMinY, originX_pt, originY_pt);
            Point2D p2 = toPdfPt(new Point2D(r.minPoint().x() + r.width(), r.minPoint().y() + r.height()), tileMinX, tileMinY, originX_pt, originY_pt);

            double x = Math.min(p1.x(), p2.x());
            double y = Math.min(p1.y(), p2.y());
            double w = Math.abs(p1.x() - p2.x());
            double h = Math.abs(p1.y() - p2.y());

            pw.printf(Locale.US, "%.2f %.2f %.2f %.2f re S\n", x, y, w, h);
        } else if (elem instanceof CircleElement circleElem) {
            Point2D centerPt = toPdfPt(circleElem.circle().center(), tileMinX, tileMinY, originX_pt, originY_pt);
            double rPt = circleElem.circle().radius() * MM_TO_POINTS;
            drawPdfCircle(pw, centerPt.x(), centerPt.y(), rPt);
        } else if (elem instanceof StitchElement stitchElem) {
            var cfg = stitchElem.config();
            pw.println("0.0 0.4 0.8 RG"); // Azul para Costura

            if (cfg.type() == StitchType.MACHINE_STITCH) {
                pw.println("[3 3] 0 d"); // Linha tracejada para máquina
                Point2D s = toPdfPt(stitchElem.baseLine().start(), tileMinX, tileMinY, originX_pt, originY_pt);
                Point2D e = toPdfPt(stitchElem.baseLine().end(), tileMinX, tileMinY, originX_pt, originY_pt);
                pw.printf(Locale.US, "%.2f %.2f m %.2f %.2f l S\n", s.x(), s.y(), e.x(), e.y());
            } else if (cfg.type() == StitchType.ROUND_PUNCH || cfg.type() == StitchType.ROUND) {
                double holeRPt = (cfg.holeDiameterMm() / 2.0) * MM_TO_POINTS;
                for (Point2D h : stitchElem.holePoints()) {
                    Point2D hp = toPdfPt(h, tileMinX, tileMinY, originX_pt, originY_pt);
                    drawPdfCircle(pw, hp.x(), hp.y(), holeRPt);
                }
            } else {
                // French slant slots
                for (var slot : stitchElem.calculateSlantSlots()) {
                    Point2D s = toPdfPt(slot.start(), tileMinX, tileMinY, originX_pt, originY_pt);
                    Point2D e = toPdfPt(slot.end(), tileMinX, tileMinY, originX_pt, originY_pt);
                    pw.printf(Locale.US, "%.2f %.2f m %.2f %.2f l S\n", s.x(), s.y(), e.x(), e.y());
                }
            }
        } else if (elem instanceof CreaseElement creaseElem) {
            pw.println("0.0 0.6 0.2 RG"); // Verde para Vincos
            pw.println("[4 2] 0 d");
            Point2D s = toPdfPt(creaseElem.line().start(), tileMinX, tileMinY, originX_pt, originY_pt);
            Point2D e = toPdfPt(creaseElem.line().end(), tileMinX, tileMinY, originX_pt, originY_pt);
            pw.printf(Locale.US, "%.2f %.2f m %.2f %.2f l S\n", s.x(), s.y(), e.x(), e.y());
        } else if (elem instanceof PolylineElement polyElem) {
            var pts = polyElem.polyline().points();
            if (!pts.isEmpty()) {
                Point2D p0 = toPdfPt(pts.getFirst(), tileMinX, tileMinY, originX_pt, originY_pt);
                pw.printf(Locale.US, "%.2f %.2f m ", p0.x(), p0.y());
                for (int i = 1; i < pts.size(); i++) {
                    Point2D p = toPdfPt(pts.get(i), tileMinX, tileMinY, originX_pt, originY_pt);
                    pw.printf(Locale.US, "%.2f %.2f l ", p.x(), p.y());
                }
                if (polyElem.polyline().isClosed()) {
                    pw.println("h S");
                } else {
                    pw.println("S");
                }
            }
        } else if (elem instanceof DimensionElement dimElem) {
            pw.println("0.8 0.5 0.0 RG"); // Laranja/Amarelo para Cotas
            Point2D s = toPdfPt(dimElem.start(), tileMinX, tileMinY, originX_pt, originY_pt);
            Point2D e = toPdfPt(dimElem.end(), tileMinX, tileMinY, originX_pt, originY_pt);
            pw.printf(Locale.US, "%.2f %.2f m %.2f %.2f l S\n", s.x(), s.y(), e.x(), e.y());

            pw.println("BT /F1 7 Tf 0.8 0.5 0.0 rg");
            pw.printf(Locale.US, "%.2f %.2f Td (%s) Tj ET\n", (s.x() + e.x()) / 2.0, (s.y() + e.y()) / 2.0 + 2, escapePdfString(dimElem.formattedText()));
        }
    }

    private static void drawPdfCircle(PrintWriter pw, double cxPt, double cyPt, double rPt) {
        double k = 0.55228475 * rPt;
        pw.printf(Locale.US, "%.2f %.2f m %.2f %.2f %.2f %.2f %.2f %.2f c ", cxPt + rPt, cyPt, cxPt + rPt, cyPt + k, cxPt + k, cyPt + rPt, cxPt, cyPt + rPt);
        pw.printf(Locale.US, "%.2f %.2f %.2f %.2f %.2f %.2f c ", cxPt - k, cyPt + rPt, cxPt - rPt, cyPt + k, cxPt - rPt, cyPt);
        pw.printf(Locale.US, "%.2f %.2f %.2f %.2f %.2f %.2f c ", cxPt - rPt, cyPt - k, cxPt - k, cyPt - rPt, cxPt, cyPt - rPt);
        pw.printf(Locale.US, "%.2f %.2f %.2f %.2f %.2f %.2f c S\n", cxPt + k, cyPt - rPt, cxPt + rPt, cyPt - k, cxPt + rPt, cyPt);
    }

    private static Point2D toPdfPt(Point2D cadPt, double tileMinX, double tileMinY, double originX_pt, double originY_pt) {
        double relX_mm = cadPt.x() - tileMinX;
        double relY_mm = cadPt.y() - tileMinY;

        double pdfX = originX_pt + (relX_mm * MM_TO_POINTS);
        double pdfY = originY_pt - (relY_mm * MM_TO_POINTS);
        return new Point2D(pdfX, pdfY);
    }

    private static String escapePdfString(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private static void writeString(ByteArrayOutputStream out, String s) throws IOException {
        out.write(s.getBytes(StandardCharsets.US_ASCII));
    }
}
