# LeatherCAD - Roadmap de Desenvolvimento

## Visão Geral

O **LeatherCAD** é um software CAD especializado no desenvolvimento de carteiras e artigos de couro. O objetivo é oferecer uma plataforma completa para desenho técnico, parametrização, simulação e preparação para produção, reduzindo o tempo de criação de moldes e aumentando a precisão.

---

# Fase 1 – MVP (CONCLUÍDO - 100%)

## Objetivo

Desenvolver uma aplicação funcional capaz de criar moldes técnicos para artigos de couro.

## Funcionalidades

### Área de desenho

- [x] Grid em milímetros
- [x] Zoom (Scroll wheel centrado no cursor)
- [x] Pan (Botão do meio / Drag)
- [x] Régua (Superiores e laterais em mm)
- [x] Snap em pontos (Endpoint, Midpoint, Centro, Grid com indicador verde neon)

### Ferramentas de desenho

- [x] Linha
- [x] Retângulo
- [x] Círculo
- [x] Arco
- [x] Curvas Bézier

### Ferramentas de edição

- [x] Seleção por clique e caixa
- [x] Mover (com vetor de deslocamento e arraste direto)
- [x] Copiar (duplicação de elementos com novo ID)
- [x] Rotacionar (em torno do centro da seleção)
- [x] Espelhar (clonagem espelhada simétrica)
- [x] Escalar (fator de escala relativo)

### Camadas

- [x] Criação
- [x] Organização (Z-Index)
- [x] Ocultar/Mostrar (Visibilidade)
- [x] Bloquear (Proteção contra edição e seleção 🔒/🔓)

### Cotas

- [x] Horizontal
- [x] Vertical
- [x] Angular
- [x] Raio
- [x] Diâmetro

### Exportação

- [x] SVG (Vetor limpo escala 1:1)
- [x] DXF (AutoCAD R12 em mm para corte laser/CNC)
- [x] PDF (Documento técnico de impressão)
- [x] PNG (Renderização de imagem em alta definição)

---

# Fase 2 – Ferramentas específicas para couro (CONCLUÍDO - 100%)

Esta fase adiciona funcionalidades voltadas exclusivamente para a produção artesanal e industrial em couro.

## Costura

Configurações:

- [x] Distância da borda (Margin/Offset)
- [x] Espaçamento entre furos (Pitch 3.85mm, 4.0mm, 5.0mm)
- [x] Diâmetro do furo
- [x] Número de furos
- [x] Tipo de costura
    - [x] Francesa (Pricking Iron 45°)
    - [x] Europeia (Diamond Chisel)
    - [x] Furo Redondo (Round Punch)

Visualização automática dos pontos/furos no Canvas e exportação 1:1.

---

## Vinco

- [x] Permitir criar linhas de vinco parametrizadas (`CreaseElement`).

Configurações:

- [x] Distância da borda
- [x] Largura visual
- [x] Profundidade visual em tom laranja aquecido (`#FF8C00`)

---

## Boleador

- [x] Criar linhas decorativas para acabamento nas bordas.

---

## Chanfro

- [x] Marcação das bordas que receberão acabamento e desbaste (`SkivingElement`).

---

## Arredondamento

- [x] Adicionar raios automaticamente em cantos de moldes.

Exemplos:

- [x] R3
- [x] R5
- [x] R8
- [x] R10

---

## Notches

- [x] Adicionar marcas de alinhamento em V/T para montagem entre peças (`NotchElement`).

---

# Fase 3 – Biblioteca de Componentes (CONCLUÍDO - 100%)

Criar componentes reutilizáveis e ferragens parametrizadas.

## Componentes Disponíveis:

- [x] Porta-cartão (moldura ISO 95x55mm, T-slot / thumb cut e costuras `CardSlotComponent`)
- [x] Janela transparente ID (moldura 100x70mm para documentos `IDWindowComponent`)
- [x] Compartimento oculto
- [x] Porta-moedas com fole e lapela (`CoinPocketComponent`)
- [x] Fecho magnético (14mm / 18mm com slots de metal `HardwareComponent`)
- [x] Elástico
- [x] Botão de pressão (10mm / 12mm / 15mm)
- [x] Zíper (comprimento parametrizado L mm)

Todos parametrizados em mm e prontos para inserção com 1-click no Canvas.

---

# Fase 4 – Parametrização (CONCLUÍDO - 100%)

Transformar desenhos em projetos inteligentes com recálculo automático de folgas de couro.

## Parâmetros Suportados:

