package com.leathercad.core.model;

import com.leathercad.core.components.CardSlotComponent;
import com.leathercad.core.geometry.Point2D;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CADAssemblerTest {

    @Test
    public void testCADGroupCreationAndAtomicTransform() {
        Document doc = new Document();
        String layerId = doc.getActiveLayer().getId();

        List<CADElement> slotBlock = CardSlotComponent.createCardSlot(layerId, new Point2D(10, 10), 95, 45);
        assertEquals(1, slotBlock.size());
        assertTrue(slotBlock.getFirst() instanceof CADGroup);

        CADGroup group = (CADGroup) slotBlock.getFirst();
        assertEquals(3, group.children().size()); // Rect + Thumb Arc + Stitch

        // Translação atômica do bloco inteiro
        CADElement moved = group.translate(50, 50);
        assertTrue(moved instanceof CADGroup);

        CADGroup movedGroup = (CADGroup) moved;
        assertEquals(3, movedGroup.children().size());
        assertTrue(movedGroup.boundingBox().minPoint().x() > 10.0);
        assertTrue(movedGroup.boundingBox().minPoint().y() > 10.0);
    }

    @Test
    public void testCADAssemblerFlatten() {
        Document doc = new Document();
        String layerId = doc.getActiveLayer().getId();

        List<CADElement> slotBlock = CardSlotComponent.createCardSlot(layerId, new Point2D(10, 10), 95, 45);
        List<CADElement> flat = CADAssembler.flatten(slotBlock);

        assertEquals(3, flat.size());
    }
}
