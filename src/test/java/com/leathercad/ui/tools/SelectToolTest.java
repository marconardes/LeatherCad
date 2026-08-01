package com.leathercad.ui.tools;

import com.leathercad.core.geometry.LineSegment;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.model.Document;
import com.leathercad.core.model.LineElement;
import com.leathercad.core.model.RectElement;
import com.leathercad.ui.viewport.CameraTransform;

import javafx.event.EventType;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SelectToolTest {

    private MouseEvent createMouseEvent(EventType<MouseEvent> type, MouseButton button, boolean isShiftDown) {
        return new MouseEvent(
            type, 0, 0, 0, 0, button, 1,
            isShiftDown, false, false, false, false, false, false, false, false, false, null
        );
    }

    @Test
    public void testSingleClickSelection() {
        Document document = new Document();
        RectElement rectElem = new RectElement(document.getActiveLayer().getId(), new Rect2D(10, 10, 50, 50));
        document.addElement(rectElem);

        SelectTool selectTool = new SelectTool();
        CameraTransform camera = new CameraTransform();

        // Clique no corpo do retângulo (20, 20)
        MouseEvent click = createMouseEvent(MouseEvent.MOUSE_PRESSED, MouseButton.PRIMARY, false);
        selectTool.onMousePressed(click, new Point2D(20, 20), document, camera);

        assertEquals(1, document.getSelectedElementIds().size());
        assertTrue(document.getSelectedElementIds().contains(rectElem.id()));
    }

    @Test
    public void testWindowVsCrossingBoxSelection() {
        Document document = new Document();
        // Elemento A: inteiramente dentro de (0,0) a (100,100)
        RectElement elemA = new RectElement(document.getActiveLayer().getId(), new Rect2D(10, 10, 30, 30));
        // Elemento B: cruza a borda X=100 (de 80 a 120)
        RectElement elemB = new RectElement(document.getActiveLayer().getId(), new Rect2D(80, 10, 40, 30));
        document.addElement(elemA);
        document.addElement(elemB);

        SelectTool selectTool = new SelectTool();
        CameraTransform camera = new CameraTransform();

        // 1. Window Selection (Esquerda -> Direita: de (0,0) a (100,100))
        MouseEvent pressW = createMouseEvent(MouseEvent.MOUSE_PRESSED, MouseButton.PRIMARY, false);
        selectTool.onMousePressed(pressW, new Point2D(0, 0), document, camera);
        MouseEvent releaseW = createMouseEvent(MouseEvent.MOUSE_RELEASED, MouseButton.PRIMARY, false);
        selectTool.onMouseReleased(releaseW, new Point2D(100, 100), document, camera);

        // Apenas elemA deve ser selecionado (pois elemB não está 100% contido)
        assertEquals(1, document.getSelectedElementIds().size());
        assertTrue(document.getSelectedElementIds().contains(elemA.id()));

        document.clearSelection();

        // 2. Crossing Selection (Direita -> Esquerda: de (100,100) a (0,0))
        MouseEvent pressC = createMouseEvent(MouseEvent.MOUSE_PRESSED, MouseButton.PRIMARY, false);
        selectTool.onMousePressed(pressC, new Point2D(100, 100), document, camera);
        MouseEvent releaseC = createMouseEvent(MouseEvent.MOUSE_RELEASED, MouseButton.PRIMARY, false);
        selectTool.onMouseReleased(releaseC, new Point2D(0, 0), document, camera);

        // Ambas as peças devem ser selecionadas no modo Crossing
        assertEquals(2, document.getSelectedElementIds().size());
        assertTrue(document.getSelectedElementIds().contains(elemA.id()));
        assertTrue(document.getSelectedElementIds().contains(elemB.id()));
    }

    @Test
    public void testMoveSelectedElementByDrag() {
        Document document = new Document();
        LineElement lineElem = new LineElement(document.getActiveLayer().getId(), new LineSegment(new Point2D(0, 0), new Point2D(50, 0)));
        document.addElement(lineElem);

        SelectTool selectTool = new SelectTool();
        CameraTransform camera = new CameraTransform();

        // Clique para selecionar a linha
        MouseEvent press = createMouseEvent(MouseEvent.MOUSE_PRESSED, MouseButton.PRIMARY, false);
        selectTool.onMousePressed(press, new Point2D(25, 0), document, camera);

        // Arraste de (25, 0) para (25, 30) (deslocamento dy = 30)
        MouseEvent drag = createMouseEvent(MouseEvent.MOUSE_DRAGGED, MouseButton.PRIMARY, false);
        selectTool.onMouseDragged(drag, new Point2D(25, 30), document, camera);

        LineElement moved = (LineElement) document.getElements().getFirst();
        assertEquals(0.0, moved.line().start().x(), 1e-9);
        assertEquals(30.0, moved.line().start().y(), 1e-9);
        assertEquals(50.0, moved.line().end().x(), 1e-9);
        assertEquals(30.0, moved.line().end().y(), 1e-9);
    }

    @Test
    public void testGripNodeDragging() {
        Document document = new Document();
        RectElement rectElem = new RectElement(document.getActiveLayer().getId(), new Rect2D(0, 0, 100, 50));
        document.addElement(rectElem);

        SelectTool selectTool = new SelectTool();
        CameraTransform camera = new CameraTransform();

        // Clique no Grip 0 (Canto superior esquerdo 0, 0)
        MouseEvent pressGrip = createMouseEvent(MouseEvent.MOUSE_PRESSED, MouseButton.PRIMARY, false);
        selectTool.onMousePressed(pressGrip, new Point2D(0, 0), document, camera);

        // Mover o canto para (-10, -10)
        MouseEvent dragGrip = createMouseEvent(MouseEvent.MOUSE_DRAGGED, MouseButton.PRIMARY, false);
        selectTool.onMouseDragged(dragGrip, new Point2D(-10, -10), document, camera);

        RectElement updated = (RectElement) document.getElements().getFirst();
        assertEquals(-10.0, updated.rect().minPoint().x(), 1e-9);
        assertEquals(-10.0, updated.rect().minPoint().y(), 1e-9);
        assertEquals(110.0, updated.rect().width(), 1e-9);
        assertEquals(60.0, updated.rect().height(), 1e-9);
    }

    @Test
    public void testResetClearsState() {
        SelectTool selectTool = new SelectTool();
        selectTool.reset();
        // Garantir que a ferramenta reseta sem exceções
        assertNotNull(selectTool.getName());
    }
}
