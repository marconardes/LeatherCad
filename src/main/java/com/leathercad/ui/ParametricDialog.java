package com.leathercad.ui;

import com.leathercad.core.model.Document;
import com.leathercad.core.parametric.*;
import com.leathercad.ui.viewport.CanvasViewport;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

public class ParametricDialog {

    public static void showDialog(Stage owner, Document document, CanvasViewport viewport) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("📐 Gerador Inteligente de Carteiras & Moldes Paramétricos");
        dialog.setResizable(false);

        VBox mainLayout = new VBox(12);
        mainLayout.setStyle("-fx-background-color: #141414;");
        mainLayout.setPadding(new Insets(18));

        Label headerLabel = new Label("Gerador Paramétrico 2D com Live Preview");
        headerLabel.setStyle("-fx-text-fill: #00FF88; -fx-font-size: 15px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);

        // 1. Seleção do Modelo Paramétrico (via ParametricRegistry)
        List<ParametricTemplate> templates = ParametricRegistry.getInstance().getAllTemplates();
        ComboBox<ParametricTemplate> modelCombo = new ComboBox<>();
        modelCombo.getItems().addAll(templates);
        if (!templates.isEmpty()) modelCombo.setValue(templates.get(0));

        modelCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ParametricTemplate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        });

        modelCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(ParametricTemplate item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getName());
            }
        });

        // 2. Espessura do Couro
        Spinner<Double> thicknessSpinner = new Spinner<>(0.6, 4.0, 1.4, 0.1);
        thicknessSpinner.setEditable(true);

        // 3. Número de Cartões
        Spinner<Integer> cardsSpinner = new Spinner<>(2, 16, 6, 2);

        // 4. Dimensões: Largura Fechada & Altura
        Spinner<Double> widthSpinner = new Spinner<>(70.0, 200.0, 105.0, 5.0);
        widthSpinner.setEditable(true);

        Spinner<Double> heightSpinner = new Spinner<>(60.0, 200.0, 85.0, 5.0);
        heightSpinner.setEditable(true);

        // 5. Parâmetros de Costura: Margem & Passo (Pitch)
        Spinner<Double> marginSpinner = new Spinner<>(1.0, 10.0, 3.85, 0.1);
        marginSpinner.setEditable(true);

        ComboBox<String> pitchCombo = new ComboBox<>();
        pitchCombo.getItems().addAll("#7 (3.85 mm)", "#8 (3.38 mm)", "#6 (4.00 mm)", "#10 (2.70 mm)");
        pitchCombo.setValue("#7 (3.85 mm)");

        // 6. Checkbox de Cotas Técnicas
        CheckBox showDimensionsCheck = new CheckBox("Exibir Cotas Técnicas no Canvas (ANNOTATION_LAYER)");
        showDimensionsCheck.setSelected(true);
        showDimensionsCheck.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        // Função de Atualização Reativa (Live Preview no Canvas2D)
        Runnable updateLivePreview = () -> {
            ParametricTemplate selectedTemplate = modelCombo.getValue();
            if (selectedTemplate == null) return;

            VariableTable vt = selectedTemplate.createDefaultVariables();
            vt.updateVariableValue("width", widthSpinner.getValue());
            vt.updateVariableValue("height", heightSpinner.getValue());
            vt.updateVariableValue("leather_thickness", thicknessSpinner.getValue());
            vt.updateVariableValue("card_slots", cardsSpinner.getValue());
            vt.updateVariableValue("margin", marginSpinner.getValue());

            double pitchVal = 3.85;
            String pitchTxt = pitchCombo.getValue();
            if (pitchTxt.contains("3.38")) pitchVal = 3.38;
            else if (pitchTxt.contains("4.00")) pitchVal = 4.00;
            else if (pitchTxt.contains("2.70")) pitchVal = 2.70;
            vt.updateVariableValue("pitch", pitchVal);

            selectedTemplate.generate(document, vt, showDimensionsCheck.isSelected());
            viewport.redraw();
        };

        // Listeners para todos os seletores acionarem o Live Preview instantâneo
        modelCombo.valueProperty().addListener((obs, o, n) -> updateLivePreview.run());
        thicknessSpinner.valueProperty().addListener((obs, o, n) -> updateLivePreview.run());
        cardsSpinner.valueProperty().addListener((obs, o, n) -> updateLivePreview.run());
        widthSpinner.valueProperty().addListener((obs, o, n) -> updateLivePreview.run());
        heightSpinner.valueProperty().addListener((obs, o, n) -> updateLivePreview.run());
        marginSpinner.valueProperty().addListener((obs, o, n) -> updateLivePreview.run());
        pitchCombo.valueProperty().addListener((obs, o, n) -> updateLivePreview.run());
        showDimensionsCheck.setOnAction(e -> updateLivePreview.run());

        // Adicionar campos ao Grid
        int row = 0;
        grid.add(new Label("Modelo de Artigo:"), 0, row);
        grid.add(modelCombo, 1, row++);

        grid.add(new Label("Espessura do Couro (mm):"), 0, row);
        grid.add(thicknessSpinner, 1, row++);

        grid.add(new Label("Quantidade de Cartões:"), 0, row);
        grid.add(cardsSpinner, 1, row++);

        grid.add(new Label("Largura (mm):"), 0, row);
        grid.add(widthSpinner, 1, row++);

        grid.add(new Label("Altura (mm):"), 0, row);
        grid.add(heightSpinner, 1, row++);

        grid.add(new Label("Margem de Costura (mm):"), 0, row);
        grid.add(marginSpinner, 1, row++);

        grid.add(new Label("Passo de Garfo (Pitch):"), 0, row);
        grid.add(pitchCombo, 1, row++);

        grid.getChildren().forEach(n -> {
            if (n instanceof Label l) l.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        });

        // Executar primeiro Live Preview
        updateLivePreview.run();

        // Botões de Ação
        Button cancelBtn = new Button("Cancelar");
        cancelBtn.setStyle("-fx-background-color: #333; -fx-text-fill: white;");
        cancelBtn.setOnAction(e -> dialog.close());

        Button generateBtn = new Button("🚀 Confirmar Molde");
        generateBtn.setStyle("-fx-background-color: #00A8FF; -fx-text-fill: white; -fx-font-weight: bold;");
        generateBtn.setOnAction(e -> {
            updateLivePreview.run();
            dialog.close();
        });

        HBox btnBox = new HBox(10, cancelBtn, generateBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        mainLayout.getChildren().addAll(headerLabel, grid, showDimensionsCheck, btnBox);

        Scene scene = new Scene(mainLayout, 460, 390);
        try {
            scene.getStylesheets().add(ParametricDialog.class.getResource("/styles/dark-theme.css").toExternalForm());
        } catch (Exception ignored) {}

        dialog.setScene(scene);
        dialog.show();
    }
}
