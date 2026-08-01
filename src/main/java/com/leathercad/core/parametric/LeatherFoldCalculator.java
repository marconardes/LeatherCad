package com.leathercad.core.parametric;

public class LeatherFoldCalculator {

    /**
     * Calcula a folga de dobra externa para que o corpo externo cubra o corpo interno sem franzir o couro.
     */
    public static double calculateFoldAllowance(double leatherThicknessMm) {
        // Fórmula técnica de marcenaria de couro: π * espessura * 1.2
        return Math.PI * leatherThicknessMm * 1.2;
    }

    /**
     * Calcula a folga necessária para empilhamento de cartões de crédito.
     */
    public static double calculateCardStackAllowance(int cardCount) {
        return cardCount * 0.75;
    }

    /**
     * Calcula a largura total do corpo externo desdobrado de uma carteira bifold.
     */
    public static double calculateOuterShellWidth(double closedWidthMm, double leatherThicknessMm) {
        double innerUnfolded = closedWidthMm * 2.0;
        return innerUnfolded + calculateFoldAllowance(leatherThicknessMm);
    }
}
