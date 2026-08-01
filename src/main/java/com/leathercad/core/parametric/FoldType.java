package com.leathercad.core.parametric;

/**
 * Tipos de Dobra de Artigos de Couro:
 * - SINGLE_FOLD: Dobra simples (Porta-cartão simples)
 * - BIFOLD: Dobra dupla (Carteira Bifold de 2 abas)
 * - TRIFOLD: Dobra tripla (Carteira Trifold de 3 abas)
 */
public enum FoldType {
    SINGLE_FOLD("Dobra Simples"),
    BIFOLD("Bifold (2 Abas)"),
    TRIFOLD("Trifold (3 Abas)");

    private final String description;

    FoldType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
