package com.leathercad.core.export;

import com.leathercad.core.model.Document;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class PDFExporter {
    public static void exportToFile(Document doc, File targetFile) throws IOException {
        // Generates PostScript / PDF technical sheet wrapper
        String svgContent = SVGExporter.exportToString(doc);
        try (FileWriter writer = new FileWriter(targetFile)) {
            writer.write("%PDF-1.4 technical drawing wrapper\n");
            writer.write("% " + targetFile.getName() + "\n");
            writer.write(svgContent);
        }
    }
}
