package com.leathercad.ui;

import com.leathercad.core.materials.LeatherMaterial;
import com.leathercad.core.materials.MaterialLibrary;
import com.leathercad.core.model.Document;
import com.leathercad.core.model.Layer;
import com.leathercad.ui.viewport.CanvasViewport;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MaterialManagerDialog {

    public static void showDialog(Stage owner, Document document, MaterialLibrary library, CanvasViewport viewport) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("Biblioteca de Materiais de Couro");

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));
        root.setStyle("-fx-background-color: #1E1E1E;");

        // Lista de Materiais à Esquerda
        ListView<LeatherMaterial> listView = new ListView<>(FXCollections.observableArrayList(library.getAllMaterials()));
        listView.setPrefWidth(220);

        // Painel de Detalhes Técnicos à Direita
        VBox detailsBox = new VBox(10);
        detailsBox.setPadding(new Insets(10, 15, 10, 15));
        detailsBox.setStyle("-fx-background-color: #252526; -fx-border-color: #333333;");

        Label nameLabel = new Label("Selecione um material...");
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #00A8FF;");

        Rectangle colorSwatch = new Rectangle(60, 25);
        colorSwatch.setArcWidth(6);
        colorSwatch.setArcHeight(6);
        colorSwatch.setStroke(Color.WHITE);

        Label thickLabel = new Label("Espessura: -");
        Label weightLabel = new Label("Gramatura: -");
        Label elasticityLabel = new Label("Elasticidade: -");
        Label shrinkageLabel = new Label("Encolhimento: -");
        Label foldLabel = new Label("Fator de Dobra: -");
        Label mfrLabel = new Label("Fabricante: -");

        detailsBox.getChildren().addAll(
            nameLabel,
            new HBox(10, new Label("Amostra de Cor:"), colorSwatch),
            thickLabel, weightLabel, elasticityLabel, shrinkageLabel, foldLabel, mfrLabel
        );

        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                nameLabel.setText(newV.name());
                colorSwatch.setFill(Color.web(newV.colorHex()));
                thickLabel.setText(String.format("Espessura: %.1f mm", newV.thicknessMm()));
                weightLabel.setText(String.format("Gramatura: %.0f g/m²", newV.weightGsm()));
                elasticityLabel.setText(String.format("Elasticidade: %.2f", newV.elasticityFactor()));
                shrinkageLabel.setText(String.format("Encolhimento: %.1f%%", newV.shrinkagePercent()));
                foldLabel.setText(String.format("Fator de Dobra: %.2f", newV.foldFactor()));
                mfrLabel.setText("Fabricante: " + newV.manufacturer());
            }
        });

        listView.getSelectionModel().selectFirst();

        // Botões de Ação
        Button assignBtn = new Button("🎯 Atribuir à Camada Ativa");
        assignBtn.setStyle("-fx-background-color: #00FF88; -fx-text-fill: black; -fx-font-weight: bold;");
        assignBtn.setOnAction(e -> {
            LeatherMaterial selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Layer activeLayer = document.getActiveLayer();
                activeLayer.setMaterial(selected);
                activeLayer.setColorHex(selected.colorHex());
                viewport.redraw();
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Material '" + selected.name() + "' atribuído à camada '" + activeLayer.getName() + "'!");
                alert.show();
            }
        });

        HBox btnBox = new HBox(10, assignBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(10, 0, 0, 0));

        root.setLeft(listView);
        root.setCenter(detailsBox);
        root.setBottom(btnBox);

        Scene scene = new Scene(root, 580, 360);
        scene.getStylesheets().add(MaterialManagerDialog.class.getResource("/styles/dark-theme.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
