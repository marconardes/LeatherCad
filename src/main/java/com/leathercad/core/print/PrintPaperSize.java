package com.leathercad.core.print;

public record PrintPaperSize(
    String name,
    double widthMm,
    double heightMm,
    boolean isLandscape
) {
    public static final PrintPaperSize A4 = new PrintPaperSize("Folha A4", 210.0, 297.0, false);
    public static final PrintPaperSize A3 = new PrintPaperSize("Folha A3", 297.0, 420.0, false);
    public static final PrintPaperSize LETTER = new PrintPaperSize("Carta / Letter", 215.9, 279.4, false);
    public static final PrintPaperSize A2 = new PrintPaperSize("Folha A2", 420.0, 594.0, false);
    public static final PrintPaperSize A1 = new PrintPaperSize("Folha A1", 594.0, 841.0, false);
    public static final PrintPaperSize PLOTTER = new PrintPaperSize("Plotter / Rolo (900mm)", 900.0, 1200.0, false);

    public double currentWidth() {
        return isLandscape ? Math.max(widthMm, heightMm) : Math.min(widthMm, heightMm);
    }

    public double currentHeight() {
        return isLandscape ? Math.min(widthMm, heightMm) : Math.max(widthMm, heightMm);
    }

    public PrintPaperSize withOrientation(boolean landscape) {
        return new PrintPaperSize(name, widthMm, heightMm, landscape);
    }

    public double printableWidth(double marginMm, double overlapMm) {
        return Math.max(10.0, currentWidth() - (marginMm * 2) - overlapMm);
    }

    public double printableHeight(double marginMm, double overlapMm) {
        return Math.max(10.0, currentHeight() - (marginMm * 2) - overlapMm);
    }
}
