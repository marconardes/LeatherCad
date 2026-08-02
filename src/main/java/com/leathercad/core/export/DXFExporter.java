package com.leathercad.core.export;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.model.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

public class DXFExporter {
    public static String exportToString(Document doc) {
        StringBuilder sb = new StringBuilder();

        // DXF HEADER
        sb.append("0\nSECTION\n2\nHEADER\n0\nENDSEC\n");

        // DXF TABLES
        sb.append("0\nSECTION\n2\nTABLES\n0\nTABLE\n2\nLAYER\n");
        for (Layer layer : doc.getLayers()) {
            sb.append("0\nLAYER\n");
            sb.append("2\n").append(layer.getName()).append("\n");
            sb.append("70\n0\n");
            sb.append("62\n7\n"); // White/Default color
            sb.append("6\nCONTINUOUS\n");
        }
        sb.append("0\nENDTAB\n0\nENDSEC\n");

        // DXF ENTITIES
        sb.append("0\nSECTION\n2\nENTITIES\n");

        for (Layer layer : doc.getLayers()) {
            if (!layer.isVisible()) continue;

            for (CADElement elem : doc.getElements()) {
                if (!elem.layerId().equals(layer.getId())) continue;

                if (elem instanceof LineElement lineElem) {
                    Point2D s = lineElem.line().start();
                    Point2D e = lineElem.line().end();
                    sb.append("0\nLINE\n");
                    sb.append("8\n").append(layer.getName()).append("\n");
                    sb.append(String.format(Locale.US, "10\n%.4f\n20\n%.4f\n30\n0.0\n", s.x(), s.y()));
                    sb.append(String.format(Locale.US, "11\n%.4f\n21\n%.4f\n31\n0.0\n", e.x(), e.y()));
                } else if (elem instanceof RectElement rectElem) {
                    Rect2D r = rectElem.rect();
                    double x1 = r.minPoint().x();
                    double y1 = r.minPoint().y();
                    double x2 = x1 + r.width();
                    double y2 = y1 + r.height();

                    // 4 lines for rectangle
                    appendLine(sb, layer.getName(), x1, y1, x2, y1);
                    appendLine(sb, layer.getName(), x2, y1, x2, y2);
                    appendLine(sb, layer.getName(), x2, y2, x1, y2);
                    appendLine(sb, layer.getName(), x1, y2, x1, y1);
                } else if (elem instanceof CircleElement circleElem) {
                    Point2D c = circleElem.circle().center();
                    sb.append("0\nCIRCLE\n");
                    sb.append("8\n").append(layer.getName()).append("\n");
                    sb.append(String.format(Locale.US, "10\n%.4f\n20\n%.4f\n30\n0.0\n", c.x(), c.y()));
                    sb.append(String.format(Locale.US, "40\n%.4f\n", circleElem.circle().radius()));
                } else if (elem instanceof com.leathercad.core.leather.StitchElement stitchElem) {
                    var cfg = stitchElem.config();
                    if (cfg.type() == com.leathercad.core.leather.StitchType.MACHINE_STITCH) {
                        Point2D s = stitchElem.baseLine().start();
                        Point2D e = stitchElem.baseLine().end();
                        appendLine(sb, layer.getName(), s.x(), s.y(), e.x(), e.y());
                    } else if (cfg.type() == com.leathercad.core.leather.StitchType.ROUND_PUNCH || cfg.type() == com.leathercad.core.leather.StitchType.ROUND) {
                        for (Point2D hole : stitchElem.holePoints()) {
                            sb.append("0\nCIRCLE\n");
                            sb.append("8\n").append(layer.getName()).append("\n");
                            sb.append(String.format(Locale.US, "10\n%.4f\n20\n%.4f\n30\n0.0\n", hole.x(), hole.y()));
                            sb.append(String.format(Locale.US, "40\n%.4f\n", cfg.holeDiameterMm() / 2.0));
                        }
                    } else {
                        // FRENCH_SLANT / DEFAULT
                        var slots = stitchElem.calculateSlantSlots();
                        for (var slot : slots) {
                            appendLine(sb, layer.getName(), slot.start().x(), slot.start().y(), slot.end().x(), slot.end().y());
                        }
                    }
                } else if (elem instanceof com.leathercad.core.leather.CreaseElement creaseElem) {
                    Point2D s = creaseElem.line().start();
                    Point2D e = creaseElem.line().end();
                    appendLine(sb, layer.getName(), s.x(), s.y(), e.x(), e.y());
                } else if (elem instanceof PolylineElement polyElem) {
                    var poly = polyElem.polyline();
                    var pts = poly.points();
                    if (!pts.isEmpty()) {
                        sb.append("0\nLWPOLYLINE\n");
                        sb.append("8\n").append(layer.getName()).append("\n");
                        sb.append("90\n").append(pts.size()).append("\n");
                        sb.append("70\n").append(poly.isClosed() ? "1\n" : "0\n");
                        for (Point2D p : pts) {
                            sb.append(String.format(Locale.US, "10\n%.4f\n20\n%.4f\n", p.x(), p.y()));
                        }
                    }
                }
            }
        }

        sb.append("0\nENDSEC\n0\nEOF\n");
        return sb.toString();
    }

    private static void appendLine(StringBuilder sb, String layerName, double x1, double y1, double x2, double y2) {
        sb.append("0\nLINE\n");
        sb.append("8\n").append(layerName).append("\n");
        sb.append(String.format(Locale.US, "10\n%.4f\n20\n%.4f\n30\n0.0\n", x1, y1));
        sb.append(String.format(Locale.US, "11\n%.4f\n21\n%.4f\n31\n0.0\n", x2, y2));
    }

    public static void exportToFile(Document doc, File targetFile) throws IOException {
        try (FileWriter writer = new FileWriter(targetFile)) {
            writer.write(exportToString(doc));
        }
    }
}
