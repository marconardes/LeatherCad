package com.leathercad.core.materials;

import java.util.*;

public class MaterialLibrary {

    private final Map<String, LeatherMaterial> materials = new LinkedHashMap<>();

    public MaterialLibrary() {
        addDefaultMaterials();
    }

    private void addDefaultMaterials() {
        addMaterial(new LeatherMaterial("veg_tan_12", "Veg Tan 1.2 mm", 1.2, 0.02, 1.5, 1.2, 750.0, "#C8A2C8", "Badalassi Carlo"));
        addMaterial(new LeatherMaterial("veg_tan_16", "Veg Tan 1.6 mm", 1.6, 0.02, 1.5, 1.2, 950.0, "#8B4513", "Wickett & Craig"));
        addMaterial(new LeatherMaterial("cromo_14", "Couro Cromo 1.4 mm", 1.4, 0.05, 0.5, 1.1, 800.0, "#2B2B2B", "Tannery Cromo"));
        addMaterial(new LeatherMaterial("pull_up_15", "Pull-Up 1.5 mm", 1.5, 0.04, 1.0, 1.15, 880.0, "#5C4033", "Horween Chromexcel"));
        addMaterial(new LeatherMaterial("crazy_horse_18", "Crazy Horse 1.8 mm", 1.8, 0.03, 1.2, 1.2, 1050.0, "#4A2511", "Artisan Leather"));
        addMaterial(new LeatherMaterial("suede_10", "Camurça / Suede 1.0 mm", 1.0, 0.08, 2.0, 1.05, 550.0, "#D2B48C", "Soft Touch Works"));
    }

    public void addMaterial(LeatherMaterial mat) {
        materials.put(mat.id(), mat);
    }

    public LeatherMaterial getMaterial(String id) {
        return materials.get(id);
    }

    public Collection<LeatherMaterial> getAllMaterials() {
        return materials.values();
    }

    public static LeatherMaterial DEFAULT_MATERIAL = LeatherMaterial.DEFAULT_VEG_TAN_14;
}
