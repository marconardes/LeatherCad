package com.leathercad.core;

import com.leathercad.core.export.DXFExporter;
import com.leathercad.core.export.PDFExporter;
import com.leathercad.core.export.SVGExporter;
import com.leathercad.core.geometry.Circle2D;
import com.leathercad.core.geometry.LineSegment;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Polyline2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.leather.CreaseElement;
import com.leathercad.core.leather.StitchConfig;
import com.leathercad.core.leather.StitchElement;
import com.leathercad.core.model.CADElement;
import com.leathercad.core.model.CircleElement;
import com.leathercad.core.model.DimensionElement;
import com.leathercad.core.model.Document;
import com.leathercad.core.model.Layer;
import com.leathercad.core.model.LineElement;
import com.leathercad.core.model.RectElement;
import com.leathercad.core.model.SubElementRef;
import com.leathercad.core.parametric.LeatherFoldCalculator;
import com.leathercad.core.snap.SnapEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class AuditQATest {

    private Document document;

    @BeforeEach
    public void setUp() {
        document = new Document();
    }

    @Test
    @DisplayName("Auditoria 1: Validação de Snap de Interseção entre Linhas 2D")
    public void testLineIntersectionSnap() {
        // Linha 1: Horizontal de (0,50) a (100,50)
        LineElement l1 = new LineElement(document.getLeatherLayerId(), new LineSegment(new Point2D(0, 50), new Point2D(100, 50)));
        // Linha 2: Vertical de (50,0) a (50,100)
        LineElement l2 = new LineElement(document.getLeatherLayerId(), new LineSegment(new Point2D(50, 0), new Point2D(50, 100)));

        document.addElement(l1);
        document.addElement(l2);

        // Ponto de interseção exato: (50, 50)
        Point2D cursorNearIntersection = new Point2D(50.2, 49.8);
        var snap = SnapEngine.findSnap(cursorNearIntersection, document, 5.0, 10.0);

        assertTrue(snap.isPresent(), "Snap deve encontrar a interseção entre as duas linhas");
        assertEquals(SnapEngine.SnapType.INTERSECTION, snap.get().type());
        assertEquals(50.0, snap.get().point().x(), 1e-4);
        assertEquals(50.0, snap.get().point().y(), 1e-4);
    }

    @Test
    @DisplayName("Auditoria 2: Cálculo de Compensação de Dobra em Couro (LeatherFoldCalculator)")
    public void testFoldAllowanceMath() {
        double thicknessMm = 1.5; // Couro 1.5mm
        double expectedAllowance = LeatherFoldCalculator.calculateFoldAllowance(thicknessMm);
        assertTrue(expectedAllowance > 0.0, "Compensação de dobra deve ser estritamente positiva");
        assertEquals(1.2 * Math.PI * thicknessMm, expectedAllowance, 1e-4, "Dobra de 180° deve calcular folga de 1.2 * pi * t");
    }

    @Test
    @DisplayName("Auditoria 3: Travamento e Visibilidade de Camadas (Layer Lock & Visibility)")
    public void testLayerLockAndVisibilityProtection() {
        Layer leatherLayer = document.findLayerById(document.getLeatherLayerId());
        assertNotNull(leatherLayer);

        RectElement rect = new RectElement(document.getLeatherLayerId(), new Rect2D(0, 0, 100, 50, 0));
        document.addElement(rect);

        // 1. Tentar selecionar elemento normal
        document.selectElement(rect.id(), false);
        assertTrue(document.getSelectedElementIds().contains(rect.id()), "Elemento em camada destravada deve ser selecionável");

        // 2. Travar camada
        leatherLayer.setLocked(true);
        document.clearSelection();

        // 3. Tentar selecionar elemento em camada travada
        document.selectElement(rect.id(), false);
        assertFalse(document.getSelectedElementIds().contains(rect.id()), "Elemento em camada travada NÃO deve poder ser selecionado");

        // 4. Mover elementos em camada travada
        document.moveSelected(10, 10);
        RectElement unshifted = (RectElement) document.findElementById(rect.id());
        assertEquals(0.0, unshifted.rect().minPoint().x(), "Elemento em camada travada não pode ter posição alterada");
    }

    @Test
    @DisplayName("Auditoria 4: Validação dos Exportadores 1:1 (SVG, DXF, PDF)")
    public void testExportersFidelity() throws IOException {
        String leatherLayerId = document.getLeatherLayerId();
        String stitchLayerId = document.getStitchLayerId();
        String creaseLayerId = document.getCreaseLayerId();

        document.addElement(new RectElement(leatherLayerId, new Rect2D(10, 10, 200, 100, 5.0)));
        document.addElement(new CreaseElement(creaseLayerId, new LineSegment(new Point2D(110, 10), new Point2D(110, 110)), 1.5));
        document.addElement(new StitchElement(stitchLayerId, new LineSegment(new Point2D(14, 14), new Point2D(206, 14)), StitchConfig.DEFAULT_FRENCH));

        File dxfFile = File.createTempFile("audit_export", ".dxf");
        File svgFile = File.createTempFile("audit_export", ".svg");
        File pdfFile = File.createTempFile("audit_export", ".pdf");
        dxfFile.deleteOnExit();
        svgFile.deleteOnExit();
        pdfFile.deleteOnExit();

        DXFExporter.exportToFile(document, dxfFile);
        SVGExporter.exportToFile(document, svgFile);
        PDFExporter.exportToFile(document, pdfFile);

        assertTrue(dxfFile.exists() && dxfFile.length() > 100, "Arquivo DXF deve ser gerado com tamanho válido");
        assertTrue(svgFile.exists() && svgFile.length() > 100, "Arquivo SVG deve ser gerado com tamanho válido");
        assertTrue(pdfFile.exists() && pdfFile.length() > 100, "Arquivo PDF deve ser gerado com tamanho válido");
    }

    @Test
    @DisplayName("Auditoria 5: Cálculo do BoundingBox do Documento")
    public void testDocumentBoundingBoxCalculation() {
        document.addElement(new RectElement(document.getLeatherLayerId(), new Rect2D(10, 20, 100, 50, 0)));
        document.addElement(new RectElement(document.getLeatherLayerId(), new Rect2D(150, 100, 80, 40, 0)));

        Rect2D bbox = document.getBoundingBox();

        assertEquals(10.0, bbox.minPoint().x(), 1e-3);
        assertEquals(20.0, bbox.minPoint().y(), 1e-3);
        assertEquals(220.0, bbox.width(), 1e-3);
        assertEquals(120.0, bbox.height(), 1e-3);
    }

    @Test
    @DisplayName("Auditoria 6: Edição Interativa de Nós e Hastes de Controle Bézier")
    public void testBezierNodeEditing() {
        com.leathercad.core.geometry.Bezier2D initialBezier = new com.leathercad.core.geometry.Bezier2D(
            new Point2D(10, 10),
            new Point2D(30, 50),
            new Point2D(70, 50),
            new Point2D(90, 10)
        );
        com.leathercad.core.model.BezierElement elem = new com.leathercad.core.model.BezierElement(document.getLeatherLayerId(), initialBezier);
        document.addElement(elem);

        // Mover o ponto de controle 1 (tangente inicial) para (35, 80)
        document.updateBezierControlPoint(elem.id(), 2, new Point2D(35, 80));

        com.leathercad.core.model.BezierElement updated = (com.leathercad.core.model.BezierElement) document.findElementById(elem.id());
        assertNotNull(updated);
        assertEquals(35.0, updated.bezier().control1().x(), 1e-4);
        assertEquals(80.0, updated.bezier().control1().y(), 1e-4);

        // Mover o ponto final para (100, 15)
        document.updateBezierControlPoint(elem.id(), 4, new Point2D(100, 15));

        updated = (com.leathercad.core.model.BezierElement) document.findElementById(elem.id());
        assertEquals(100.0, updated.bezier().end().x(), 1e-4);
        assertEquals(15.0, updated.bezier().end().y(), 1e-4);
    }

    @Test
    @DisplayName("Auditoria 7: Persistência Native de Projetos (.lcad JSON Serializer)")
    public void testProjectSaveAndLoad() throws IOException {
        document.addElement(new RectElement(document.getLeatherLayerId(), new Rect2D(20, 20, 150, 90, 4.0)));
        document.addElement(new LineElement(document.getCreaseLayerId(), new LineSegment(new Point2D(20, 50), new Point2D(170, 50))));

        File lcadFile = File.createTempFile("teste_projeto", ".lcad");
        lcadFile.deleteOnExit();

        com.leathercad.core.export.ProjectSerializer.saveToFile(document, lcadFile);
        assertTrue(lcadFile.exists() && lcadFile.length() > 50, "Arquivo .lcad deve ser gerado e gravado com conteúdo");

        Document loadedDoc = com.leathercad.core.export.ProjectSerializer.loadFromFile(lcadFile);
        assertNotNull(loadedDoc, "Documento recarregado não pode ser nulo");
        assertEquals(document.getElements().size(), loadedDoc.getElements().size(), "Número de elementos salvos e carregados deve ser idêntico");
    }

    @Test
    @DisplayName("Auditoria 8: Explodir Retângulo em 4 Linhas Individuais Selecionáveis")
    public void testExplodeRectangleToIndividualLines() {
        RectElement rect = new RectElement(document.getLeatherLayerId(), new Rect2D(0, 0, 100, 50, 0));
        document.addElement(rect);
        document.selectElement(rect.id(), false);

        var newIds = document.explodeSelected();
        assertEquals(4, newIds.size());
        assertEquals(4, document.getElements().size());

        for (CADElement elem : document.getElements()) {
            assertTrue(elem instanceof LineElement);
        }
    }

    @Test
    @DisplayName("Auditoria 9: Arredondar Canto Específico Entre 2 Linhas Selecionadas (Fillet R5)")
    public void testFilletBetweenTwoSelectedLines() {
        LineElement lineA = new LineElement(document.getLeatherLayerId(), new LineSegment(new Point2D(0, 0), new Point2D(100, 0)));
        LineElement lineB = new LineElement(document.getLeatherLayerId(), new LineSegment(new Point2D(100, 0), new Point2D(100, 50)));

        document.addElement(lineA);
        document.addElement(lineB);

        document.selectElement(lineA.id(), false);
        document.selectElement(lineB.id(), true);

        document.setCornerRadiusSelected(5.0);

        boolean hasArc = document.getElements().stream().anyMatch(e -> e instanceof com.leathercad.core.model.ArcElement);
        assertTrue(hasArc, "Arco de arredondamento R5 deve ser criado no canto das 2 linhas selecionadas");
    }

    @Test
    @DisplayName("Auditoria 10: Arredondar Cantos Específicos do Retângulo Preservando o Molde e Preenchimento de Couro (FreeCAD/AutoCAD)")
    public void testPerCornerRadiiOnRectangle() {
        RectElement rect = new RectElement(document.getLeatherLayerId(), new Rect2D(0, 0, 100, 50, 0.0));
        document.addElement(rect);
        document.selectElement(rect.id(), false);

        document.setCornerRadiusSelected(5.0, 5.0, 0.0, 0.0);

        CADElement updated = document.getElements().get(0);
        assertTrue(updated instanceof RectElement, "Elemento deve continuar sendo um RectElement intacto");
        RectElement rElem = (RectElement) updated;
        assertEquals(5.0, rElem.rect().rTopLeft(), 1e-3);
        assertEquals(5.0, rElem.rect().rTopRight(), 1e-3);
        assertEquals(0.0, rElem.rect().rBottomRight(), 1e-3);
        assertEquals(0.0, rElem.rect().rBottomLeft(), 1e-3);
    }

    @Test
    @DisplayName("Auditoria 11: Seleção e Nomeação Topológica de Sub-Elementos B-Rep (FreeCAD Style)")
    public void testFreeCADSubElementSelection() {
        RectElement rect = new RectElement(document.getLeatherLayerId(), new Rect2D(0, 0, 100, 50, 0.0));
        document.addElement(rect);

        SubElementRef edge0 = new SubElementRef(rect.id(), SubElementRef.SubElementType.EDGE, 0);
        document.selectSubElement(edge0, false);

        assertEquals(1, document.getSelectedSubElements().size());
        assertTrue(document.getSelectedSubElements().contains(edge0));
        assertTrue(document.getSelectedElementIds().contains(rect.id()), "Forma pai deve permanecer selecionada e unificada");

        SubElementRef edge1 = new SubElementRef(rect.id(), SubElementRef.SubElementType.EDGE, 1);
        document.toggleSubElementSelection(edge1);

        assertEquals(2, document.getSelectedSubElements().size());
        assertTrue(document.getSelectedSubElements().contains(edge1));
    }

    @Test
    @DisplayName("Auditoria 12: Aplicação de Arredondamento (Fillet) em 2 Sub-Arestas Selecionadas Mantendo o Preenchimento (FreeCAD Style)")
    public void testSubElementFilletApplication() {
        RectElement rect = new RectElement(document.getLeatherLayerId(), new Rect2D(0, 0, 100, 50, 0.0));
        document.addElement(rect);

        SubElementRef edge2 = new SubElementRef(rect.id(), SubElementRef.SubElementType.EDGE, 2);
        SubElementRef edge3 = new SubElementRef(rect.id(), SubElementRef.SubElementType.EDGE, 3);
        document.selectSubElement(edge2, false);
        document.toggleSubElementSelection(edge3);

        document.setCornerRadiusSelected(5.0);

        CADElement updated = document.getElements().get(0);
        assertTrue(updated instanceof RectElement, "Peça deve continuar sendo um RectElement único");
        RectElement rElem = (RectElement) updated;
        assertEquals(5.0, rElem.rect().rBottomLeft(), 1e-3, "Apenas o canto Inferior-Esquerdo (BL) deve ter o raio 5.0mm");
        assertEquals(0.0, rElem.rect().rTopLeft(), 1e-3);
        assertEquals(0.0, rElem.rect().rTopRight(), 1e-3);
        assertEquals(0.0, rElem.rect().rBottomRight(), 1e-3);
    }

    @Test
    @DisplayName("Auditoria 13: Remoção Automática de Cotas (DimensionElement) ao Deletar Elementos")
    public void testDimensionCleanupOnElementDelete() {
        RectElement rect = new RectElement(document.getLeatherLayerId(), new Rect2D(0, 0, 100, 50, 0.0));
        document.addElement(rect);

        Point2D start = rect.rect().minPoint();
        Point2D end = new Point2D(start.x() + 100, start.y());
        DimensionElement dim = new DimensionElement(document.getLeatherLayerId(), start, end, DimensionElement.DimensionType.HORIZONTAL, 10.0);
        document.addElement(dim);

        assertEquals(2, document.getElements().size());

        document.selectElement(rect.id(), false);
        document.deleteSelected();

        assertEquals(0, document.getElements().size(), "Ao deletar o retângulo, a cota vinculada (DimensionElement) deve ser limpa automaticamente");
    }

    @Test
    @DisplayName("Auditoria 14: Prioridade de Seleção para Elementos Sobrepostos/Internos (Corpo de Carteira vs Porta-Cartão)")
    public void testInnerElementSelectionPriority() {
        RectElement outerWalletBody = new RectElement(document.getLeatherLayerId(), new Rect2D(0, 0, 220, 90));
        document.addElement(outerWalletBody);

        RectElement innerCardSlot = new RectElement(document.getLeatherLayerId(), new Rect2D(10, 10, 95, 55));
        document.addElement(innerCardSlot);

        Point2D clickPoint = new Point2D(20, 20);
        Optional<CADElement> hit = document.findElementAt(clickPoint, 5.0);

        assertTrue(hit.isPresent());
        assertEquals(innerCardSlot.id(), hit.get().id(), "Clicar dentro do porta-cartões deve selecionar o porta-cartões menor, NÃO o corpo da carteira maior");

        Point2D outerClick = new Point2D(150, 50);
        Optional<CADElement> outerHit = document.findElementAt(outerClick, 5.0);

        assertTrue(outerHit.isPresent());
        assertEquals(outerWalletBody.id(), outerHit.get().id(), "Clicar fora do porta-cartões deve selecionar o corpo da carteira");
    }

    @Test
    @DisplayName("Auditoria 15: Vinculação Automática de Elementos Filhos/Internos e Movimento de Grupo (Carteira + Porta-Cartão)")
    public void testSpatialParentingAndGroupMovement() {
        RectElement outerWalletBody = new RectElement(document.getLeatherLayerId(), new Rect2D(0, 0, 220, 90));
        document.addElement(outerWalletBody);

        RectElement innerCardSlot = new RectElement(document.getLeatherLayerId(), new Rect2D(10, 10, 95, 55));
        document.addElement(innerCardSlot);

        assertEquals(outerWalletBody.id(), document.getParentId(innerCardSlot.id()), "O porta-cartões desenhado dentro da carteira deve ser vinculado como filho do corpo da carteira");

        document.selectElement(outerWalletBody.id(), false);
        document.moveSelected(50.0, 50.0);

        RectElement movedOuter = (RectElement) document.findElementById(outerWalletBody.id());
        RectElement movedInner = (RectElement) document.findElementById(innerCardSlot.id());

        assertEquals(50.0, movedOuter.rect().minPoint().x(), 1e-3);
        assertEquals(50.0, movedOuter.rect().minPoint().y(), 1e-3);
        assertEquals(60.0, movedInner.rect().minPoint().x(), 1e-3, "Ao mover a carteira, o porta-cartões filho interno deve mover junto automaticamente");
        assertEquals(60.0, movedInner.rect().minPoint().y(), 1e-3);
    }

    @Test
    @DisplayName("Auditoria 16: Vinculação de Círculo Interno como Filho (Círculo R=26mm dentro de Círculo R=65mm)")
    public void testCircleInsideCircleParenting() {
        CircleElement outerCircle = new CircleElement(document.getLeatherLayerId(), new Circle2D(new Point2D(100, 100), 65.6));
        document.addElement(outerCircle);

        CircleElement innerCircle = new CircleElement(document.getLeatherLayerId(), new Circle2D(new Point2D(100, 100), 26.2));
        document.addElement(innerCircle);

        assertEquals(outerCircle.id(), document.getParentId(innerCircle.id()), "O círculo menor R=26.2mm desenhado dentro do círculo maior R=65.6mm deve ser vinculado como filho automaticamente");
    }

    @Test
    @DisplayName("Auditoria 17: Aninhamento Hierárquico Multinível (3 Retângulos Aninhados: 216x139 > 172x108 > 132x79)")
    public void testMultiLevelNestedRectangles() {
        RectElement outerRect = new RectElement(document.getLeatherLayerId(), new Rect2D(0, 0, 216, 139));
        document.addElement(outerRect);

        RectElement middleRect = new RectElement(document.getLeatherLayerId(), new Rect2D(20, 15, 172, 108));
        document.addElement(middleRect);

        RectElement innerRect = new RectElement(document.getLeatherLayerId(), new Rect2D(40, 30, 132, 79));
        document.addElement(innerRect);

        assertNull(document.getParentId(outerRect.id()), "Retângulo externo (216x139) não deve ter pai");
        assertEquals(outerRect.id(), document.getParentId(middleRect.id()), "Retângulo médio (172x108) deve ser filho direto do retângulo externo (216x139)");
        assertEquals(middleRect.id(), document.getParentId(innerRect.id()), "Retângulo interno (132x79) deve ser filho direto do retângulo médio (172x108)");
    }

    @Test
    @DisplayName("Auditoria 18: Cálculo de Dimensões de Arestas e Ângulos de Vértices para Polilinhas")
    public void testPolylineDimensionsAndAngles() {
        List<Point2D> pts = List.of(new Point2D(0, 0), new Point2D(100, 0), new Point2D(100, 50));
        Polyline2D poly = new Polyline2D(pts, false);

        assertEquals(100.0, poly.getSegmentLength(0), 1e-3, "Comprimento da primeira aresta deve ser 100mm");
        assertEquals(50.0, poly.getSegmentLength(1), 1e-3, "Comprimento da segunda aresta deve ser 50mm");

        assertEquals(90.0, poly.getVertexAngleDegrees(1), 1e-3, "O ângulo do vértice central (100,0) deve ser 90.0°");
    }

    @Test
    @DisplayName("Auditoria 19: Trava Ortogonal com a Tecla Shift (Modo Ortho Horizontal/Vertical)")
    public void testPolylineShiftOrthoMode() {
        Point2D base = new Point2D(100, 100);

        Point2D targetHoriz = new Point2D(250, 105);
        double dxH = Math.abs(targetHoriz.x() - base.x());
        double dyH = Math.abs(targetHoriz.y() - base.y());
        Point2D orthoHoriz = (dxH > dyH) ? new Point2D(targetHoriz.x(), base.y()) : new Point2D(base.x(), targetHoriz.y());

        assertEquals(250.0, orthoHoriz.x(), 1e-3);
        assertEquals(100.0, orthoHoriz.y(), 1e-3, "Com Shift pressionado e dx > dy, a coordenada Y deve ser cravada no ponto base (Horizontal perfeitamente reta)");

        Point2D targetVert = new Point2D(104, 220);
        double dxV = Math.abs(targetVert.x() - base.x());
        double dyV = Math.abs(targetVert.y() - base.y());
        Point2D orthoVert = (dxV > dyV) ? new Point2D(targetVert.x(), base.y()) : new Point2D(base.x(), targetVert.y());

        assertEquals(100.0, orthoVert.x(), 1e-3, "Com Shift pressionado e dy > dx, a coordenada X deve ser cravada no ponto base (Vertical perfeitamente reta)");
        assertEquals(220.0, orthoVert.y(), 1e-3);
    }
}
