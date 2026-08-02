package com.leathercad.core.export;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.leather.CreaseElement;
import com.leathercad.core.leather.StitchElement;
import com.leathercad.core.leather.StitchType;
import com.leathercad.core.model.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;

public class DXFExporter {

    public static final String CUT_LAYER = "CUT_LAYER";
    public static final String STITCH_LAYER = "STITCH_LAYER";
    public static final String CREASE_LAYER = "CREASE_LAYER";
    public static final String ANNOTATION_LAYER = "ANNOTATION_LAYER";

    public static String exportToString(Document doc) {
        return exportToString(doc, null);
    }

    public static String exportToString(Document doc, Set<String> activeLayerIds) {
        StringBuilder sb = new StringBuilder();

        // DXF HEADER
        sb.append("0\nSECTION\n2\nHEADER\n0\nENDSEC\n");

        // DXF TABLES (CAMADAS PADRONIZADAS CNC/LASER)
        sb.append("0\nSECTION\n2\nTABLES\n0\nTABLE\n2\nLAYER\n70\n4\n");

        // Camada 1: Corte (Vermelho - 1)
        appendLayerTableEntry(sb, CUT_LAYER, 1);
        // Camada 2: Costura (Azul - 5)
        appendLayerTableEntry(sb, STITCH_LAYER, 5);
        // Camada 3: Vinco (Verde - 3)
        appendLayerTableEntry(sb, CREASE_LAYER, 3);
        // Camada 4: Cotas (Amarelo - 2)
        appendLayerTableEntry(sb, ANNOTATION_LAYER, 2);

        // Camadas customizadas do documento
        for (Layer layer : doc.getLayers()) {
            if (!layer.getName().equals(CUT_LAYER) && !layer.getName().equals(STITCH_LAYER) &&
                !layer.getName().equals(CREASE_LAYER) && !layer.getName().equals(ANNOTATION_LAYER)) {
                appendLayerTableEntry(sb, layer.getName(), 7); // Branco/Padrão
            }
        }

        sb.append("0\nENDTAB\n0\nENDSEC\n");

        // DXF ENTITIES
        sb.append("0\nSECTION\n2\nENTITIES\n");

        for (CADElement elem : doc.getElements()) {
            // Filtro por camada ativa se fornecido
            if (activeLayerIds != null && !activeLayerIds.contains(elem.layerId())) {
                continue;
            }

            Layer layer = doc.findLayerById(elem.layerId());
            if (layer != null && !layer.isVisible()) {
                continue; // Pula elementos em camadas ocultas
            }

            String targetLayer = resolveDxfLayerName(elem, layer);

            if (elem instanceof LineElement lineElem) {
                Point2D s = lineElem.line().start();
                Point2D e = lineElem.line().end();
                appendLine(sb, targetLayer, s.x(), s.y(), e.x(), e.y());
            } else if (elem instanceof RectElement rectElem) {
                Rect2D r = rectElem.rect();
                double x1 = r.minPoint().x();
                double y1 = r.minPoint().y();
                double x2 = x1 + r.width();
                double y2 = y1 + r.height();

                appendLine(sb, targetLayer, x1, y1, x2, y1);
                appendLine(sb, targetLayer, x2, y1, x2, y2);
                appendLine(sb, targetLayer, x2, y2, x1, y2);
                appendLine(sb, targetLayer, x1, y2, x1, y1);
            } else if (elem instanceof CircleElement circleElem) {
                Point2D c = circleElem.circle().center();
                appendCircle(sb, targetLayer, c.x(), c.y(), circleElem.circle().radius());
            } else if (elem instanceof StitchElement stitchElem) {
                var cfg = stitchElem.config();
                if (cfg.type() == StitchType.MACHINE_STITCH) {
                    Point2D s = stitchElem.baseLine().start();
                    Point2D e = stitchElem.baseLine().end();
                    appendLine(sb, targetLayer, s.x(), s.y(), e.x(), e.y());
                } else if (cfg.type() == StitchType.ROUND_PUNCH || cfg.type() == StitchType.ROUND) {
                    for (Point2D hole : stitchElem.holePoints()) {
                        appendCircle(sb, targetLayer, hole.x(), hole.y(), cfg.holeDiameterMm() / 2.0);
                    }
                } else {
                    for (var slot : stitchElem.calculateSlantSlots()) {
                        appendLine(sb, targetLayer, slot.start().x(), slot.start().y(), slot.end().x(), slot.end().y());
                    }
                }
            } else if (elem instanceof CreaseElement creaseElem) {
                Point2D s = creaseElem.line().start();
                Point2D e = creaseElem.line().end();
                appendLine(sb, targetLayer, s.x(), s.y(), e.x(), e.y());
            } else if (elem instanceof PolylineElement polyElem) {
                var poly = polyElem.polyline();
                var pts = poly.points();
                if (!pts.isEmpty()) {
                    sb.append("0\nLWPOLYLINE\n");
                    sb.append("8\n").append(targetLayer).append("\n");
                    sb.append("90\n").append(pts.size()).append("\n");
                    sb.append("70\n").append(poly.isClosed() ? "1\n" : "0\n");
                    for (Point2D p : pts) {
                        sb.append(String.format(Locale.US, "10\n%.4f\n20\n%.4f\n", p.x(), p.y()));
                    }
                }
            } else if (elem instanceof DimensionElement dimElem) {
                Point2D s = dimElem.start();
                Point2D e = dimElem.end();
                appendLine(sb, targetLayer, s.x(), s.y(), e.x(), e.y());

                sb.append("0\nTEXT\n");
                sb.append("8\n").append(targetLayer).append("\n");
                sb.append(String.format(Locale.US, "10\n%.4f\n20\n%.4f\n30\n0.0\n", (s.x() + e.x()) / 2.0, (s.y() + e.y()) / 2.0));
                sb.append(String.format(Locale.US, "40\n%.4f\n", 2.5));
                sb.append("1\n").append(dimElem.formattedText()).append("\n");
            }
        }

        sb.append("0\nENDSEC\n0\nEOF\n");
        return sb.toString();
    }

