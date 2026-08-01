package com.leathercad.core.parametric;

/**
 * Tabela de Variáveis de Projeto Paramétrico de Couro.
 */
public record ProjectVariables(
    double widthMm,             // Largura aberta da carteira (ex: 215.0mm para notas de real/dólar)
    double heightMm,            // Altura (ex: 95.0mm)
    double leatherThicknessMm,  // Espessura do couro (ex: 1.4mm)
    int cardSlotsCount,         // Número de cartões (ex: 6 cartões)
    FoldType foldType,          // Tipo de dobra
    double marginMm,            // Margem de costura (ex: 3.85mm)
    double stitchPitchMm        // Espaçamento de garfo (ex: 3.85mm)
) {
    public static ProjectVariables DEFAULT_BIFOLD = new ProjectVariables(
        215.0, 95.0, 1.4, 6, FoldType.BIFOLD, 3.85, 3.85
    );

    public static ProjectVariables DEFAULT_CARD_HOLDER = new ProjectVariables(
        100.0, 70.0, 1.2, 4, FoldType.SINGLE_FOLD, 3.0, 3.85
    );
}
