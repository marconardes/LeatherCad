# 🧰 LeatherCAD

> **Software CAD de Alta Precisão para Modelagem e Confecção de Artigos de Couro (Carteiras, Bolsas e Acessórios)**.

O **LeatherCAD** é uma plataforma CAD 2D especializada no desenvolvimento técnico de produtos de couro. Ela oferece ferramentas de desenho geométrico em escala milimétrica, sistema de costuras parametrizadas, vincos, marcadores de montagem e exportação para máquinas de corte vetorial (laser/CNC).

---

## 🛠️ Stack Tecnológica

- **Linguagem**: Java 21 LTS (com suporte a records imutáveis e pattern matching).
- **Interface Gráfica**: JavaFX 21 (Canvas 2D com aceleramento gráfico).
- **Build System**: Apache Maven.
- **Estrutura**: Clean Architecture / DDD (separação entre o Core Geométrico e a UI JavaFX).

---

## 🚀 Como Executar

### Pré-requisitos
- **Java 21** ou superior (`java -version`).
- **Maven 3.8+** (`mvn -version`).

### Comandos Principais

1. **Compilar o Projeto**:
   ```bash
   mvn clean compile
   ```

2. **Executar os Testes Unitários**:
   ```bash
   mvn test
   ```

3. **Abrir a Aplicação Gráfica (CAD Viewport 2D)**:
   ```bash
   mvn javafx:run
   ```

---

## 📐 Funcionalidades da Fase 1 (MVP - 100% Concluído)

- [x] **Área de Desenho 2D**:
  - Canvas com tema escuro CAD (`#1E1E1E`).
  - Grade milimétrica responsiva (1mm secundária e 10mm principal).
  - Controle de Câmera: **Zoom** focado no cursor e **Pan** (arraste com botão do meio).
  - Réguas dinâmicas superiores e laterais em mm.
  - **SnapEngine**: Atração automática por Endpoints, Midpoints, Centro e Grid com indicador gráfico em verde neon (`#00FF88`).
- [x] **Primitivas de Desenho**:
  - Ferramentas de **Linha**, **Retângulo**, **Círculo**, **Arco** e **Curvas Bézier** com preview elástico (*rubber-banding*).
- [x] **Ferramentas de Edição Interativa**:
  - Caixa de Seleção por arraste de mouse (*rubber-band selection box*).
  - Arraste e mover elementos diretamente no Canvas.
  - Ações de Mover, Copiar, Rotacionar (90°), **Clonagem Espelhada Simétrica**, Escala (1.5x) e Exclusão.
  - Atalhos de teclado (`Delete`, `Ctrl+D`, `ESC`).
- [x] **Gerenciamento de Camadas**:
  - Organização por Z-Index, controle de visibilidade (👁/🙈) e bloqueio contra edições (🔒/🔓).
- [x] **Cotas Tecnológicas**:
  - Ferramentas de cotagem **Horizontal**, **Vertical**, **Raio**, **Diâmetro** e **Angular** com linhas de chamada e rótulos formatados em mm.
- [x] **Exportadores CAD Escala 1:1**:
  - Exportação para **SVG**, **DXF** (AutoCAD R12 em mm para corte laser/CNC), **PDF** e **PNG**.

---

## 🤖 Sistema de Robôs e Agentes (Antigravity)

O projeto possui um ecossistema de robôs em `.agents/` para auxiliar o desenvolvimento continuado através do assistente de IA:

| Robô / Skill | Descrição |
| :--- | :--- |
| **`leathercad-architect`** | Governança da arquitetura, encerramento sequencial obrigatório de 100% de cada fase e atualização do `roadmap.md`. |
| **`leathercad-geometry-engine`** | Matemática 2D, primitivas imutáveis, álgebra vetorial e exportação CAD (SVG/DXF/PDF). |
| **`leathercad-leather-engine`** | Especificações de costura (francesa, europeia, redonda), furação (chisel), vincos e cantos arredondados. |
| **`leathercad-parametric-engine`** | Tabela de variáveis globais (dimensões de cartões, espessura de couro) e recálculo reativo de peças. |
| **`leathercad-ui-javafx`** | Desenvolvimento da UI JavaFX, ferramentas do canvas 2D, réguas e inspetor de propriedades. |

---

## 🗺️ Roadmap do Projeto

Acompanhe o desenvolvimento completo de todas as 13 fases no arquivo **[roadmap.md](roadmap.md)**.
