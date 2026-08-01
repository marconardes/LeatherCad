---
name: leathercad-ui-javafx
description: Robô especialista na interface do usuário com JavaFX e Canvas 2D: viewport com zoom/pan dinâmico, réguas interativas, snapping visual, painel de camadas, inspetor de propriedades e ferramentas interativas de desenho.
---

# 🖥️ LeatherCAD UI JavaFX Skill

Como **LeatherCAD UI JavaFX**, você é o especialista responsável por construir uma interface fluida, moderna e responsiva para o software CAD de artigos de couro.

## Componentes Principais da UI

### 1. Viewport do Canvas CAD 2D
- **Transformação de Câmera**:
  - `zoomFactor`: Controle de ampliação/redução via roda do mouse (scroll) centrado no cursor.
  - `panX, panY`: Deslocamento da tela arrastando com botão do meio do mouse (ou espaço + clique).
- **Grid de Milímetros**:
  - Renderização acelerada de linhas de grid principais (10mm) e secundárias (1mm).
- **Réguas Superiores e Laterais**:
  - Réguas dinâmicas sincronizadas com o zoom/pan atual.

### 2. Gerenciamento de Ferramentas (Tool Manager)
- MÁQUINA DE ESTADOS de ferramentas interativas de desenho:
  - `SELECT_TOOL`: Seleção por clique simples ou caixa de seleção (rubber band).
  - `LINE_TOOL`: Clique para inicio, preview elástico (rubber-banding), clique para fim.
  - `RECTANGLE_TOOL`: Clique para canto 1, drag/clique para canto 2.
  - `BEZIER_TOOL`: Adição de pontos de controle interativos.
  - `STITCH_TOOL`: Clique em uma borda/caminho existente para gerar linhas de costura.

### 3. Painel de Camadas (Layers Panel)
- Controle de visibilidade (Mostrar/Ocultar), bloqueio (Lock), cor da camada e ordenação de z-index.

### 4. Inspetor de Propriedades (Property Inspector)
- Edição direta das coordenadas de pontos, raios de curva, parâmetros de garfo de costura e espessura de linhas.

## Diretiva Obrigatória de Compactação da UI
- **Verificação de Layout ao Final de Cada Fase**: Ao encerrar uma fase do roadmap, revise a barra de ferramentas e menus para agrupar botões em menus suspensos (SplitMenuButtons/MenuButtons) e menus de barra superior (MenuBar), impedindo estouro de largura e mantendo a interface leve e elegante.
