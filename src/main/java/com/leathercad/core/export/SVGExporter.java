package com.leathercad.core.export;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.model.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

public class SVGExporter {
    public static String exportToString(Document doc) {
        StringBuilder sb = new StringBuilder();

        // Calculate bounding box of all elements
        double minX = 0, minY = 0, width = 300, height = 200;
        if (!doc.getElements().isEmpty()) {
            double minXVal = Double.MAX_VALUE, minYVal = Double.MAX_VALUE;
            double maxXVal = -Double.MAX_VALUE, maxYVal = -Double.MAX_VALUE;
            for (CADElement elem : doc.getElements()) {
                Rect2D box = elem.boundingBox();
                minXVal = Math.min(minXVal, box.minPoint().x());
                minYVal = Math.min(minYVal, box.minPoint().y());
                maxXVal = Math.max(maxXVal, box.minPoint().x() + box.width());
                maxYVal = Math.max(maxYVal, box.minPoint().y() + box.height());
            }
            minX = minXVal - 10;
            minY = minYVal - 10;
            width = (maxXVal - minXVal) + 20;
            height = (maxYVal - minYVal) + 20;
        }

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
        sb.append(String.format(Locale.US,
            "<svg width=\"%.2fmm\" height=\"%.2fmm\" viewBox=\"%.2f %.2f %.2f %.2f\" xmlns=\"http://www.w3.org/2000/svg\">\n",
            width, height, minX, minY, width, height));

        for (Layer layer : doc.getLayers()) {
            if (!layer.isVisible()) continue;
            sb.append(String.format(Locale.US, "  <g id=\"%s\" stroke=\"%s\" stroke-width=\"0.5\" fill=\"none\">\n",
                layer.getName(), layer.getColorHex()));

            for (CADElement elem : doc.getElements()) {
                if (!elem.layerId().equals(layer.getId())) continue;

                if (elem instanceof LineElement lineElem) {
                    Point2D s = lineElem.line().start();
                    Point2D e = lineElem.line().end();
                    sb.append(String.format(Locale.US, "    <line x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\" />\n",
                        s.x(), s.y(), e.x(), e.y()));
                } else if (elem instanceof RectElement rectElem) {
                    Rect2D r = rectElem.rect();
                    sb.append(String.format(Locale.US, "    <rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" rx=\"%.2f\" />\n",
                        r.minPoint().x(), r.minPoint().y(), r.width(), r.height(), r.cornerRadius()));
                } else if (elem instanceof CircleElement circleElem) {
                    Point2D c = circleElem.circle().center();
                    sb.append(String.format(Locale.US, "    <circle cx=\"%.2f\" cy=\"%.2f\" r=\"%.2f\" />\n",
                        c.x(), c.y(), circleElem.circle().radius()));
                } else if (elem instanceof com.leathercad.core.leather.StitchElement stitchElem) {
                    var cfg = stitchElem.config();
                    if (cfg.type() == com.leathercad.core.leather.StitchType.MACHINE_STITCH) {
                        Point2D s = stitchElem.baseLine().start();
                        Point2D e = stitchElem.baseLine().end();
                        sb.append(String.format(Locale.US, "    <line x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\" stroke=\"#FFD700\" stroke-dasharray=\"4,2\" stroke-width=\"0.8\" />\n",
                            s.x(), s.y(), e.x(), e.y()));
                    } else if (cfg.type() == com.leathercad.core.leather.StitchType.ROUND_PUNCH || cfg.type() == com.leathercad.core.leather.StitchType.ROUND) {
                        for (Point2D hole : stitchElem.holePoints()) {
                            sb.append(String.format(Locale.US, "    <circle cx=\"%.2f\" cy=\"%.2f\" r=\"%.2f\" fill=\"none\" stroke=\"#FFD700\" stroke-width=\"0.4\" />\n",
                                hole.x(), hole.y(), cfg.holeDiameterMm() / 2.0));
                        }
                    } else {
                        // FRENCH_SLANT / DEFAULT
                        var slots = stitchElem.calculateSlantSlots();
                        for (var slot : slots) {
                            sb.append(String.format(Locale.US, "    <line x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\" stroke=\"#FFD700\" stroke-width=\"0.6\" />\n",
                                slot.start().x(), slot.start().y(), slot.end().x(), slot.end().y()));
                        }
                    }
                } else if (elem instanceof com.leathercad.core.leather.CreaseElement creaseElem) {
                    Point2D s = creaseElem.line().start();
                    Point2D e = creaseElem.line().end();
                    sb.append(String.format(Locale.US, "    <line x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\" stroke=\"#FF8C00\" stroke-dasharray=\"2,2\" />\n",
                        s.x(), s.y(), e.x(), e.y()));
                } else if (elem instanceof PolylineElement polyElem) {
                    var poly = polyElem.polyline();
                    StringBuilder pointsStr = new StringBuilder();
                    for (Point2D p : poly.points()) {
                        pointsStr.append(String.format(Locale.US, "%.2f,%.2f ", p.x(), p.y()));
                    }
                    String tag = poly.isClosed() ? "polygon" : "polyline";
                    sb.append(String.format(Locale.US, "    <%s points=\"%s\" />\n", tag, pointsStr.toString().trim()));
                } else if (elem instanceof DimensionElement dimElem) {
                    Point2D s = dimElem.start();
                    Point2D e = dimElem.end();
                    sb.append(String.format(Locale.US, "    <line x1=\"%.2f\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\" stroke=\"#00E676\" stroke-width=\"0.5\" stroke-dasharray=\"2,2\" />\n",
                        s.x(), s.y(), e.x(), e.y()));
                    sb.append(String.format(Locale.US, "    <text x=\"%.2f\" y=\"%.2f\" font-family=\"Segoe UI, sans-serif\" font-size=\"3\" fill=\"#00E676\">%s</text>\n",
                        (s.x() + e.x()) / 2.0, (s.y() + e.y()) / 2.0, dimElem.formattedText()));
                }
            }
            sb.append("  </g>\n");
        }

        sb.append("</svg>\n");
        return sb.toString();
    }

    public static void exportToFile(Document doc, File targetFile) throws IOException {
        try (FileWriter writer = new FileWriter(targetFile)) {
            writer.write(exportToString(doc));
        }
    }
}
