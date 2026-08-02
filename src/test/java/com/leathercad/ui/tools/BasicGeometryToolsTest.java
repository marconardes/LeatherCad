package com.leathercad.ui.tools;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.model.DimensionElement;
import com.leathercad.core.model.Document;
import com.leathercad.core.model.LineElement;
import com.leathercad.core.model.RectElement;
import com.leathercad.ui.viewport.CameraTransform;

import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BasicGeometryToolsTest {

    private MouseEvent createDummyMouseEvent(MouseButton button, boolean isShiftDown) {
        return new MouseEvent(
            MouseEvent.MOUSE_PRESSED, 0, 0, 0, 0, button, 1,
            isShiftDown, false, false, false, false, false, false, false, false, false, null
        );
    }

    @Test
    public void testLineToolPureElementCreation() {
        Document document = new Document();
        LineTool lineTool = new LineTool();
        CameraTransform camera = new CameraTransform();

        // 1º Clique: Ponto inicial (0, 0)
        MouseEvent click1 = createDummyMouseEvent(MouseButton.PRIMARY, false);
        lineTool.onMousePressed(click1, new Point2D(0, 0), document, camera);
        assertEquals(0, document.getElements().size());

        // 2º Clique: Ponto final (100, 50)
        MouseEvent click2 = createDummyMouseEvent(MouseButton.PRIMARY, false);
        lineTool.onMousePressed(click2, new Point2D(100, 50), document, camera);

        // Deve conter exatamente 1 elemento pura primitiva LineElement (sem DimensionElement)
        assertEquals(1, document.getElements().size());
        assertTrue(document.getElements().getFirst() instanceof LineElement);
        long dimCount = document.getElements().stream()
                .filter(e -> e instanceof DimensionElement)
                .count();
        assertEquals(0, dimCount, "Não deve adicionar cotas fixas automáticas ao documento.");
    }

    @Test
    public void testRectToolPureElementCreation() {
        Document document = new Document();
        RectTool rectTool = new RectTool();
        CameraTransform camera = new CameraTransform();

        // 1º Clique: Canto (10, 10)
        MouseEvent click1 = createDummyMouseEvent(MouseButton.PRIMARY, false);
        rectTool.onMousePressed(click1, new Point2D(10, 10), document, camera);
        assertEquals(0, document.getElements().size());

        // 2º Clique: Canto Oposto (60, 40)
        MouseEvent click2 = createDummyMouseEvent(MouseButton.PRIMARY, false);
        rectTool.onMousePressed(click2, new Point2D(60, 40), document, camera);

        // Deve conter exatamente 1 elemento pura primitiva RectElement (sem DimensionElement)
        assertEquals(1, document.getElements().size());
        assertTrue(document.getElements().getFirst() instanceof RectElement);
        long dimCount = document.getElements().stream()
                .filter(e -> e instanceof DimensionElement)
                .count();
        assertEquals(0, dimCount, "Não deve adicionar cotas fixas automáticas ao documento.");
    }

    @Test
    public void testRectToolOrthoSquareLocking() {
        RectTool rectTool = new RectTool();
        Point2D base = new Point2D(0, 0);

        // 1. Sem Shift e sem Snap -> Retângulo normal
        Point2D p1 = rectTool.getEffectivePoint(base, new Point2D(100, 40), false, false);
        assertEquals(100.0, p1.x(), 1e-9);
        assertEquals(40.0, p1.y(), 1e-9);

        // 2. Com Shift (+X, +Y) -> max(100, 40) = 100 -> Quadrado (100, 100)
        Point2D p2 = rectTool.getEffectivePoint(base, new Point2D(100, 40), true, false);
        assertEquals(100.0, p2.x(), 1e-9);
        assertEquals(100.0, p2.y(), 1e-9);

        // 3. Com Shift (-X, +Y) -> dx=-30, dy=80 -> max(30, 80) = 80 -> Quadrado (-80, 80)
        Point2D p3 = rectTool.getEffectivePoint(base, new Point2D(-30, 80), true, false);
        assertEquals(-80.0, p3.x(), 1e-9);
        assertEquals(80.0, p3.y(), 1e-9);

        // 4. Com Snap ativo -> Snap prevalece sobre a trava Ortho
        Point2D snapPoint = new Point2D(100, 40);
        Point2D p4 = rectTool.getEffectivePoint(base, snapPoint, true, true);
        assertEquals(100.0, p4.x(), 1e-9);
        assertEquals(40.0, p4.y(), 1e-9);
    }

    @Test
    public void testLineToolOrthoLocking() {
        LineTool lineTool = new LineTool();
        Point2D base = new Point2D(0, 0);

        // dx > dy com Shift -> Alinhamento horizontal em Y=base.y
        Point2D p1 = lineTool.getEffectivePoint(base, new Point2D(80, 20), true, false);
        assertEquals(80.0, p1.x(), 1e-9);
        assertEquals(0.0, p1.y(), 1e-9);

        // dy > dx com Shift -> Alinhamento vertical em X=base.x
        Point2D p2 = lineTool.getEffectivePoint(base, new Point2D(20, 80), true, false);
        assertEquals(0.0, p2.x(), 1e-9);
        assertEquals(80.0, p2.y(), 1e-9);
    }

    @Test
    public void testCleanResetAndCancellation() {
        Document document = new Document();
        LineTool lineTool = new LineTool();
        RectTool rectTool = new RectTool();
        CameraTransform camera = new CameraTransform();

        // Clique 1 na linha
        lineTool.onMousePressed(createDummyMouseEvent(MouseButton.PRIMARY, false), new Point2D(10, 10), document, camera);
        // Clique com botão direito (SECONDARY) cancela a ação
        lineTool.onMousePressed(createDummyMouseEvent(MouseButton.SECONDARY, false), new Point2D(20, 20), document, camera);
        assertEquals(0, document.getElements().size());

        // Clique 1 no retângulo
        rectTool.onMousePressed(createDummyMouseEvent(MouseButton.PRIMARY, false), new Point2D(10, 10), document, camera);
        // Reset explícito
        rectTool.reset();
        // Próximo clique volta a ser 1º clique
        rectTool.onMousePressed(createDummyMouseEvent(MouseButton.PRIMARY, false), new Point2D(20, 20), document, camera);
        assertEquals(0, document.getElements().size());
    }

    @Test
    public void testDimensionToolLinearCreationAndFormattedText() {
        Document doc = new Document();
        DimensionTool dimTool = new DimensionTool(DimensionElement.DimensionType.LINEAR);
        CameraTransform camera = new CameraTransform();

        // 1º clique: P1 (0, 0)
        dimTool.onMousePressed(createDummyMouseEvent(MouseButton.PRIMARY, false), new Point2D(0, 0), doc, camera);
        // 2º clique: P2 (100, 0)
        dimTool.onMousePressed(createDummyMouseEvent(MouseButton.PRIMARY, false), new Point2D(100, 0), doc, camera);
        // 3º clique: Offset (50, 10)
        dimTool.onMousePressed(createDummyMouseEvent(MouseButton.PRIMARY, false), new Point2D(50, 10), doc, camera);

        assertEquals(1, doc.getElements().size());
        assertTrue(doc.getElements().getFirst() instanceof DimensionElement);

        DimensionElement dim = (DimensionElement) doc.getElements().getFirst();
        assertEquals(DimensionElement.DimensionType.LINEAR, dim.type());
        assertEquals("100.00 mm", dim.formattedText());
        assertEquals(10.0, dim.offsetMm(), 1e-9);
    }

    @Test
    public void testDimensionToolRadiusAndCalloutNote() {
        DimensionElement dimRadius = new DimensionElement("layer1", new Point2D(0, 0), new Point2D(5.0, 0), DimensionElement.DimensionType.RADIUS, 5.0);
        assertEquals("R 5.00 mm", dimRadius.formattedText());

        DimensionElement dimCallout = new DimensionElement("layer1", new Point2D(0, 0), new Point2D(10, 10), DimensionElement.DimensionType.CALLOUT_NOTE, 0.0, "Couro Bovino 1.5mm");
        assertEquals("Couro Bovino 1.5mm", dimCallout.formattedText());
    }
}
