package com.leathercad.core.model;

import java.util.Objects;
import java.util.UUID;

public class Layer {
    private final String id;
    private String name;
    private String colorHex;
    private boolean visible;
    private boolean locked;
    private int zIndex;

    public Layer(String name, String colorHex, int zIndex) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.colorHex = colorHex;
        this.visible = true;
        this.locked = false;
        this.zIndex = zIndex;
    }

    private com.leathercad.core.materials.LeatherMaterial material = com.leathercad.core.materials.MaterialLibrary.DEFAULT_MATERIAL;

    public com.leathercad.core.materials.LeatherMaterial getMaterial() { return material; }
    public void setMaterial(com.leathercad.core.materials.LeatherMaterial material) { this.material = material; }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public int getZIndex() { return zIndex; }
    public void setZIndex(int zIndex) { this.zIndex = zIndex; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Layer layer = (Layer) o;
        return Objects.equals(id, layer.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
