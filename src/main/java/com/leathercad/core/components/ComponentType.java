package com.leathercad.core.components;

/**
 * Tipos de Componentes Reutilizáveis de Couro e Ferragens:
 * - CARD_SLOT: Porta-cartão com corte T-slot / polegar
 * - ID_WINDOW: Janela transparente para documentos
 * - COIN_POCKET: Porta-moedas com fole e aba
 * - MAGNETIC_SNAP: Fecho magnético de metal (14mm / 18mm)
 * - PRESS_STUD: Botão de pressão (10mm / 12mm / 15mm)
 * - ZIPPER: Zíper técnico parametrizado
 * - ELASTIC: Tira de elástico
 */
public enum ComponentType {
    CARD_SLOT("Porta-Cartão"),
    ID_WINDOW("Janela Transparente ID"),
    COIN_POCKET("Porta-Moedas com Lapela"),
    MAGNETIC_SNAP("Fecho Magnético"),
    PRESS_STUD("Botão de Pressão"),
    ZIPPER("Zíper Parametrizado"),
    ELASTIC("Tira de Elástico");

    private final String label;

    ComponentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
