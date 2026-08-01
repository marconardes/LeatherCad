package com.leathercad.core.nesting;

public record HideSheet(String name, double widthMm, double heightMm, double spacingMm) {

    public static final HideSheet FULL_HIDE_1000x800 = new HideSheet("Pele Inteira de Couro (1000 x 800 mm)", 1000.0, 800.0, 5.0);
    public static final HideSheet SHEET_A3 = new HideSheet("Placa / Folha A3 (420 x 297 mm)", 420.0, 297.0, 5.0);
    public static final HideSheet SHEET_A4 = new HideSheet("Placa / Retalho A4 (297 x 210 mm)", 297.0, 210.0, 4.0);

    public double areaCm2() {
        return (widthMm * heightMm) / 100.0;
    }
}
