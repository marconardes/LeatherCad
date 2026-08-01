package com.leathercad.core.nesting;

import com.leathercad.core.components.CardSlotComponent;
import com.leathercad.core.geometry.LineSegment;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.leather.StitchConfig;
import com.leathercad.core.leather.StitchElement;
import com.leathercad.core.model.CADElement;
import com.leathercad.core.model.Document;
import com.leathercad.core.model.RectElement;
import com.leathercad.core.parametric.CardHolderTemplate;
import com.leathercad.core.parametric.ProjectVariables;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class NestingEngineTest {

    @Test
    public void testHideSheetPresets() {
        HideSheet fullHide = HideSheet.FULL_HIDE_1000x800;
        assertEquals(1000.0, fullHide.widthMm());
        assertEquals(800.0, fullHide.heightMm());
        assertEquals(8000.0, fullHide.areaCm2());

        HideSheet a4 = HideSheet.SHEET_A4;
        assertEquals(297.0, a4.widthMm());
    }

    @Test
    public void testNestingOptimization() {
        Document doc = new Document();
        String layerId = doc.getActiveLayer().getId();

        doc.addElement(new RectElement(layerId, new Rect2D(0, 0, 100, 80)));
        doc.addElement(new RectElement(layerId, new Rect2D(0, 0, 95, 55)));
        doc.addElement(new RectElement(layerId, new Rect2D(0, 0, 100, 70)));

        NestingResult result = NestingOptimizer.optimizeLayout(doc, HideSheet.SHEET_A3, true);

        assertNotNull(result);
        assertEquals(3, result.placedPieces().size());
        assertTrue(result.efficiencyPercent() > 0.0);
        assertTrue(result.usedAreaCm2() > 0.0);
        assertTrue(result.placedPieces().stream().allMatch(NestingPiece::isPlaced));
    }

    @Test
    public void testChildElementsAssociationInNesting() {
        Document doc = new Document();
        String layerId = doc.getActiveLayer().getId();

        RectElement piece = new RectElement(layerId, new Rect2D(10, 10, 100, 80));
        StitchElement stitch = new StitchElement(layerId, new LineSegment(new Point2D(15, 15), new Point2D(95, 15)), StitchConfig.DEFAULT_FRENCH);

        doc.addElement(piece);
        doc.addElement(stitch);

        NestingResult result = NestingOptimizer.optimizeLayout(doc, HideSheet.FULL_HIDE_1000x800, true);

        assertNotNull(result);
        NestingPiece p1 = result.placedPieces().getFirst();
        assertEquals(1, p1.childElementIds().size());
        assertEquals(stitch.id(), p1.childElementIds().getFirst());
    }

    @Test
    public void testMultipleIdenticalCardSlotsOwnership() {
        Document doc = new Document();
        String layerId = doc.getActiveLayer().getId();

        // 3 porta-cartões idênticos em posições Y diferentes
        List<CADElement> slot1 = CardSlotComponent.createCardSlot(layerId, new Point2D(10, 10), 95, 45);
        List<CADElement> slot2 = CardSlotComponent.createCardSlot(layerId, new Point2D(10, 60), 95, 45);
        List<CADElement> slot3 = CardSlotComponent.createCardSlot(layerId, new Point2D(10, 110), 95, 45);

        for (CADElement e : slot1) doc.addElement(e);
        for (CADElement e : slot2) doc.addElement(e);
        for (CADElement e : slot3) doc.addElement(e);

        NestingResult result = NestingOptimizer.optimizeLayout(doc, HideSheet.FULL_HIDE_1000x800, true);
        assertNotNull(result);

        // Deve haver 3 peças de corte de porta-cartão no Nesting
        assertEquals(3, result.placedPieces().size());

        // Cada porta-cartão é um bloco atômico CADGroup que encapsula seus 3 sub-elementos internos
        for (NestingPiece piece : result.placedPieces()) {
            CADElement elem = doc.findElementById(piece.id());
            assertTrue(elem instanceof com.leathercad.core.model.CADGroup);
            com.leathercad.core.model.CADGroup group = (com.leathercad.core.model.CADGroup) elem;
            assertEquals(3, group.children().size(), "Cada bloco atômico de porta-cartão encapsula 3 sub-elementos");
        }
    }

    @Test
    public void testBifoldWalletNestingApplyWithoutDoubleMovements() {
        Document doc = new Document();
        CardHolderTemplate.generateCardHolder(doc, ProjectVariables.DEFAULT_BIFOLD);

        int totalOriginalElements = doc.getElements().size();
        assertTrue(totalOriginalElements > 5);

        NestingResult result = NestingOptimizer.optimizeLayout(doc, HideSheet.FULL_HIDE_1000x800, true);
        assertNotNull(result);

        // Aplica o Nesting
        List<CADElement> newDocElements = NestingOptimizer.applyNestingToDocument(doc, result);

        assertNotNull(newDocElements);
        assertEquals(totalOriginalElements, newDocElements.size());
    }

    @Test
    public void testRotatedNestingPieceCoordinatesExactFit() {
        Document doc = new Document();
        String layerId = doc.getActiveLayer().getId();

        RectElement piece = new RectElement(layerId, new Rect2D(10, 10, 95, 45));
        doc.addElement(piece);

        NestingPiece nestingPiece = new NestingPiece(piece.id(), layerId, "Teste", 95, 45);
        nestingPiece.setOrigMinPoint(new Point2D(10, 10));
        nestingPiece.place(100, 100, 90.0); // Colocado em (100, 100) com rotação de 90°

        NestingResult result = new NestingResult(
            List.of(nestingPiece),
            HideSheet.FULL_HIDE_1000x800,
            8000, 42.75, 7957.25, 0.5, 1
        );

        List<CADElement> newDoc = NestingOptimizer.applyNestingToDocument(doc, result);
        assertNotNull(newDoc);
        assertEquals(1, newDoc.size());

        CADElement rotatedPiece = newDoc.getFirst();
        Rect2D bbox = rotatedPiece.boundingBox();

        // Após rotacionar 90°, minPoint.x deve ser 100 e minPoint.y deve ser 100 (tolerância milimétrica de 0.1mm)
        assertEquals(100.0, bbox.minPoint().x(), 0.1, "MinPoint X da peça rotacionada 90° deve coincidir exatamente com placedX");
        assertEquals(100.0, bbox.minPoint().y(), 0.1, "MinPoint Y da peça rotacionada 90° deve coincidir exatamente com placedY");
    }
}
