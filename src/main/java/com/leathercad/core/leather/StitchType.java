package com.leathercad.core.leather;

/**
 * Tipos de Garfo de Costura / Chisel de Couro:
 * - FRENCH: Garfo francês (Pricking Iron 45° dentes finos inclinados)
 * - EUROPEAN: Garfo europeu (Diamond Chisel em losango)
 * - ROUND: Furo circular (Round Hole Punch)
 */
public enum StitchType {
    FRENCH("Francesa (Pricking Iron 45°)"),
    EUROPEAN("Europeia (Diamond Chisel)"),
    ROUND("Furo Redondo (Round Punch)");

    private final String description;

    StitchType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
