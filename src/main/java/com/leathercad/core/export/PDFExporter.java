package com.leathercad.core.export;

import com.leathercad.core.model.Document;
import com.leathercad.core.print.PrintPDFExporter;
import com.leathercad.core.print.PrintPaperSize;

import java.io.File;
import java.io.IOException;

public class PDFExporter {

    public static void exportToFile(Document doc, File targetFile) throws IOException {
        PrintPDFExporter.exportMultiPagePDF(doc, PrintPaperSize.A4, 10.0, 10.0, targetFile);
    }
}
