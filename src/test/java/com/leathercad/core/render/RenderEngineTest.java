package com.leathercad.core.render;

import com.leathercad.core.materials.LeatherMaterial;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RenderEngineTest {

    @Test
    public void testMetalFinishes() {
        RealisticHardwareRenderer.MetalFinish brass = RealisticHardwareRenderer.MetalFinish.BRASS;
        assertNotNull(brass);
        assertEquals("Latão Polido", brass.getLabel());

        RealisticHardwareRenderer.MetalFinish silver = RealisticHardwareRenderer.MetalFinish.SILVER;
        assertEquals("Prata Inox", silver.getLabel());
    }

    @Test
    public void testLeatherMaterialPropertiesForRender() {
        LeatherMaterial vegTan = LeatherMaterial.DEFAULT_VEG_TAN_14;
        assertNotNull(vegTan.colorHex());
        assertTrue(vegTan.colorHex().startsWith("#"));
    }
}
