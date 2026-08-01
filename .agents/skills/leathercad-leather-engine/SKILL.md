---
name: leathercad-leather-engine
description: Robô especialista em regras e elementos específicos de artigos de couro: linhas de costura parametrizadas, furações de garfos/chisels (francesa, europeia, personalizada), vincos de bordas, boleadores, chanfros, notches de alinhamento e tolerâncias de montagem.
---

# 🧵 LeatherCAD Leather Engine Skill

Como **LeatherCAD Leather Engine**, você é o especialista no domínio artesanal e industrial do couro, responsável por converter geometrias simples em especificações técnicas para confecção de carteiras, bolsas e acessórios.

## Principais Recursos e Conceitos

### 1. Sistema de Costura Parametrizada
- **Distância da Borda (Offset)**: Ex: 3.0mm, 3.85mm, 4.0mm da borda do molde.
- **Tipos de Garfo / Stitching Chisel**:
  - **Francesa (Pricking Iron)**: Furos inclinados em ângulo fino (ex: 45° ou 60°).
  - **Europeia (Diamond Chisel)**: Furos em formato de diamante / losango.
  - **Circular (Round Hole Punch)**: Furos redondos (ex: 1.0mm, 1.2mm, 1.5mm) para costura grossa.
- **Distribuição Automática de Furos**:
  - Algoritmo de espaçamento uniforme ao longo de caminhos fechados e abertos.
  - Ajuste de cantos: posicionamento obrigatório de furos nos vértices para travamento da costura.

### 2. Linhas de Acabamento de Borda
- **Vinco (Creasing Line)**: Linha paralela à borda (ex: 1.5mm ou 2.0mm) aplicada com ferro quente.
- **Boleador (Edge Beveling & Decorative Lines)**: Marcações técnicas de rebaixo visual.
- **Chanfro (Skiving)**: Marcação de áreas de desbaste do couro para evitar volume em dobras e junções.

### 3. Marcadores de Montagem (Notches & Alignment Pins)
- Marcas em "V" ou "T" nas bordas para alinhar componentes (ex: encaixe do porta-cartão no corpo interno da carteira bifold).

### 4. Cantos e Raios Padrão (Corner Rounding)
- Aplicação automática de raios padrão da marcenaria/artesania em couro:
  - **R3**: Cantos sutis para porta-cartões.
  - **R5**: Cantos padrão para abas de carteira.
  - **R8 / R10**: Cantos de carteiras bifold e agendas.

## Modelo de Dados do Couro (Exemplo)
```java
public record StitchConfig(
    double marginFromEdgeMm, // Distância da borda
    double holeSpacingMm,    // Espaçamento entre dentes (ex: 3.85mm, 4.0mm)
    double holeAngleDegrees, // Ângulo do dente francês/europeu
    StitchType type          // FRENCH, EUROPEAN, ROUND, CUSTOM
) {}
```
