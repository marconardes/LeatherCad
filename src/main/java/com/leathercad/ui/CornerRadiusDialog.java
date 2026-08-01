package com.leathercad.ui;

import com.leathercad.core.model.Document;
import com.leathercad.ui.viewport.CanvasViewport;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class CornerRadiusDialog {

    public static void showDialog(Stage owner, Document document, CanvasViewport viewport, double defaultRadius) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("Arredondamento de Cantos de Couro (FreeCAD / AutoCAD)");

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #FFFFFF;");

        Label titleLabel = new Label("📐 Arredondar Cantos do Molde (Preserva o Material)");
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1A1A1A;");

        Label descLabel = new Label("Selecione quais cantos da peça de couro devem ser arredondados:");
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666666;");

        CheckBox cbTL = new CheckBox("Canto Superior Esquerdo (TL)");
        CheckBox cbTR = new CheckBox("Canto Superior Direito (TR)");
        CheckBox cbBR = new CheckBox("Canto Inferior Direito (BR)");
        CheckBox cbBL = new CheckBox("Canto Inferior Esquerdo (BL)");

        cbTL.setSelected(true);
        cbTR.setSelected(true);
        cbBR.setSelected(false);
        cbBL.setSelected(false);

        HBox radiusBox = new HBox(10);
        radiusBox.setAlignment(Pos.CENTER_LEFT);
        Label radiusLabel = new Label("Raio (mm):");
        Spinner<Double> radiusSpinner = new Spinner<>(0.5, 100.0, defaultRadius > 0 ? defaultRadius : 5.0, 1.0);
        radiusSpinner.setEditable(true);
        radiusSpinner.setPrefWidth(100);
        radiusBox.getChildren().addAll(radiusLabel, radiusSpinner);

        HBox presetBox = new HBox(8);
        presetBox.setAlignment(Pos.CENTER_LEFT);
        Button btnTop = new Button("Topos (TL+TR)");
        Button btnAll = new Button("Todos (4 Cantos)");
        Button btnBottom = new Button("Bases (BL+BR)");

        btnTop.setOnAction(e -> { cbTL.setSelected(true); cbTR.setSelected(true); cbBR.setSelected(false); cbBL.setSelected(false); });
        btnAll.setOnAction(e -> { cbTL.setSelected(true); cbTR.setSelected(true); cbBR.setSelected(true); cbBL.setSelected(true); });
        btnBottom.setOnAction(e -> { cbTL.setSelected(false); cbTR.setSelected(false); cbBR.setSelected(true); cbBL.setSelected(true); });
        presetBox.getChildren().addAll(btnTop, btnAll, btnBottom);

        Button applyBtn = new Button("✨ Aplicar ao Molde");
        applyBtn.setStyle("-fx-background-color: #007ACC; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 20;");
        applyBtn.setOnAction(e -> {
            double r = radiusSpinner.getValue();
            double rTL = cbTL.isSelected() ? r : 0.0;
            double rTR = cbTR.isSelected() ? r : 0.0;
            double rBR = cbBR.isSelected() ? r : 0.0;
            double rBL = cbBL.isSelected() ? r : 0.0;

            document.setCornerRadiusSelected(rTL, rTR, rBR, rBL);
            viewport.redraw();
            dialog.close();
        });

        root.getChildren().addAll(titleLabel, descLabel, cbTL, cbTR, cbBR, cbBL, radiusBox, presetBox, applyBtn);
        Scene scene = new Scene(root, 380, 340);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
