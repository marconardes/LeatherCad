package com.leathercad.core.export;

import com.leathercad.ui.viewport.CanvasViewport;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

public class PNGExporter {
    public static void exportViewportToPNG(CanvasViewport viewport, File targetFile) throws IOException {
        WritableImage writableImage = new WritableImage((int) viewport.getWidth(), (int) viewport.getHeight());
        viewport.snapshot(null, writableImage);
        ImageIO.write(SwingFXUtils.fromFXImage(writableImage, null), "png", targetFile);
    }
}
