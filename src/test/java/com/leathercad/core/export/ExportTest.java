package com.leathercad.core.export;

import com.leathercad.core.geometry.LineSegment;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.leather.CreaseElement;
import com.leathercad.core.leather.StitchConfig;
import com.leathercad.core.leather.StitchElement;
import com.leathercad.core.model.DimensionElement;
import com.leathercad.core.model.Document;
import com.leathercad.core.model.LineElement;
import com.leathercad.core.model.RectElement;
import com.leathercad.core.print.PrintPDFExporter;
import com.leathercad.core.print.PrintPaperSize;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class ExportTest {

    @Test
    public void testSVGExport() {
        Document doc = new Document();
        LineSegment line = new LineSegment(new Point2D(0, 0), new Point2D(85, 54));
        doc.addElement(new LineElement(doc.getActiveLayer().getId(), line));

        String svg = SVGExporter.exportToString(doc);

        assertNotNull(svg);
        assertTrue(svg.contains("<svg"));
        assertTrue(svg.contains("<line"));
        assertTrue(svg.contains("85.00"));
    }

    @Test
    public void testDXFExportWithIndustrialLayers() {
        Document doc = new Document();
        String layerId = doc.getActiveLayer().getId();

        doc.addElement(new RectElement(layerId, new Rect2D(0, 0, 100, 50)));
        doc.addElement(new StitchElement(layerId, new LineSegment(new Point2D(5, 5), new Point2D(95, 5)), StitchConfig.DEFAULT_FRENCH));
        doc.addElement(new CreaseElement(layerId, new LineSegment(new Point2D(0, 2), new Point2D(100, 2)), 1.5));
        doc.addElement(new DimensionElement(layerId, new Point2D(0, 0), new Point2D(100, 0), DimensionElement.DimensionType.HORIZONTAL, 10.0));

        String dxf = DXFExporter.exportToString(doc);

        assertNotNull(dxf);
        assertTrue(dxf.contains("SECTION"));
        assertTrue(dxf.contains("ENTITIES"));
        assertTrue(dxf.contains("CUT_LAYER"), "DXF deve possuir a camada CUT_LAYER");
        assertTrue(dxf.contains("STITCH_LAYER"), "DXF deve possuir a camada STITCH_LAYER");
        assertTrue(dxf.contains("CREASE_LAYER"), "DXF deve possuir a camada CREASE_LAYER");
        assertTrue(dxf.contains("ANNOTATION_LAYER"), "DXF deve possuir a camada ANNOTATION_LAYER");
        assertTrue(dxf.contains("EOF"));
    }

    @Test
    public void testPrintPDFExporter1to1WithCalibrationSquare() throws IOException {
        Document doc = new Document();
        doc.addElement(new RectElement(doc.getActiveLayer().getId(), new Rect2D(10, 10, 300, 450))); // Molde maior que A4 (vai gerar tiling)

        File pdfFile = File.createTempFile("mold_export_1to1", ".pdf");
        pdfFile.deleteOnExit();

        PrintPDFExporter.exportMultiPagePDF(doc, PrintPaperSize.A4, 10.0, 10.0, pdfFile);

        assertTrue(pdfFile.exists() && pdfFile.length() > 500, "Arquivo PDF vetorial deve ser criado com tamanho válido");

        // Ler conteúdo do PDF como string ASCII para validar cabeçalho PDF 1.4 e marcas de aferição
        byte[] bytes = java.nio.file.Files.readAllBytes(pdfFile.toPath());
        String pdfHeader = new String(bytes, 0, Math.min(bytes.length, 1000), java.nio.charset.StandardCharsets.US_ASCII);

        assertTrue(pdfHeader.contains("%PDF-1.4"), "Arquivo deve ser PDF v1.4 válido");
        assertTrue(pdfHeader.contains("AFERICAO DE ESCALA 50mm x 50mm") || pdfHeader.contains("Folha"), "PDF deve possuir texto de aferição e cabeçalho");
    }
}
