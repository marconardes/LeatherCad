package com.leathercad.ui;

import com.leathercad.core.model.Document;
import com.leathercad.ui.tools.FilletTool;
import com.leathercad.ui.tools.ToolManager;
import com.leathercad.ui.viewport.CanvasViewport;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class CornerRadiusDialog {

    public static void showDialog(Stage owner, Document document, CanvasViewport viewport, ToolManager toolManager, double defaultRadius) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Arredondamento de Cantos / Fillet (LeatherCAD)");

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #2D2D30; -fx-text-fill: #FFFFFF;");

        Label titleLabel = new Label("📐 Arredondar Cantos do Molde (Fillet)");
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #FFFFFF;");

        Label descLabel = new Label("Informe o raio R (mm) e escolha os cantos ou use a ferramenta interativa:");
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #CCCCCC;");

        CheckBox cbTL = new CheckBox("Canto Superior Esquerdo (TL)");
        CheckBox cbTR = new CheckBox("Canto Superior Direito (TR)");
        CheckBox cbBR = new CheckBox("Canto Inferior Direito (BR)");
        CheckBox cbBL = new CheckBox("Canto Inferior Esquerdo (BL)");
        cbTL.setStyle("-fx-text-fill: #E0E0E0;");
        cbTR.setStyle("-fx-text-fill: #E0E0E0;");
        cbBR.setStyle("-fx-text-fill: #E0E0E0;");
        cbBL.setStyle("-fx-text-fill: #E0E0E0;");

        boolean hasSubSelection = document != null && !document.getSelectedSubElements().isEmpty();
        if (hasSubSelection) {
            java.util.Set<Integer> edgeIndices = new java.util.HashSet<>();
            java.util.Set<Integer> vertexIndices = new java.util.HashSet<>();
            for (com.leathercad.core.model.SubElementRef s : document.getSelectedSubElements()) {
                if (s.type() == com.leathercad.core.model.SubElementRef.SubElementType.EDGE) edgeIndices.add(s.index());
                if (s.type() == com.leathercad.core.model.SubElementRef.SubElementType.VERTEX) vertexIndices.add(s.index());
            }

            boolean selTL = vertexIndices.contains(0) || (edgeIndices.contains(0) && edgeIndices.contains(3));
            boolean selTR = vertexIndices.contains(1) || (edgeIndices.contains(0) && edgeIndices.contains(1));
            boolean selBR = vertexIndices.contains(2) || (edgeIndices.contains(1) && edgeIndices.contains(2));
            boolean selBL = vertexIndices.contains(3) || (edgeIndices.contains(2) && edgeIndices.contains(3));

            cbTL.setSelected(selTL);
            cbTR.setSelected(selTR);
            cbBR.setSelected(selBR);
            cbBL.setSelected(selBL);
        } else {
            cbTL.setSelected(true);
            cbTR.setSelected(true);
            cbBR.setSelected(true);
            cbBL.setSelected(true);
        }

        HBox radiusBox = new HBox(10);
        radiusBox.setAlignment(Pos.CENTER_LEFT);
        Label radiusLabel = new Label("Raio (mm):");
        radiusLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold;");

        Spinner<Double> radiusSpinner = new Spinner<>(0.5, 100.0, defaultRadius > 0 ? defaultRadius : 5.0, 0.5);
        radiusSpinner.setEditable(true);
        radiusSpinner.setPrefWidth(110);
        radiusBox.getChildren().addAll(radiusLabel, radiusSpinner);

        // Presets de raio em mm para artesanato em couro
        Label presetsLabel = new Label("Presets rápidos:");
        presetsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #AAAAAA;");
        HBox radiusPresetBox = new HBox(6);
        radiusPresetBox.setAlignment(Pos.CENTER_LEFT);

        double[] presets = {3.0, 5.0, 8.0, 10.0, 15.0};
        for (double rVal : presets) {
            Button btnR = new Button(String.format("R%.0f", rVal));
            btnR.setStyle("-fx-background-color: #3E3E42; -fx-text-fill: #FFFFFF; -fx-font-size: 11px; -fx-cursor: hand;");
            btnR.setOnAction(e -> radiusSpinner.getValueFactory().setValue(rVal));
            radiusPresetBox.getChildren().add(btnR);
        }

        HBox cantosPresetBox = new HBox(8);
        cantosPresetBox.setAlignment(Pos.CENTER_LEFT);
        Button btnTop = new Button("Topos (TL+TR)");
        Button btnAll = new Button("Todos os Cantos");
        Button btnBottom = new Button("Bases (BL+BR)");
        String btnStyle = "-fx-background-color: #3E3E42; -fx-text-fill: #FFFFFF; -fx-font-size: 11px;";
        btnTop.setStyle(btnStyle);
        btnAll.setStyle(btnStyle);
        btnBottom.setStyle(btnStyle);

        btnTop.setOnAction(e -> { cbTL.setSelected(true); cbTR.setSelected(true); cbBR.setSelected(false); cbBL.setSelected(false); });
        btnAll.setOnAction(e -> { cbTL.setSelected(true); cbTR.setSelected(true); cbBR.setSelected(true); cbBL.setSelected(true); });
        btnBottom.setOnAction(e -> { cbTL.setSelected(false); cbTR.setSelected(false); cbBR.setSelected(true); cbBL.setSelected(true); });
        cantosPresetBox.getChildren().addAll(btnTop, btnAll, btnBottom);

        Button applyBtn = new Button("✨ Aplicar Seleção (Enter)");
        applyBtn.setDefaultButton(true);
        applyBtn.setStyle("-fx-background-color: #007ACC; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 18; -fx-cursor: hand;");

        Runnable applyAction = () -> {
            double r = radiusSpinner.getValue();
            if (toolManager != null) {
                toolManager.setActiveTool("Arredondar Cantos (Fillet)");
                if (toolManager.getActiveTool() instanceof FilletTool filletTool) {
                    filletTool.setRadiusMm(r);
                }
            }
            if (document != null && !document.getSelectedElementIds().isEmpty()) {
                double rTL = cbTL.isSelected() ? r : 0.0;
                double rTR = cbTR.isSelected() ? r : 0.0;
                double rBR = cbBR.isSelected() ? r : 0.0;
                double rBL = cbBL.isSelected() ? r : 0.0;
                document.setCornerRadiusSelected(rTL, rTR, rBR, rBL);
            }
            if (viewport != null) {
                viewport.redraw();
            }
            dialog.close();
        };

        applyBtn.setOnAction(e -> applyAction.run());

        root.getChildren().addAll(
            titleLabel, descLabel,
            radiusBox, presetsLabel, radiusPresetBox,
            new Separator(),
            cbTL, cbTR, cbBR, cbBL, cantosPresetBox,
            new Separator(), applyBtn
        );

        Scene scene = new Scene(root, 400, 420);
        // Suporte a atalhos: Enter para confirmar, ESC para fechar/cancelar
        scene.addEventFilter(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
            if (event.getCode() == KeyCode.ENTER) {
                applyAction.run();
                event.consume();
            } else if (event.getCode() == KeyCode.ESCAPE) {
                dialog.close();
                event.consume();
            }
        });

        dialog.setScene(scene);
        dialog.showAndWait();
    }

    public static void showDialog(Stage owner, Document document, CanvasViewport viewport, double defaultRadius) {
        showDialog(owner, document, viewport, null, defaultRadius);
    }
}
