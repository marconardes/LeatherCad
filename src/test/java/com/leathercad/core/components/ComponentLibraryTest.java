package com.leathercad.core.components;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.model.CADAssembler;
import com.leathercad.core.model.CADElement;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ComponentLibraryTest {

    @Test
    public void testCardSlotComponent() {
        List<CADElement> elements = CardSlotComponent.createCardSlot("layer1", new Point2D(0, 0), 95.0, 55.0);

        assertNotNull(elements);
        assertEquals(1, elements.size()); // Bloco atômico CADGroup
        List<CADElement> flat = CADAssembler.flatten(elements);
        assertEquals(3, flat.size()); // Outer rect, thumb cut arc, bottom stitch
    }

    @Test
    public void testIDWindowComponent() {
        List<CADElement> elements = IDWindowComponent.createIDWindow("layer1", new Point2D(0, 0));

        assertNotNull(elements);
        assertEquals(1, elements.size()); // Bloco atômico CADGroup
        List<CADElement> flat = CADAssembler.flatten(elements);
        assertEquals(2, flat.size()); // Outer frame, inner cutout
    }

    @Test
    public void testHardwareComponent() {
        List<CADElement> snaps = HardwareComponent.createMagneticSnap("layer1", new Point2D(100, 100), 14.0);
        assertNotNull(snaps);
        assertTrue(snaps.size() >= 3);

        List<CADElement> studs = HardwareComponent.createPressStud("layer1", new Point2D(100, 100), 12.0);
        assertEquals(2, studs.size());

        List<CADElement> zippers = HardwareComponent.createZipper("layer1", new Point2D(0, 0), 150.0, 30.0);
        assertEquals(2, zippers.size());
    }
}
