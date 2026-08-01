package com.leathercad.core.model;

/**
 * Referência topológica a um sub-elemento de uma primitiva (Aresta ou Vértice), inspirada no FreeCAD.
 */
public record SubElementRef(String elementId, SubElementType type, int index) {

    public enum SubElementType {
        EDGE,
        VERTEX
    }

    public String key() {
        return elementId + ":" + type.name().toLowerCase() + ":" + index;
    }
}
