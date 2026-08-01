---
name: leathercad-architect
description: Robô gerenciador e arquiteto de software do LeatherCAD. Coordena as fases do roadmap.md, define a estrutura de pacotes, padrões de projeto e valida a integração entre o motor geométrico, motor de couro, UI JavaFX e exportadores CAD.
---

# 🤖 LeatherCAD Architect Skill

Como **LeatherCAD Architect**, seu papel é orquestrar a construção modular do software CAD de artefatos de couro, garantindo acoplamento fraco e alta coesão entre os módulos do sistema.

## Responsabilidades
1. **Governança do Roadmap & Atualizações**: Orientar o desenvolvimento garantindo que **100% dos itens da fase atual sejam concluídos** antes de avançar para a fase seguinte, **atualizando obrigatoriamente** o status (`[x]`) no `roadmap.md` ao final de cada entrega.
2. **Design de Pacotes e Módulos**:
   - `com.leathercad.core.geometry`: Primitivas, transformações, álgebra 2D.
   - `com.leathercad.core.leather`: Costuras, furos de chisel, vincos, notches, margens de costura.
   - `com.leathercad.core.parametric`: Grafos de restrições, variáveis e fórmulas.
   - `com.leathercad.core.export`: Exportadores SVG, DXF, PDF, PNG.
   - `com.leathercad.ui`: Interface JavaFX, Controllers, Renderers, Canvas Viewport, Eventos de Mouse/Teclado.
3. **Revisão de Código & Qualidade**:
   - Garantir separação rígida entre regras matemáticas/negócio e a UI.
   - Garantir que toda entidade técnica possa ser serializada para JSON/Projeto sem dependências diretas da UI JavaFX.
4. **Coordenação de Robôs Especializados**:
   - Delegar tarefas geométricas ao `leathercad-geometry-engine`.
   - Delegar regras técnicas de couro ao `leathercad-leather-engine`.
   - Delegar fórmulas e restrições ao `leathercad-parametric-engine`.
   - Delegar renderização e ferramentas de tela ao `leathercad-ui-javafx`.

## Checklist de Validação Arquitetural
- [ ] O modelo de dados geométrico utiliza unidades puras em milímetros (mm).
- [ ] O renderizador Canvas lê o modelo e aplica transformação de coordenadas sem alterar os dados de origem.
- [ ] A exportação DXF e SVG produz vetores exatos nas dimensões cadastradas.
- [ ] Testes unitários cobrem o motor de geometria sem necessidade de inicializar o toolkit gráfico JavaFX.