- [x] Largura (`widthMm`)
- [x] Altura (`heightMm`)
- [x] Número de cartões (`cardSlotsCount`)
- [x] Espessura do couro (`leatherThicknessMm`, ex: 1.4mm)
- [x] Tipo de dobra (`FoldType`: Single fold, Bifold, Trifold)
- [x] Margem de costura (`marginMm`, ex: 3.85mm)
- [x] Tipo de costura (`StitchConfig`)

### Gerador Inteligente 1-Click (`BifoldWalletTemplate`):

Entrada do Usuário:
- [x] Carteira Bifold
- [x] 6 cartões
- [x] 2 compartimentos ocultos
- [x] Couro de 1,4 mm
- [x] Margem de 3.85 mm

Saída Técnica Automática:
- [x] Todas as peças geradas automaticamente em escala 1:1 com cálculo de folga de dobra externa (`π * t * 1.2`), corpo interno, porta-cartões em degrau, vincos e costura perimetral.

---

# Fase 5 – Motor Paramétrico (CONCLUÍDO - 100%)

Sistema de CAD paramétrico reativo no estilo Fusion 360 / SolidWorks.

## Funcionalidades Implementadas:

- [x] Avaliador de expressões matemáticas (`FormulaEngine`: `+`, `-`, `*`, `/`, `(`, `)`, `PI`, `sqrt`)
- [x] Tabela de variáveis globais de projeto (`VariableTable`)
- [x] Recálculo reativo em cascata ao alterar qualquer valor
- [x] Painel de Variáveis lateral em JavaFX (`VariableTablePanel`) na aba `📐 Variáveis (Fusion 360)`
- [x] Fórmulas técnicas encadeadas (ex: `largura_total = card_width + (2 * margin) + (2 * leather_thickness)`)

---

# Fase 6 – Biblioteca de Materiais (CONCLUÍDO - 100%)

Cadastro técnico e gerenciamento de couros e materiais de marcenaria.

## Materiais Pré-Cadastrados (`MaterialLibrary`):

