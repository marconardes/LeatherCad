package com.leathercad.ui;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.leather.CreaseElement;
import com.leathercad.core.leather.StitchElement;
import com.leathercad.core.model.*;
import com.leathercad.ui.viewport.CanvasViewport;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ModelTreeViewPanel extends BorderPane {

    private final Document document;
    private final CanvasViewport viewport;
    private final TreeView<TreeItemData> treeView;
    private boolean isUpdatingSelection = false;

    public static record TreeItemData(String label, String elementId, SubElementRef subRef) {
        @Override
        public String toString() {
            return label;
        }
    }

    public ModelTreeViewPanel(Document document, CanvasViewport viewport) {
        this.document = document;
        this.viewport = viewport;

        setPadding(new Insets(10));
        setStyle("-fx-background-color: #252526;");

        Label title = new Label("🌳 Árvore de Elementos (FreeCAD B-Rep)");
        title.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");

        Button refreshBtn = new Button("🔄 Atualizar");
        refreshBtn.setStyle("-fx-background-color: #333333; -fx-text-fill: #E0E0E0; -fx-font-size: 11px;");
        refreshBtn.setOnAction(e -> rebuildTree());

        HBox topBox = new HBox(10, title, refreshBtn);
        topBox.setPadding(new Insets(0, 0, 10, 0));
        setTop(topBox);

        TreeItem<TreeItemData> rootItem = new TreeItem<>(new TreeItemData("📄 Projeto LeatherCAD", null, null));
        rootItem.setExpanded(true);

        treeView = new TreeView<>(rootItem);
        treeView.setShowRoot(true);
        treeView.setStyle("-fx-background-color: #1E1E1E; -fx-text-fill: #CCCCCC;");

        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (isUpdatingSelection || newVal == null) return;
            TreeItemData data = newVal.getValue();
            if (data == null) return;

            if (data.subRef() != null) {
                document.selectSubElement(data.subRef(), false);
            } else if (data.elementId() != null) {
                document.selectElement(data.elementId(), false);
            }
            viewport.redraw();
        });

        setCenter(treeView);
        document.addOnDocumentChangedListener(this::rebuildTree);
        rebuildTree();
    }

    public void rebuildTree() {
        isUpdatingSelection = true;
        TreeItem<TreeItemData> root = treeView.getRoot();
        root.getChildren().clear();

        for (Layer layer : document.getLayers()) {
            TreeItem<TreeItemData> layerItem = new TreeItem<>(new TreeItemData("📂 " + layer.getName(), null, null));
            layerItem.setExpanded(true);

            for (CADElement elem : document.getElements()) {
                if (!elem.layerId().equals(layer.getId())) continue;
                if (document.getParentId(elem.id()) != null) continue; // Filhos são exibidos aninhados no pai

                TreeItem<TreeItemData> elemItem = buildElementTreeItem(elem);
                layerItem.getChildren().add(elemItem);
            }

            if (!layerItem.getChildren().isEmpty()) {
                root.getChildren().add(layerItem);
            }
        }
        isUpdatingSelection = false;
    }

    private TreeItem<TreeItemData> buildElementTreeItem(CADElement elem) {
        String elemLabel = formatElementLabel(elem);
        TreeItem<TreeItemData> elemItem = new TreeItem<>(new TreeItemData(elemLabel, elem.id(), null));
        elemItem.setExpanded(true);

        if (elem instanceof RectElement rectElem) {
            var rect = rectElem.rect();
            TreeItem<TreeItemData> edge0 = new TreeItem<>(new TreeItemData("── Aresta 0 (Superior: " + String.format("%.1f", rect.width()) + "mm)", elem.id(), new SubElementRef(elem.id(), SubElementRef.SubElementType.EDGE, 0)));
            TreeItem<TreeItemData> edge1 = new TreeItem<>(new TreeItemData("── Aresta 1 (Direita: " + String.format("%.1f", rect.height()) + "mm)", elem.id(), new SubElementRef(elem.id(), SubElementRef.SubElementType.EDGE, 1)));
            TreeItem<TreeItemData> edge2 = new TreeItem<>(new TreeItemData("── Aresta 2 (Inferior: " + String.format("%.1f", rect.width()) + "mm)", elem.id(), new SubElementRef(elem.id(), SubElementRef.SubElementType.EDGE, 2)));
            TreeItem<TreeItemData> edge3 = new TreeItem<>(new TreeItemData("── Aresta 3 (Esquerda: " + String.format("%.1f", rect.height()) + "mm)", elem.id(), new SubElementRef(elem.id(), SubElementRef.SubElementType.EDGE, 3)));

            TreeItem<TreeItemData> v0 = new TreeItem<>(new TreeItemData("• Vértice 0 (TL)", elem.id(), new SubElementRef(elem.id(), SubElementRef.SubElementType.VERTEX, 0)));
            TreeItem<TreeItemData> v1 = new TreeItem<>(new TreeItemData("• Vértice 1 (TR)", elem.id(), new SubElementRef(elem.id(), SubElementRef.SubElementType.VERTEX, 1)));
            TreeItem<TreeItemData> v2 = new TreeItem<>(new TreeItemData("• Vértice 2 (BR)", elem.id(), new SubElementRef(elem.id(), SubElementRef.SubElementType.VERTEX, 2)));
            TreeItem<TreeItemData> v3 = new TreeItem<>(new TreeItemData("• Vértice 3 (BL)", elem.id(), new SubElementRef(elem.id(), SubElementRef.SubElementType.VERTEX, 3)));

            elemItem.getChildren().addAll(edge0, edge1, edge2, edge3, v0, v1, v2, v3);
        } else if (elem instanceof PolylineElement polyElem) {
            var poly = polyElem.polyline();
            var pts = poly.points();
            int numEdges = poly.isClosed() ? pts.size() : pts.size() - 1;

            for (int i = 0; i < numEdges; i++) {
                double len = poly.getSegmentLength(i);
                elemItem.getChildren().add(new TreeItem<>(new TreeItemData("── Aresta " + i + " (Comprimento: " + String.format("%.1f", len) + "mm)", elem.id(), new SubElementRef(elem.id(), SubElementRef.SubElementType.EDGE, i))));
            }

            for (int i = 0; i < pts.size(); i++) {
                Point2D p = pts.get(i);
                double angle = poly.getVertexAngleDegrees(i);
                String angleStr = (angle > 0.01) ? " | Ângulo: " + String.format("%.1f°", angle) : "";
                elemItem.getChildren().add(new TreeItem<>(new TreeItemData("• Vértice " + i + " (" + String.format("%.1f, %.1f", p.x(), p.y()) + " mm" + angleStr + ")", elem.id(), new SubElementRef(elem.id(), SubElementRef.SubElementType.EDGE, i))));
            }
        }

        for (String childId : document.getChildrenIds(elem.id())) {
            CADElement childElem = document.findElementById(childId);
            if (childElem != null) {
                TreeItem<TreeItemData> childItem = buildElementTreeItem(childElem);
                elemItem.getChildren().add(childItem);
            }
        }

        return elemItem;
    }

    private String formatElementLabel(CADElement elem) {
        if (elem instanceof RectElement r) {
            return "🟦 Retângulo (" + String.format("%.1fx%.1f mm", r.rect().width(), r.rect().height()) + ")";
        } else if (elem instanceof PolylineElement p) {
            return "📐 Polilinha (" + p.polyline().points().size() + " pontos)";
        } else if (elem instanceof LineElement l) {
            return "📏 Linha (" + String.format("%.1f mm", l.line().length()) + ")";
        } else if (elem instanceof StitchElement s) {
            return "🧵 Costura (" + s.holePoints().size() + " furos)";
        } else if (elem instanceof CreaseElement c) {
            return "🟧 Vinco Perimetral";
        } else if (elem instanceof CircleElement circ) {
            return "⭕ Círculo (R=" + String.format("%.1f", circ.circle().radius()) + "mm)";
        } else if (elem instanceof ArcElement arc) {
            return "🌙 Arco (R=" + String.format("%.1f", arc.arc().radius()) + "mm)";
        } else if (elem instanceof DimensionElement dim) {
            return "📏 Cota (" + dim.formattedText() + ")";
        }
        return "🔹 " + elem.getClass().getSimpleName();
    }
}
