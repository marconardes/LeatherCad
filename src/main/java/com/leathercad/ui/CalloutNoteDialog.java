package com.leathercad.ui;

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

import java.util.Optional;

public class CalloutNoteDialog {

    public static Optional<String> showDialog(Stage owner, String defaultNote) {
        Stage dialog = new Stage();
        dialog.initOwner(owner);
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.setTitle("📝 Nota de Ficha Técnica (Callout)");

        VBox root = new VBox(14);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #2D2D30; -fx-text-fill: #FFFFFF;");

        Label titleLabel = new Label("📌 Balão de Especificação Técnica");
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #00E676;");

        Label descLabel = new Label("Digite a especificação para o apontador no molde:");
        descLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #CCCCCC;");

        TextField textField = new TextField(defaultNote != null && !defaultNote.isBlank() ? defaultNote : "Couro Bovino 1.5mm");
        textField.setStyle("-fx-background-color: #3E3E42; -fx-text-fill: #FFFFFF; -fx-font-size: 13px;");

        Label presetsLabel = new Label("Sugestões Rápidas de Artesanato:");
        presetsLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #AAAAAA;");

        HBox presetBox1 = new HBox(6);
        presetBox1.setAlignment(Pos.CENTER_LEFT);
        String[] presets1 = {"Couro Bovino 1.5mm", "Costura #7 (3.85mm)", "Vinco Borda 1.5mm"};

        for (String pText : presets1) {
            Button btnP = new Button(pText);
            btnP.setStyle("-fx-background-color: #3E3E42; -fx-text-fill: #FFFFFF; -fx-font-size: 11px; -fx-cursor: hand;");
            btnP.setOnAction(e -> textField.setText(pText));
            presetBox1.getChildren().add(btnP);
        }

        HBox presetBox2 = new HBox(6);
        presetBox2.setAlignment(Pos.CENTER_LEFT);
        String[] presets2 = {"Boleado 2.0mm", "Chanfro 45°", "Refilo / Skiving 0.8mm"};

        for (String pText : presets2) {
            Button btnP = new Button(pText);
            btnP.setStyle("-fx-background-color: #3E3E42; -fx-text-fill: #FFFFFF; -fx-font-size: 11px; -fx-cursor: hand;");
            btnP.setOnAction(e -> textField.setText(pText));
            presetBox2.getChildren().add(btnP);
        }

        Button applyBtn = new Button("✨ Inserir Nota (Enter)");
        applyBtn.setDefaultButton(true);
        applyBtn.setStyle("-fx-background-color: #00E676; -fx-text-fill: #1E1E1E; -fx-font-weight: bold; -fx-padding: 8 18; -fx-cursor: hand;");

        final String[] result = new String[1];

        Runnable applyAction = () -> {
            String text = textField.getText().trim();
            if (!text.isEmpty()) {
                result[0] = text;
            }
            dialog.close();
        };

        applyBtn.setOnAction(e -> applyAction.run());

        root.getChildren().addAll(
            titleLabel, descLabel, textField,
            presetsLabel, presetBox1, presetBox2,
            new Separator(), applyBtn
        );

        Scene scene = new Scene(root, 420, 310);
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

        return Optional.ofNullable(result[0]);
    }
}
