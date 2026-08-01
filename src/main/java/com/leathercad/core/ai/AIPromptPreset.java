package com.leathercad.core.ai;

public record AIPromptPreset(String title, String prompt) {
    public static final AIPromptPreset BIFOLD_6_CARDS = new AIPromptPreset(
        "💳 Carteira Bifold 6 Cartões + Notas",
        "Crie uma carteira bifold de couro para 6 cartões com compartimento principal de notas e margem de costura de 3.85mm."
    );

    public static final AIPromptPreset MINIMALIST_CARDHOLDER = new AIPromptPreset(
        "🪪 Porta-Cartões Minimalista 3 Slots",
        "Crie um porta-cartões minimalist de couro com 3 divisórias verticais, recorte de polegar e furação de costura perimetral."
    );

    public static final AIPromptPreset PASSPORT_COVER = new AIPromptPreset(
        "✈️ Capa de Passaporte + Janela ID",
        "Crie uma capa de passaporte de viagem dobrável com bolso transparente ID e aba interna de encaixe."
    );

    public static final AIPromptPreset COIN_WALLET = new AIPromptPreset(
        "👛 Carteira Porta-Moedas + Fecho Magnético",
        "Crie um porta-moedas de couro com lapela de fechamento, fecho magnético de 14mm e vincos laterais."
    );
}
