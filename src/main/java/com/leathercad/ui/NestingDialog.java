package com.leathercad.ui;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.model.CADElement;
import com.leathercad.core.model.Document;
import com.leathercad.core.model.RectElement;
import com.leathercad.core.nesting.*;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class NestingDialog {

    private static NestingResult currentResult;

    public static void showDialog(Stage owner, Document document) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("🧩 Estúdio de Corte Inteligente (Nesting)");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #141414;");
        root.setPadding(new Insets(15));

        // Controles de Configuração do Nesting
        ComboBox<HideSheet> sheetCombo = new ComboBox<>();
        sheetCombo.getItems().addAll(
            HideSheet.FULL_HIDE_1000x800,
            HideSheet.SHEET_A3,
            HideSheet.SHEET_A4
        );
        sheetCombo.setValue(HideSheet.FULL_HIDE_1000x800);

        CheckBox rotateCheck = new CheckBox("Permitir Rotação 90°");
        rotateCheck.setSelected(true);
        rotateCheck.setStyle("-fx-text-fill: white;");

        Button runBtn = new Button("🧩 Executar Nesting");
        runBtn.setStyle("-fx-background-color: #00A8FF; -fx-text-fill: white; -fx-font-weight: bold;");

        HBox controlBox = new HBox(10, new Label("Tamanho da Pele/Placa:"), sheetCombo, rotateCheck, runBtn);
        controlBox.setAlignment(Pos.CENTER_LEFT);
        controlBox.setPadding(new Insets(0, 0, 15, 0));
        ((Label) controlBox.getChildren().get(0)).setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        Canvas nestingCanvas = new Canvas(750, 450);
        TextArea statsText = new TextArea();
        statsText.setEditable(false);
        statsText.setPrefRowCount(4);
        statsText.setStyle("-fx-control-inner-background: #1E1E1E; -fx-text-fill: #00FF88; -fx-font-family: monospace;");

        runBtn.setOnAction(e -> {
            HideSheet selectedSheet = sheetCombo.getValue();
            currentResult = NestingOptimizer.optimizeLayout(document, selectedSheet, rotateCheck.isSelected());
            drawNestingCanvas(nestingCanvas, currentResult);
            statsText.setText(currentResult.formattedSummary());
        });

        // Rodar nesting inicial
        currentResult = NestingOptimizer.optimizeLayout(document, sheetCombo.getValue(), rotateCheck.isSelected());
        drawNestingCanvas(nestingCanvas, currentResult);
        statsText.setText(currentResult.formattedSummary());

        Button applyBtn = new Button("✂️ Aplicar Nesting ao Canvas");
        applyBtn.setStyle("-fx-background-color: #2ED573; -fx-text-fill: white; -fx-font-weight: bold;");
        applyBtn.setOnAction(e -> {
            if (currentResult != null) {
                List<CADElement> newElements = NestingOptimizer.applyNestingToDocument(document, currentResult);
                document.clear();
                for (CADElement elem : newElements) {
                    document.addElement(elem);
                }
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Plano de corte e costuras aplicados com sucesso ao Canvas!");
                alert.show();
                dialog.close();
            }
        });

        HBox bottomBox = new HBox(15, statsText, applyBtn);
        bottomBox.setAlignment(Pos.CENTER_RIGHT);
        bottomBox.setPadding(new Insets(10, 0, 0, 0));

        root.setTop(controlBox);
        root.setCenter(nestingCanvas);
        root.setBottom(bottomBox);

        Scene scene = new Scene(root, 800, 620);
        scene.getStylesheets().add(NestingDialog.class.getResource("/styles/dark-theme.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private static void drawNestingCanvas(Canvas canvas, NestingResult result) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.setFill(Color.web("#141414"));
        gc.fillRect(0, 0, w, h);

        if (result == null) return;

        HideSheet sheet = result.hideSheet();
        double margin = 30.0;
        double scale = Math.min((w - margin * 2) / sheet.widthMm(), (h - margin * 2) / sheet.heightMm());

        double sheetW = sheet.widthMm() * scale;
        double sheetH = sheet.heightMm() * scale;
        double startX = (w - sheetW) / 2.0;
        double startY = (h - sheetH) / 2.0;

        // Desenha a Chapa/Pele de Couro
        gc.setFill(Color.web("#2B2B2B"));
        gc.fillRect(startX, startY, sheetW, sheetH);
        gc.setStroke(Color.web("#00A8FF"));
        gc.setLineWidth(2.0);
        gc.strokeRect(startX, startY, sheetW, sheetH);

        // Desenha as peças encaixadas com margem de corte
        for (NestingPiece piece : result.placedPieces()) {
            if (!piece.isPlaced()) continue;

            double px = startX + piece.placedX() * scale;
            double py = startY + piece.placedY() * scale;
            double pw = piece.currentWidth() * scale;
            double ph = piece.currentHeight() * scale;

            gc.setFill(Color.web("#8B4513", 0.70));
            gc.fillRect(px, py, pw, ph);

            gc.setStroke(Color.web("#FFD700"));
            gc.setLineWidth(1.5);
            gc.strokeRect(px, py, pw, ph);

            // Rótulo da peça
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Consolas", 10));
            gc.fillText(String.format("%.0fx%.0fm", piece.currentWidth(), piece.currentHeight()), px + 4, py + 14);
        }
    }
}
