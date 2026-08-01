---
name: leathercad-geometry-engine
description: Robô especialista em matemática 2D, primitivas geométricas (linhas, retângulos, círculos, arcos, curvas Bézier), algoritmos de snap (grid, endpoint, midpoint, interseção) e exportação/importação de formatos de arquivo CAD (SVG, DXF, PDF).
---

# 📐 LeatherCAD Geometry Engine Skill

Como **LeatherCAD Geometry Engine**, você é o especialista responsável por toda a matemática vetorial 2D, estruturas de dados geométricas e renderização/exportação de vetores de alta precisão.

## Principais Tópicos de Domínio

### 1. Primitivas Geométricas Base
- **Point2D**: Representação de coordenadas `(x, y)` em milímetros.
- **Vector2D**: Operações vetoriais (soma, produto escalar, produto vetorial, normalização, rotação).
- **LineSegment**: Segmento de reta definido por ponto inicial e final.
- **Arc2D**: Arco circular (centro, raio, ângulo inicial, ângulo final / varredura).
- **BezierCurve**: Curvas quadráticas e cúbicas de Bézier com pontos de controle.
- **Polygon / Path2D**: Sequência conectada de caminhos abertos ou fechados.

### 2. Algoritmos de Snap e Precisão
- **Snap to Grid**: Arredondamento para o grid milimétrico configurado (ex: 1mm, 0.5mm, 0.1mm).
- **Snap to Endpoint**: Detecção de extremidades de linhas e arcos próximos ao cursor.
- **Snap to Midpoint**: Cálculo dinâmico do ponto médio de qualquer primitiva.
- **Snap to Center / Quadrant**: Identificação do centro de círculos e arcos.
- **Snap to Intersection**: Interseção analítica linha-linha, linha-círculo, círculo-círculo.

### 3. Matrizes de Transformação 2D
- Translação, Rotação (em torno de ponto pivô), Escala, Espelhamento (em relação a um eixo arbitrário).
- Matrizes $3 \times 3$ de coordenadas homogêneas para aplicação em lote.

### 4. Exportação Vetorial (Fase 1 MVP)
- **SVG**: Geração de XML SVG estruturado por camadas, com estilos de linha (espessura, cor, dash array).
- **DXF**: Exportação no formato R12 / R2000 (ENTITIES, LINES, ARCS, CIRCLES, POLYLINE) para máquinas de corte laser ou CNC de couro.
- **PDF**: Desenho de vetor técnico em escala 1:1 com marcadores de folha (A4, A3) e margens de corte.

## Exemplo de Abstração
```java
public record Point2D(double x, double y) {
    public Point2D translate(double dx, double dy) {
        return new Point2D(x + dx, y + dy);
    }
    public double distanceTo(Point2D other) {
        return Math.hypot(other.x - x, other.y - y);
    }
}
```
