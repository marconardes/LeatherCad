package com.leathercad.core.model;

import com.leathercad.core.geometry.Arc2D;
import com.leathercad.core.geometry.LineSegment;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Polyline2D;
import com.leathercad.core.geometry.Rect2D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DocumentFilletTest {

    private Document document;
    private Layer mainLayer;

    @BeforeEach
    public void setUp() {
        document = new Document();
        mainLayer = document.getActiveLayer();
    }

    @Test
    public void testArc2DCalculateFilletOrthogonal() {
        Point2D pA = new Point2D(0, 0);
        Point2D vertex = new Point2D(0, 100);
        Point2D pB = new Point2D(100, 100);

        var resultOpt = Arc2D.calculateFillet(pA, vertex, pB, 10.0);
        assertTrue(resultOpt.isPresent());

        var fRes = resultOpt.get();
        assertEquals(10.0, fRes.effectiveRadius(), 1e-4);
        assertTrue(fRes.tA().distanceTo(new Point2D(0, 90)) < 1e-4);
        assertTrue(fRes.tB().distanceTo(new Point2D(10, 100)) < 1e-4);
        assertTrue(fRes.center().distanceTo(new Point2D(10, 90)) < 1e-4);
        assertEquals(180.0, fRes.startDeg(), 1e-4);
        assertEquals(-90.0, fRes.sweepDeg(), 1e-4);
    }

    @Test
    public void testArc2DCalculateFilletTruncationWhenRadiusTooLarge() {
        Point2D pA = new Point2D(0, 90); // comprimento 10mm
        Point2D vertex = new Point2D(0, 100);
        Point2D pB = new Point2D(100, 100); // comprimento 100mm

        // Pede raio 50mm mas aresta A só tem 10mm
        var resultOpt = Arc2D.calculateFillet(pA, vertex, pB, 50.0);
        assertTrue(resultOpt.isPresent());

        var fRes = resultOpt.get();
        // O raio efetivo deve ser truncado para no máximo ~9.5mm
        assertTrue(fRes.effectiveRadius() <= 9.5);
        assertTrue(fRes.effectiveRadius() > 0);
    }

    @Test
    public void testApplyFilletAtVertexOnRectElement() {
        RectElement rect = new RectElement(mainLayer.getId(), new Rect2D(0, 0, 100, 50));
        document.addElement(rect);

        // Aplica fillet no vértice 0 (TL - Top Left)
        boolean applied = document.applyFilletAtVertex(rect, 0, 8.0);
        assertTrue(applied);

        CADElement updated = document.findElementById(rect.id());
        assertInstanceOf(RectElement.class, updated);
        RectElement updatedRect = (RectElement) updated;
        assertEquals(8.0, updatedRect.rect().rTopLeft());
        assertEquals(0.0, updatedRect.rect().rTopRight());
    }

    @Test
    public void testApplyFilletAtVertexOnPolylineElement() {
        List<Point2D> pts = List.of(
            new Point2D(0, 0),
            new Point2D(0, 100),
            new Point2D(100, 100),
            new Point2D(100, 0)
        );
        PolylineElement poly = new PolylineElement(mainLayer.getId(), new Polyline2D(pts, true));
        document.addElement(poly);

        // Aplica fillet no vértice 1 (0, 100)
        boolean applied = document.applyFilletAtVertex(poly, 1, 10.0);
        assertTrue(applied);

        // Deve ter adicionado um ArcElement no documento
        boolean hasArc = document.getElements().stream().anyMatch(e -> e instanceof ArcElement);
        assertTrue(hasArc);

        // A polilina deve ter sido atualizada com os pontos de tangência no lugar do vértice 1
        CADElement updated = document.findElementById(poly.id());
        assertInstanceOf(PolylineElement.class, updated);
        PolylineElement updatedPoly = (PolylineElement) updated;
        assertEquals(5, updatedPoly.polyline().points().size());
    }

    @Test
    public void testApplyFilletAtVertexOnLinePair() {
        LineElement l1 = new LineElement(mainLayer.getId(), new LineSegment(new Point2D(0, 0), new Point2D(0, 100)));
        LineElement l2 = new LineElement(mainLayer.getId(), new LineSegment(new Point2D(0, 100), new Point2D(100, 100)));
        document.addElement(l1);
        document.addElement(l2);

        boolean applied = document.applyFilletAtVertex(l1, 1, 10.0);
        assertTrue(applied);

        boolean hasArc = document.getElements().stream().anyMatch(e -> e instanceof ArcElement);
        assertTrue(hasArc);
    }

    @Test
    public void testFilletScopeWithThreeEdgesSelected() {
        RectElement rect = new RectElement(mainLayer.getId(), new Rect2D(0, 0, 100, 50));
        document.addElement(rect);

        // Seleciona 3 arestas do retângulo: 0 (Top), 1 (Right), 2 (Bottom)
        document.selectSubElement(new SubElementRef(rect.id(), SubElementRef.SubElementType.EDGE, 0), false);
        document.selectSubElement(new SubElementRef(rect.id(), SubElementRef.SubElementType.EDGE, 1), true);
        document.selectSubElement(new SubElementRef(rect.id(), SubElementRef.SubElementType.EDGE, 2), true);

        // Aplica o raio de canto de 10mm aos cantos selecionados
        document.setCornerRadiusSelected(10.0);

        CADElement updated = document.findElementById(rect.id());
        assertInstanceOf(RectElement.class, updated);
        RectElement updatedRect = (RectElement) updated;

        // Canto TR (1) conecta edge 0 e 1 -> deve ser 10.0
        assertEquals(10.0, updatedRect.rect().rTopRight());
        // Canto BR (2) conecta edge 1 e 2 -> deve ser 10.0
        assertEquals(10.0, updatedRect.rect().rBottomRight());

        // Canto TL (0) e BL (3) dependem da edge 3 que NAO foi selecionada -> devem ser 0.0
        assertEquals(0.0, updatedRect.rect().rTopLeft());
        assertEquals(0.0, updatedRect.rect().rBottomLeft());
    }

    @Test
    public void testFilletScopeWithSpecificVerticesSelected() {
        RectElement rect = new RectElement(mainLayer.getId(), new Rect2D(0, 0, 100, 50));
        document.addElement(rect);

        // Seleciona apenas o vértice 0 (TL)
        document.selectSubElement(new SubElementRef(rect.id(), SubElementRef.SubElementType.VERTEX, 0), false);

        document.setCornerRadiusSelected(5.0);

        CADElement updated = document.findElementById(rect.id());
        assertInstanceOf(RectElement.class, updated);
        RectElement updatedRect = (RectElement) updated;

        assertEquals(5.0, updatedRect.rect().rTopLeft());
        assertEquals(0.0, updatedRect.rect().rTopRight());
        assertEquals(0.0, updatedRect.rect().rBottomRight());
        assertEquals(0.0, updatedRect.rect().rBottomLeft());
    }
}
