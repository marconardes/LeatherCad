package com.leathercad.core.model;

import com.leathercad.core.components.CardSlotComponent;
import com.leathercad.core.geometry.LineSegment;
import com.leathercad.core.geometry.Point2D;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EditTest {

    @Test
    public void testMoveSelected() {
        Document doc = new Document();
        LineSegment line = new LineSegment(new Point2D(0, 0), new Point2D(10, 0));
        LineElement elem = new LineElement(doc.getActiveLayer().getId(), line);

        doc.addElement(elem);
        doc.selectElement(elem.id(), false);

        doc.moveSelected(5, 5);

        assertEquals(1, doc.getElements().size());
        CADElement moved = doc.getElements().get(0);
        assertTrue(moved instanceof LineElement);
        LineElement movedLine = (LineElement) moved;
        assertEquals(5.0, movedLine.line().start().x());
        assertEquals(5.0, movedLine.line().start().y());
    }

    @Test
    public void testCopySelected() {
        Document doc = new Document();
        LineSegment line = new LineSegment(new Point2D(0, 0), new Point2D(10, 0));
        LineElement elem = new LineElement(doc.getActiveLayer().getId(), line);

        doc.addElement(elem);
        doc.selectElement(elem.id(), false);

        doc.copySelected(10, 0);

        assertEquals(2, doc.getElements().size());
    }

    @Test
    public void testDeleteSelected() {
        Document doc = new Document();
        LineSegment line = new LineSegment(new Point2D(0, 0), new Point2D(10, 0));
        LineElement elem = new LineElement(doc.getActiveLayer().getId(), line);

        doc.addElement(elem);
        doc.selectElement(elem.id(), false);

        doc.deleteSelected();

        assertEquals(0, doc.getElements().size());
    }

    @Test
    public void testMirrorCloneSelected() {
        Document doc = new Document();
        LineSegment line = new LineSegment(new Point2D(0, 0), new Point2D(10, 0));
        LineElement elem = new LineElement(doc.getActiveLayer().getId(), line);

        doc.addElement(elem);
        doc.selectElement(elem.id(), false);

        doc.mirrorSelectedHorizontal();

        // Must preserve original and create 1 mirrored clone (total 2 elements)
        assertEquals(2, doc.getElements().size());
    }

    @Test
    public void testComponentChildElementsMovement() {
        Document doc = new Document();
        String layerId = doc.getActiveLayer().getId();

        List<CADElement> slotElements = CardSlotComponent.createCardSlot(layerId, new Point2D(10, 10), 95, 55);
        for (CADElement elem : slotElements) {
            doc.addElement(elem);
        }

        // Seleciona a moldura do bloco porta-cartão (CADGroup ou CADElement)
        CADElement blockElem = slotElements.get(0);
        doc.selectElement(blockElem.id(), false);

        // Mover o porta-cartão por (50, 50)
        doc.moveSelected(50, 50);

        // O bloco atômico deve mover com todos os seus sub-elementos internos
        assertEquals(1, doc.getElements().size());

        for (CADElement elem : doc.getElements()) {
            assertTrue(elem.boundingBox().center().x() >= 50.0);
            assertTrue(elem.boundingBox().center().y() >= 50.0);
        }
    }
}
