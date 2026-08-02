package com.leathercad.core.leather;

/**
 * Configuração parametrizada de costura de couro.
 */
public record StitchConfig(
    double marginMm,        // Distância da borda (ex: 3.0mm, 3.85mm, 4.0mm)
    double pitchMm,         // Espaçamento nominal entre dentes (ex: 2.70, 3.00, 3.38, 3.85, 4.00, 5.00mm)
    double holeDiameterMm,  // Diâmetro para ROUND_PUNCH (ex: 1.2mm)
    double slotLengthMm,    // Comprimento da fenda inclinada para FRENCH_SLANT (ex: 2.0mm)
    double angleDegrees,    // Inclinação da fenda (+45.0° ou -45.0° relativa à tangente)
    StitchType type         // Tipo de costura/garfo
) {
    public StitchConfig(double marginMm, double pitchMm, double holeDiameterMm, double angleDegrees, StitchType type) {
        this(marginMm, pitchMm, holeDiameterMm, 2.0, angleDegrees, type);
    }

    public static final StitchConfig DEFAULT = new StitchConfig(3.85, 3.85, 1.2, 2.0, 45.0, StitchType.FRENCH_SLANT);
    public static final StitchConfig DEFAULT_FRENCH = DEFAULT;
    public static final StitchConfig DEFAULT_EUROPEAN = new StitchConfig(3.0, 4.0, 1.2, 2.0, 45.0, StitchType.FRENCH_SLANT);
    public static final StitchConfig DEFAULT_ROUND = new StitchConfig(4.0, 5.0, 1.2, 0.0, 0.0, StitchType.ROUND_PUNCH);
    public static final StitchConfig DEFAULT_MACHINE = new StitchConfig(3.85, 3.85, 1.0, 0.0, 0.0, StitchType.MACHINE_STITCH);
}
