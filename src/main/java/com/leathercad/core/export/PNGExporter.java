package com.leathercad.core.export;

import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.model.*;
import com.leathercad.ui.viewport.CanvasViewport;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PNGExporter {

    public static void exportViewportToPNG(CanvasViewport viewport, File targetFile) throws IOException {
        WritableImage writableImage = new WritableImage((int) viewport.getWidth(), (int) viewport.getHeight());
        viewport.snapshot(null, writableImage);
        ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null), "png", targetFile);
    }

    public static void exportToFile(Document doc, File targetFile) throws IOException {
        Rect2D bbox = doc.getBoundingBox();
        int margin = 20;
        int imgWidth = Math.max(800, (int) Math.ceil(bbox.width()) + margin * 2);
        int imgHeight = Math.max(600, (int) Math.ceil(bbox.height()) + margin * 2);

        BufferedImage img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fundo escuro CAD
        g2.setColor(new Color(0x14, 0x14, 0x14));
        g2.fillRect(0, 0, imgWidth, imgHeight);

        double minX = bbox.minPoint().x() - margin;
        double minY = bbox.minPoint().y() - margin;

        g2.setColor(new Color(0x00, 0xFF, 0x88));

        for (CADElement elem : doc.getElements()) {
            if (elem instanceof LineElement lineElem) {
                int x1 = (int) (lineElem.line().start().x() - minX);
                int y1 = (int) (lineElem.line().start().y() - minY);
                int x2 = (int) (lineElem.line().end().x() - minX);
                int y2 = (int) (lineElem.line().end().y() - minY);
                g2.drawLine(x1, y1, x2, y2);
            } else if (elem instanceof RectElement rectElem) {
                Rect2D r = rectElem.rect();
                int x = (int) (r.minPoint().x() - minX);
                int y = (int) (r.minPoint().y() - minY);
                int w = (int) r.width();
                int h = (int) r.height();
                g2.drawRect(x, y, w, h);
            } else if (elem instanceof CircleElement circleElem) {
                double radius = circleElem.circle().radius();
                int x = (int) (circleElem.circle().center().x() - minX - radius);
                int y = (int) (circleElem.circle().center().y() - minY - radius);
                int diameter = (int) (radius * 2);
                g2.drawOval(x, y, diameter, diameter);
            }
        }

        g2.dispose();
        ImageIO.write(img, "png", targetFile);
    }
}
