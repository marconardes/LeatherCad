package com.leathercad.ui;

import com.leathercad.core.leather.StitchConfig;
import com.leathercad.core.leather.StitchType;
import com.leathercad.ui.tools.StitchTool;
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

public class StitchConfigDialog {

    public static void showDialog(Stage owner, ToolManager toolManager, CanvasViewport viewport) {
        StitchTool stitchTool = null;
        if (toolManager != null && toolManager.getActiveTool() instanceof StitchTool tool) {
            stitchTool = tool;
        }

        StitchConfig currentCfg = (stitchTool != null) ? stitchTool.getConfig() : StitchConfig.DEFAULT;
        double currentOffset = (stitchTool != null) ? stitchTool.getOffsetMm() : 3.85;

        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("⚙️ Configuração de Costura Parametrizada (LeatherCAD)");

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #2D2D30; -fx-text-fill: #FFFFFF;");

        Label titleLabel = new Label("🧵 Parâmetros de Costura de Couro");
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #FFD700;");

        // Tipo de Costura / Garfo
        Label typeLabel = new Label("Tipo de Garfo / Estilo:");
        typeLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold;");

        ComboBox<StitchType> typeBox = new ComboBox<>();
        typeBox.getItems().addAll(StitchType.FRENCH_SLANT, StitchType.ROUND_PUNCH, StitchType.MACHINE_STITCH);
        typeBox.setValue(currentCfg.type() == StitchType.FRENCH || currentCfg.type() == StitchType.EUROPEAN ? StitchType.FRENCH_SLANT : currentCfg.type());
        typeBox.setPrefWidth(260);

        // Presets Rápidos de Pitch
        Label presetsLabel = new Label("Presets Rápidos de Passo / Dente (Pitch):");
        presetsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #AAAAAA;");

        Spinner<Double> pitchSpinner = new Spinner<>(1.0, 20.0, currentCfg.pitchMm(), 0.05);
        pitchSpinner.setEditable(true);
        pitchSpinner.setPrefWidth(100);

        HBox presetBox = new HBox(6);
        presetBox.setAlignment(Pos.CENTER_LEFT);

        double[] pitchPresets = {2.70, 3.00, 3.38, 3.85, 4.00, 5.00};
        String[] presetNames = {"#10 (2.70)", "#9 (3.00)", "#8 (3.38)", "#7 (3.85)", "4.00mm", "5.00mm"};

        for (int i = 0; i < pitchPresets.length; i++) {
            final double pVal = pitchPresets[i];
            Button btnP = new Button(presetNames[i]);
            btnP.setStyle("-fx-background-color: #3E3E42; -fx-text-fill: #FFFFFF; -fx-font-size: 11px; -fx-cursor: hand;");
            btnP.setOnAction(e -> pitchSpinner.getValueFactory().setValue(pVal));
            presetBox.getChildren().add(btnP);
        }

        // Alternância de Ângulo (+45° / -45°)
        Label angleLabel = new Label("Orientação da Fenda:");
        angleLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold;");

        ToggleGroup angleGroup = new ToggleGroup();
        ToggleButton btnAnglePos = new ToggleButton("📐 +45° (Direita)");
        ToggleButton btnAngleNeg = new ToggleButton("📐 -45° (Esquerda)");
        btnAnglePos.setToggleGroup(angleGroup);
        btnAngleNeg.setToggleGroup(angleGroup);

        String toggleStyle = "-fx-background-color: #3E3E42; -fx-text-fill: white; -fx-font-size: 11px; -fx-cursor: hand;";
        btnAnglePos.setStyle(toggleStyle);
        btnAngleNeg.setStyle(toggleStyle);

        if (currentCfg.angleDegrees() < 0) {
            btnAngleNeg.setSelected(true);
        } else {
            btnAnglePos.setSelected(true);
        }

        HBox angleBox = new HBox(10, angleLabel, btnAnglePos, btnAngleNeg);
        angleBox.setAlignment(Pos.CENTER_LEFT);

        // Dimensões do Dente e Vazador
        HBox slotBox = new HBox(10);
        slotBox.setAlignment(Pos.CENTER_LEFT);
        Label slotLabel = new Label("Comprimento Fenda (mm):");
        slotLabel.setStyle("-fx-text-fill: #CCCCCC;");
        Spinner<Double> slotSpinner = new Spinner<>(0.5, 10.0, currentCfg.slotLengthMm(), 0.1);
        slotSpinner.setEditable(true);
        slotSpinner.setPrefWidth(90);
        slotBox.getChildren().addAll(slotLabel, slotSpinner);

        HBox holeBox = new HBox(10);
        holeBox.setAlignment(Pos.CENTER_LEFT);
        Label holeLabel = new Label("Diâmetro Vazador (mm):");
        holeLabel.setStyle("-fx-text-fill: #CCCCCC;");
        Spinner<Double> holeSpinner = new Spinner<>(0.5, 10.0, currentCfg.holeDiameterMm(), 0.1);
        holeSpinner.setEditable(true);
        holeSpinner.setPrefWidth(90);
        holeBox.getChildren().addAll(holeLabel, holeSpinner);

        // Margem de Costura / Offset (mm)
        HBox offsetBox = new HBox(10);
        offsetBox.setAlignment(Pos.CENTER_LEFT);
        Label offsetLabel = new Label("Margem da Borda / Offset (mm):");
        offsetLabel.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold;");
        Spinner<Double> offsetSpinner = new Spinner<>(0.0, 50.0, currentOffset, 0.25);
        offsetSpinner.setEditable(true);
        offsetSpinner.setPrefWidth(100);
        offsetBox.getChildren().addAll(offsetLabel, offsetSpinner);

        Button applyBtn = new Button("✨ Aplicar Configuração (Enter)");
        applyBtn.setDefaultButton(true);
        applyBtn.setStyle("-fx-background-color: #007ACC; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 18; -fx-cursor: hand;");

        final StitchTool finalStitchTool = stitchTool;
        Runnable applyAction = () -> {
            StitchType selType = typeBox.getValue();
            double pitch = pitchSpinner.getValue();
            double angle = btnAngleNeg.isSelected() ? -45.0 : 45.0;
            double slotLen = slotSpinner.getValue();
            double holeDia = holeSpinner.getValue();
            double margin = offsetSpinner.getValue();

            StitchConfig newConfig = new StitchConfig(margin, pitch, holeDia, slotLen, angle, selType);

            if (finalStitchTool != null) {
                finalStitchTool.setConfig(newConfig);
                finalStitchTool.setOffsetMm(margin);
            }

            if (viewport != null) {
                viewport.redraw();
            }
            dialog.close();
        };

        applyBtn.setOnAction(e -> applyAction.run());

        root.getChildren().addAll(
            titleLabel, typeLabel, typeBox,
            presetsLabel, presetBox,
            new Separator(),
            angleBox, slotBox, holeBox, offsetBox,
            new Separator(), applyBtn
        );

        Scene scene = new Scene(root, 440, 480);
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
}