    private static void appendLayerTableEntry(StringBuilder sb, String layerName, int colorCode) {
        sb.append("0\nLAYER\n");
        sb.append("2\n").append(layerName).append("\n");
        sb.append("70\n0\n");
        sb.append("62\n").append(colorCode).append("\n");
        sb.append("6\nCONTINUOUS\n");
    }

    private static String resolveDxfLayerName(CADElement elem, Layer layer) {
        if (elem instanceof StitchElement) {
            return STITCH_LAYER;
        } else if (elem instanceof CreaseElement) {
            return CREASE_LAYER;
        } else if (elem instanceof DimensionElement) {
            return ANNOTATION_LAYER;
        } else if (elem instanceof LineElement || elem instanceof RectElement || elem instanceof CircleElement || elem instanceof PolylineElement) {
            return CUT_LAYER;
        }
        return layer != null ? layer.getName() : CUT_LAYER;
    }

    private static void appendLine(StringBuilder sb, String layerName, double x1, double y1, double x2, double y2) {
        sb.append("0\nLINE\n");
        sb.append("8\n").append(layerName).append("\n");
        sb.append(String.format(Locale.US, "10\n%.4f\n20\n%.4f\n30\n0.0\n", x1, y1));
        sb.append(String.format(Locale.US, "11\n%.4f\n21\n%.4f\n31\n0.0\n", x2, y2));
    }

    private static void appendCircle(StringBuilder sb, String layerName, double cx, double cy, double radius) {
        sb.append("0\nCIRCLE\n");
        sb.append("8\n").append(layerName).append("\n");
        sb.append(String.format(Locale.US, "10\n%.4f\n20\n%.4f\n30\n0.0\n", cx, cy));
        sb.append(String.format(Locale.US, "40\n%.4f\n", radius));
    }

    public static void exportToFile(Document doc, File targetFile) throws IOException {
        exportToFile(doc, null, targetFile);
    }

    public static void exportToFile(Document doc, Set<String> activeLayerIds, File targetFile) throws IOException {
        try (FileWriter writer = new FileWriter(targetFile)) {
            writer.write(exportToString(doc, activeLayerIds));
        }
    }
}
