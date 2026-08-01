package com.leathercad.core.materials;

public record LeatherMaterial(
    String id,
    String name,
    double thicknessMm,
    double elasticityFactor,
    double shrinkagePercent,
    double foldFactor,
    double weightGsm,
    String colorHex,
    String manufacturer
) {
    public static LeatherMaterial DEFAULT_VEG_TAN_14 = new LeatherMaterial(
        "veg_tan_14",
        "Veg Tan 1.4 mm",
        1.4,
        0.02,
        1.5,
        1.2,
        850.0,
        "#8B4513",
        "Badalassi Carlo"
    );
}