- [x] Veg Tan 1.2 mm (#C8A2C8, Badalassi Carlo)
- [x] Veg Tan 1.6 mm (#8B4513, Wickett & Craig)
- [x] Couro Cromo 1.4 mm (#2B2B2B, Tannery Cromo)
- [x] Pull-up 1.5 mm (#5C4033, Horween Chromexcel)
- [x] Crazy Horse 1.8 mm (#4A2511, Artisan Leather)
- [x] Camurça / Suede 1.0 mm (#D2B48C, Soft Touch Works)

## Propriedades Físicas e Ficha Técnica (`LeatherMaterial`):

- [x] Espessura (`thicknessMm`)
- [x] Elasticidade (`elasticityFactor`)
- [x] Encolhimento (`shrinkagePercent`)
- [x] Fator de dobra (`foldFactor`)
- [x] Peso / Gramatura (`weightGsm` em g/m²)
- [x] Cor Hexadecimal (`colorHex`) com mostrador visual (color swatch)
- [x] Fabricante (`manufacturer`)
- [x] Atribuição de material por camada (`Layer.setMaterial()`)

---

# Fase 7 – Simulação (CONCLUÍDO - 100%)

Simulação técnica de montagem 2.5D/3D e diagnóstico físico de artigos de couro.

## Recursos de Simulação Implementados:

- [x] Simulação de dobras com projeção isométrica 2.5D animada de 0° a 180° (`FoldSimulator`)
- [x] Análise de acúmulo de espessura de camadas empilhadas (`LayerStackAnalyzer`)
- [x] Detecção de interferências e alerta de rebaixamento de bordas (skiving) (`CollisionDetector`)
- [x] Cálculo de área ($cm^2$), volume total ($cm^3$) e peso físico estimado ($g$) (`VolumeCalculator`)
- [x] Interface de visualização em JavaFX com controle deslizante de dobras (`SimulationDialog`)

---

# Fase 8 – Renderização (CONCLUÍDO - 100%)

Renderização fotorrealista em alta fidelidade técnica.

## Recursos de Renderização Implementados:

- [x] Textura procedural de grão e porosidade do couro (`LeatherTextureGenerator`)
- [x] Costuras 3D fotorrealistas com fio encerado e furações profundas (`RealisticStitchRenderer`)
- [x] Acabamento de bordas com chanfro e efeito polido *burnished* (`EdgeFinishRenderer`)
- [x] Ferragens metálicas fotorrealistas (latão, prata, gunmetal) (`RealisticHardwareRenderer`)
- [x] Botões de pressão, fechos magnéticos e zíperes metálicos
- [x] Sombras projetadas (*drop shadows*) e profundidade entre camadas
- [x] Estúdio de renderização fotorrealista JavaFX (`RealisticRenderDialog`) com exportação PNG HD

---

# Fase 9 – Corte Inteligente (Nesting) (CONCLUÍDO - 100%)

Otimização de plano de corte e disposição automática na chapa de couro.

## Recursos de Nesting Implementados:

- [x] Modelo de pele/chapa de couro e formatos predefinidos (`HideSheet`: Pele 1000x800mm, A3, A4)
- [x] Algoritmo 2D Bin Packing MaxRects / Bottom-Left com rotação automática (0° e 90°) (`NestingOptimizer`)
- [x] Agrupamento automático de peças por material de camada
- [x] Cálculo em tempo real de aproveitamento (Yield %), área usada ($cm^2$) e desperdício ($cm^2$) (`NestingResult`)
- [x] Interface de estúdio JavaFX (`NestingDialog`) com visualização de corte e botão "✂️ Aplicar Nesting ao Canvas"

---

# Fase 10 – Produção (CONCLUÍDO - 100%)

Gerador automático de Ficha Técnica (BOM) e estatísticas de insumos de fabricação.

## Recursos de Produção Implementados:

- [x] Lista estruturada de peças com dimensões ($W \times H$ mm) e quantidades (`BOMItem`)
- [x] Consumo total de couro em área plana ($cm^2$) e pés quadrados ($ft^2$) (`ProductionCalculator`)
- [x] Consumo acumulado de linha de costura em metros ($m$) com margem de arremate
- [x] Contagem automatizada de furações de chisel e ferragens (botões, fechos)
- [x] Estimativa de tempo total de corte (minutos)
- [x] Estúdio de Ficha Técnica JavaFX (`ProductionReportDialog`) com exportação de relatório TXT/PDF

---

# Fase 11 – Impressão (CONCLUÍDO - 100%)

Sistema avançado de impressão em escala real 1:1, paginação automática (*tiling*), marcas de corte e sobreposição de junção para corte manual na bancada de couro.

## Recursos de Impressão Implementados:

- [x] Presets de folha e papel (`A4`, `A3`, `LETTER`, `A2`, `A1`, `PLOTTER`) em retrato ou paisagem (`PrintPaperSize`)
- [x] Paginação automática (*Tiling*) para projetos maiores que a folha física (`PrintEngine` e `PrintTile`)
- [x] Margem de sobreposição de segurança ajustável (ex: 10mm) com linha guia de colagem
- [x] Marcas de corte (*crop marks*) e mira de alinhamento nos cantos das folhas
- [x] Estúdio de Impressão JavaFX (`PrintStudioDialog`) com preview de grade de páginas e envio para impressora (`PrinterJob`)
- [x] Exportador de relatório e gabarito multi-páginas 1:1 (`PrintPDFExporter`)

---

# Fase 12 – Inteligência Artificial (CONCLUÍDO - 100%)

Gerador automático de moldes 2D parametrizados por linguagem natural com suporte ao Ollama Local (`http://localhost:11434`).

## Recursos de IA Implementados:

- [x] Conector HTTP com Ollama Local (`OllamaClient`) e suporte aos modelos locais instalados (`qwen2.5:1.5b`, `gemma3:4b`, `gemma4:e2b`, `gemma3:270m`)
- [x] Sintetizador CAD de IA (`AIDesignGenerator`) para tradução de prompts em geometria completa (peças, cotas, costuras, vincos e ferragens)
- [x] Presets de prompts rápidos (`AIPromptPreset`): Bifold 6 cartões, Porta-cartões 3 slots, Capa de Passaporte, Porta-Moedas com lapela
- [x] Estúdio de Assistente de IA JavaFX (`AIAssistantDialog`) com indicador de conexão, seleção de modelos e log de execução
- [x] Motor de IA preditivo embutido de fallback para execução offline ilimitada

---

# Fase 13 – Manuais de Montagem (CONCLUÍDO - 100%)

Geração automática de manuais de instrução passo a passo e fichas técnicas de montagem em PDF.

Funcionalidades:

- [x] Sequência automática de etapas de montagem (`AssemblyManualEngine`: Preparação -> Skiving -> Vincos -> Colagem -> Furação -> Costura -> Ferragens -> Borda)
- [x] Tabela mestre de ferramentas e insumos necessários por etapa (chisels 3.85mm, agulhas cegas, fio encerado 0.6mm, cola, Tokonole, tintas)
- [x] Estúdio JavaFX de Manual de Montagem (`AssemblyManualDialog`) no menu `Produção`
- [x] Exportação de relatório e gabarito técnico de oficina (`AssemblyManualPDFExporter`)

---

# Fase 14 – Auditoria Global, QA & Varredura de Bugs (CONCLUÍDO - 100%)

Revisão completa, refatoração de código e varredura integral de bugs em todo o ecossistema do LeatherCAD.

Funcionalidades:

- [x] Auditoria e correção de precisão no motor geométrico (`SnapEngine`: Snap de Interseção real 2D, Endpoints, Midpoints e Bounding Boxes)
- [x] Validação técnica do motor de couro e paramétrico (`StitchElement`, `LeatherFoldCalculator`: Compensação $1.2 \pi t$, Garfo Francês 45°, 3.85mm)
- [x] Revisão geral de UI JavaFX (Viewport Canvas, Renderização Fotorrealista, Painel dinâmico de Camadas com travas `🔒` e visibilidade `👁`, Toolbars compactas)
- [x] Teste de fidelidade 1:1 de exportação e impressão (`DXFExporter`, `SVGExporter`, `PDFExporter` e `PNGExporter`)
- [x] Bateria completa de 54 testes automatizados unitários + regressão + auditoria de estabilidade (`AuditQATest` 100% verde)

---

# Fase 15 – Arquitetura de Sub-Elementos B-Rep & Nomeação Topológica (Inspirada no FreeCAD) (EM EXECUÇÃO)

Migração do modelo de seleção e geometria do LeatherCAD para uma arquitetura topológica B-Rep inspirada no FreeCAD / OpenCASCADE. Todos os elementos geométricos e de couro (`RectElement`, `PolylineElement`, `BezierElement`, `CircleElement`, `StitchElement`, `CreaseElement`) passam a expor sub-elementos nomeados (Arestas `e0..N` e Vértices `v0..N`).

## Funcionalidades:

- [ ] **Nomeação Topológica de Sub-Elementos (`SubElementRef`)**: Referências unívocas para sub-arestas (`e0..N`) e sub-vértices (`v0..N`) em todas as primitivas geométricas e de couro.
- [ ] **Seleção Fina de Sub-Arestas no Canvas**: Seleção de 1 aresta ou 2 arestas com `Ctrl`/`Shift` em qualquer primitiva mantendo a peça de couro preenchida e unificada.
- [ ] **Destaque Visual Sub-Elemento Estilo CAD**: Renderização de halo de brilho Rosa Magenta (`#FF0055`) de 7px e traço de 3px especificamente sobre a sub-aresta/nó selecionado.
- [ ] **Operação Direct-on-Shape Fillet (FreeCAD Style)**: Aplicação de raio de canto (*Fillet*) em cantos/arestas de peças unificadas de qualquer formato sem precisar decompor/explodir a forma em retas soltas.
- [ ] **Preservação Integral de Propriedades de Material**: Garante que o preenchimento translúcido de couro, massa e área física sejam 100% preservados em todas as primitivas.

---

# Arquitetura

```text
LeatherCAD
│
├── Core
│
├── Geometry Engine
│
├── Sketch Engine
│
├── Parametric Engine
│
├── Leather Engine
│
├── Stitch Engine
│
├── Nesting Engine
│
├── Rendering Engine
│
├── AI Engine
│
├── Export Engine
│
├── Plugin System
│
└── User Interface
```

---

# Tecnologias Sugeridas

## Backend / Core

- Java 21
- OpenCascade
- Apache Commons Math

## Interface

- JavaFX
- Skia
- SVG

## Persistência

- SQLite
- JSON

## Inteligência Artificial

- Python
- FastAPI
- LLM Local
- Ollama
- OpenAI API (opcional)

---

# Roadmap de Versões

| Versão | Objetivo |
|---------|----------|
| 0.1 | Área de desenho 2D |
| 0.2 | Ferramentas de edição e exportação |
| 0.3 | Ferramentas específicas para couro |
| 0.4 | Biblioteca de componentes |
| 0.5 | Sistema paramétrico |
| 0.6 | Geração automática de carteiras |
| 0.7 | Biblioteca de materiais |
| 0.8 | Corte inteligente (Nesting) |
| 0.9 | Renderização |
| 1.0 | Inteligência Artificial |
| 2.0 | Marketplace e sistema de plugins |

---

# Objetivo Final

O LeatherCAD pretende se tornar a principal plataforma CAD para criação de artigos de couro, oferecendo uma solução especializada que combina:

- Desenho técnico
- Parametrização
- Automação de moldes
- Simulação de montagem
- Otimização de corte
- Renderização
- Inteligência Artificial
- Marketplace

A proposta é atender desde artesãos independentes até pequenas e médias fábricas, oferecendo um fluxo de trabalho completo, do conceito à produção.