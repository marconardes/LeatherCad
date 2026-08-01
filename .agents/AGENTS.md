# LeatherCAD - Regras do Projeto e Diretrizes para o Antigravity

Este arquivo contém as diretrizes globais de desenvolvimento para o projeto **LeatherCAD**. Todas as sessões do Antigravity neste workspace devem respeitar estas diretrizes.

## Stack Tecnológica & Arquitetura
- **Linguagem Principal**: Java 21 LTS (com suporte a records, pattern matching, virtual threads).
- **Interface Gráfica**: JavaFX (Canvas 2D para renderização acelerada de precisão).
- **Build System**: Maven ou Gradle.
- **Geometria & Matemática**: Precisão milimétrica em coordenadas 2D de ponto flutuante (`double`).
- **Padrão Arquitetural**: Clean Architecture / DDD separando Core (Geometria e Regras de Negócio do Couro) da UI (JavaFX) e Exportadores (SVG/DXF/PDF).

## Padrões de Código & Design System 2D
- **Imutabilidade**: Primitivas geométricas simples (Ponto, Vetor, Marcadores) devem ser imutáveis.
- **Unidades**: Todas as dimensões internas são mantidas em **milímetros (mm)**. Conversores de visualização devem manipular zoom e DPI do Canvas.
- **Camadas & Seleção**: O motor gráfico deve suportar seleções dinâmicas, snap para pontos de ancoragem (endpoints, midpoints, interseções, alinhamentos) e visibilidade por camadas.
- **Extensibilidade**: Motores de couro (costuras, vincos, boleadores) estendem a geometria base adicionando meta-informações de fabricação.

## Fluxo por Fases & Atualização do Roadmap (roadmap.md)
- **Execução Sequencial Obrigatória por Fases**: O Antigravity **deve obrigatoriamente concluir 100% dos itens da fase atual** no `roadmap.md` antes de iniciar qualquer funcionalidade da fase seguinte.
- **Revisão e Compactação de UI ao Final de Cada Fase**: Ao encerrar qualquer fase do roadmap, os robôs **devem obrigatoriamente revisar o layout da interface gráfica (UI JavaFX)** para garantir que a barra de ferramentas, menus e painéis estejam limpos, organizados em menus suspensos/dropdowns e compactos, impedindo o estouro de largura na tela do usuário.
1. **Fase 1 - MVP**: Área de desenho 2D, primitivas (linha, retângulo, círculo, arco, bézier), edição (mover, copiar, rotacionar, espelhar, escalar), camadas, cotas e exportadores (SVG, DXF, PDF, PNG).
2. **Fase 2 - Ferramentas de Couro**: Costuras parametrizadas, furações (chisel), vincos, boleadores, chanfros, cantos arredondados (radii R3/R5/R8/R10) e notches.
3. **Fases 3 a 13**: Biblioteca de componentes, motor paramétrico, simulação, nesting (corte inteligente) e IA.

## Agente Especialista: Cliente Chato (`leathercad-picky-client`)
- **Crivo de Qualidade & Usabilidade**: Toda funcionalidade entregue deve passar pela validação do agente **`leathercad-picky-client`**, garantindo que não existam falhas visuais, elementos que "zoam" ao arrastar ou encaixar, nem desalinhamentos de interface.

