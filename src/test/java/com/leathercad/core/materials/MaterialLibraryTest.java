package com.leathercad.core.materials;

import com.leathercad.core.model.Layer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MaterialLibraryTest {

    @Test
    public void testDefaultMaterialsInLibrary() {
        MaterialLibrary library = new MaterialLibrary();

        assertTrue(library.getAllMaterials().size() >= 6);

        LeatherMaterial vegTan16 = library.getMaterial("veg_tan_16");
        assertNotNull(vegTan16);
        assertEquals(1.6, vegTan16.thicknessMm(), 1e-4);
        assertEquals("#8B4513", vegTan16.colorHex());
    }

    @Test
    public void testLayerMaterialAssignment() {
        Layer layer = new Layer("Corpo Principal", "#00FF88", 1);
        LeatherMaterial crazyHorse = new LeatherMaterial("crazy_18", "Crazy Horse 1.8 mm", 1.8, 0.03, 1.2, 1.2, 1050.0, "#4A2511", "Artisan");

        layer.setMaterial(crazyHorse);

        assertEquals("Crazy Horse 1.8 mm", layer.getMaterial().name());
        assertEquals(1.8, layer.getMaterial().thicknessMm(), 1e-4);
    }
}
