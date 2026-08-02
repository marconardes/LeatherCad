package com.leathercad.core.leather;

/**
 * Tipos de Garfo de Costura / Chisel de Couro:
 * - FRENCH: Garfo francês (Pricking Iron 45° dentes finos inclinados)
 * - EUROPEAN: Garfo europeu (Diamond Chisel em losango)
 * - ROUND: Furo circular (Round Hole Punch)
 */
public enum StitchType {
    FRENCH_SLANT("Francesa / Diamante (45°)"),
    ROUND_PUNCH("Vazador Redondo"),
    MACHINE_STITCH("Linha de Máquina"),

    // Aliases legados para retrocompatibilidade
    FRENCH("Francesa / Diamante (45°)"),
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
