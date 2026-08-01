package com.leathercad.core.print;

import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.model.Document;
import com.leathercad.core.model.RectElement;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PrintEngineTest {

    @Test
    public void testPaperPresets() {
        PrintPaperSize a4 = PrintPaperSize.A4;
        assertEquals(210.0, a4.widthMm());
        assertEquals(297.0, a4.heightMm());

        PrintPaperSize a3 = PrintPaperSize.A3;
        assertEquals(297.0, a3.widthMm());
        assertEquals(420.0, a3.heightMm());
    }

    @Test
    public void testTileCalculationForLargePattern() {
        Document doc = new Document();
        String layerId = doc.getActiveLayer().getId();

        // Molde grande de 400mm x 300mm que estoura 1 folha A4 (210x297)
        doc.addElement(new RectElement(layerId, new Rect2D(0, 0, 400, 300)));

        List<PrintTile> tiles = PrintEngine.calculateTiles(doc, PrintPaperSize.A4, 5.0, 10.0);

        assertNotNull(tiles);
        assertFalse(tiles.isEmpty());

        // Deve requerer mais de 1 página A4 (pelo menos 2x2 = 4 páginas)
        assertTrue(tiles.size() >= 4);

        PrintTile tile1 = tiles.getFirst();
        assertEquals(1, tile1.pageIndex());
        assertEquals(tiles.size(), tile1.totalPages());
        assertNotNull(tile1.formattedLabel());
    }
}
