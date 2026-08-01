package com.leathercad.core.leather;

/**
 * Configuração parametrizada de costura de couro.
 */
public record StitchConfig(
    double marginMm,        // Distância da borda (ex: 3.0mm, 3.85mm, 4.0mm)
    double pitchMm,         // Espaçamento entre furos/dentes (ex: 3.85mm, 4.0mm, 5.0mm)
    double holeDiameterMm,  // Diâmetro do furo/largura do dente (ex: 1.0mm)
    double angleDegrees,    // Inclinação do dente (ex: 45.0°)
    StitchType type         // Tipo de garfo
) {
    public static StitchConfig DEFAULT_FRENCH = new StitchConfig(3.85, 3.85, 1.0, 45.0, StitchType.FRENCH);
    public static StitchConfig DEFAULT_EUROPEAN = new StitchConfig(3.0, 4.0, 1.2, 45.0, StitchType.EUROPEAN);
    public static StitchConfig DEFAULT_ROUND = new StitchConfig(4.0, 5.0, 1.5, 0.0, StitchType.ROUND);
}
