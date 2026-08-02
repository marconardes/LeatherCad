package com.leathercad.ui;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.model.CADElement;
import com.leathercad.core.model.Document;
import com.leathercad.core.print.*;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.*;
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

import java.io.File;
import java.util.List;

public class PrintStudioDialog {

    private static PrintPaperSize currentPaper = PrintPaperSize.A4;
    private static boolean isLandscape = false;
    private static double marginMm = 5.0;
    private static double overlapMm = 10.0;

    public static void showDialog(Stage owner, Document document) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("🖨️ Estúdio de Impressão 1:1 & Paginação de Moldes");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #141414;");
        root.setPadding(new Insets(15));

        // Painel Superior de Configurações
        ComboBox<PrintPaperSize> paperCombo = new ComboBox<>();
        paperCombo.getItems().addAll(
            PrintPaperSize.A4,
            PrintPaperSize.A3,
            PrintPaperSize.LETTER,
            PrintPaperSize.A2,
            PrintPaperSize.A1,
            PrintPaperSize.PLOTTER
        );
        paperCombo.setValue(PrintPaperSize.A4);

        RadioButton portraitRadio = new RadioButton("Retrato");
        RadioButton landscapeRadio = new RadioButton("Paisagem");
        ToggleGroup orientGroup = new ToggleGroup();
        portraitRadio.setToggleGroup(orientGroup);
        landscapeRadio.setToggleGroup(orientGroup);
        portraitRadio.setSelected(true);
        portraitRadio.setStyle("-fx-text-fill: white;");
        landscapeRadio.setStyle("-fx-text-fill: white;");

        Spinner<Double> marginSpinner = new Spinner<>(0.0, 30.0, 5.0, 1.0);
        marginSpinner.setPrefWidth(70);

        Spinner<Double> overlapSpinner = new Spinner<>(0.0, 30.0, 10.0, 2.0);
        overlapSpinner.setPrefWidth(70);

        CheckBox cropMarksCheck = new CheckBox("Marcas de Corte");
        cropMarksCheck.setSelected(true);
        cropMarksCheck.setStyle("-fx-text-fill: white;");

        HBox topBox = new HBox(12,
            new Label("Tamanho Papel:"), paperCombo,
            portraitRadio, landscapeRadio,
            new Label("Margem:"), marginSpinner,
            new Label("Sobreposição:"), overlapSpinner,
            cropMarksCheck
        );
        topBox.setAlignment(Pos.CENTER_LEFT);
        topBox.setPadding(new Insets(0, 0, 15, 0));
        topBox.getChildren().forEach(n -> {
            if (n instanceof Label l) l.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        });

        Canvas printCanvas = new Canvas(760, 430);

        TextArea statsText = new TextArea();
        statsText.setEditable(false);
        statsText.setPrefRowCount(3);
        statsText.setStyle("-fx-control-inner-background: #1E1E1E; -fx-text-fill: #00FF88; -fx-font-family: monospace;");

        Runnable updatePreview = () -> {
            currentPaper = paperCombo.getValue().withOrientation(landscapeRadio.isSelected());
            marginMm = marginSpinner.getValue();
            overlapMm = overlapSpinner.getValue();

            List<PrintTile> tiles = PrintEngine.calculateTiles(document, currentPaper, marginMm, overlapMm);
            drawPrintPreview(printCanvas, document, currentPaper, tiles, cropMarksCheck.isSelected());

            if (!tiles.isEmpty()) {
                PrintTile first = tiles.getFirst();
                statsText.setText(String.format(
                    "🎉 PAGINAÇÃO EM ESCALA REAL 1:1 CONCLUÍDA!\n" +
                    "- Total de Páginas Necessárias: %d folhas %s (%d colunas x %d linhas)\n" +
                    "- Margem de Segurança: %.1f mm | Sobreposição de Junção: %.1f mm",
                    tiles.size(), currentPaper.name(), first.totalCols(), first.totalRows(), marginMm, overlapMm
                ));
            } else {
                statsText.setText("Nenhum elemento no documento para impressão.");
            }
        };

        paperCombo.setOnAction(e -> updatePreview.run());
        portraitRadio.setOnAction(e -> updatePreview.run());
        landscapeRadio.setOnAction(e -> updatePreview.run());
        marginSpinner.valueProperty().addListener((obs, o, n) -> updatePreview.run());
        overlapSpinner.valueProperty().addListener((obs, o, n) -> updatePreview.run());
        cropMarksCheck.setOnAction(e -> updatePreview.run());

        updatePreview.run();

        Button printJobBtn = new Button("🖨️ Imprimir Moldes (PrinterJob)");
        printJobBtn.setStyle("-fx-background-color: #00A8FF; -fx-text-fill: white; -fx-font-weight: bold;");
        printJobBtn.setOnAction(e -> {
            PrinterJob job = PrinterJob.createPrinterJob();
            if (job != null && job.showPrintDialog(owner)) {
                boolean success = job.printPage(printCanvas);
                if (success) {
                    job.endJob();
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Impressão de moldes enviada com sucesso à impressora!");
                    alert.show();
                }
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Nenhuma impressora selecionada ou impressão cancelada.");
                alert.show();
            }
        });

        Button pdfBtn = new Button("📄 Exportar Gabarito Multi-Páginas 1:1 (PDF)");
        pdfBtn.setStyle("-fx-background-color: #2ED573; -fx-text-fill: white; -fx-font-weight: bold;");
        pdfBtn.setOnAction(e -> {
            try {
                File file = new File("leathercad_moldes_1to1.pdf");
                PrintPDFExporter.exportMultiPagePDF(document, currentPaper, marginMm, overlapMm, file);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Gabarito multi-páginas 1:1 exportado com sucesso em: " + file.getAbsolutePath());
                alert.show();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        HBox btnBox = new HBox(10, printJobBtn, pdfBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        HBox bottomBox = new HBox(15, statsText, btnBox);
        bottomBox.setAlignment(Pos.CENTER_RIGHT);
        bottomBox.setPadding(new Insets(10, 0, 0, 0));

        root.setTop(topBox);
        root.setCenter(printCanvas);
        root.setBottom(bottomBox);

        Scene scene = new Scene(root, 810, 600);
        scene.getStylesheets().add(PrintStudioDialog.class.getResource("/styles/dark-theme.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private static void drawPrintPreview(Canvas canvas, Document document, PrintPaperSize paper, List<PrintTile> tiles, boolean showCropMarks) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();

        gc.setFill(Color.web("#141414"));
        gc.fillRect(0, 0, w, h);

        if (tiles.isEmpty()) return;

        // Enquadramento da grade de folhas
        PrintTile firstTile = tiles.getFirst();
        double gridW = firstTile.totalCols() * paper.currentWidth();
        double gridH = firstTile.totalRows() * paper.currentHeight();

        double margin = 30.0;
        double scale = Math.min((w - margin * 2) / gridW, (h - margin * 2) / gridH);

        double startX = (w - gridW * scale) / 2.0;
        double startY = (h - gridH * scale) / 2.0;

        // 1. Desenha os elementos do documento em escala de visualização
        gc.setStroke(Color.web("#00FF88"));
        gc.setLineWidth(1.5);
        for (CADElement elem : document.getElements()) {
            Rect2D b = elem.boundingBox();
            double px = startX + (b.minPoint().x() - firstTile.worldBounds().minPoint().x() + 10) * scale;
            double py = startY + (b.minPoint().y() - firstTile.worldBounds().minPoint().y() + 10) * scale;
            double pw = b.width() * scale;
            double ph = b.height() * scale;

            gc.setFill(Color.web("#8B4513", 0.50));
            gc.fillRect(px, py, pw, ph);
            gc.strokeRect(px, py, pw, ph);
        }

        // 2. Desenha a grade de folhas (Tiling Grid)
        for (PrintTile tile : tiles) {
            double sheetX = startX + (tile.colIndex() * paper.currentWidth()) * scale;
            double sheetY = startY + (tile.rowIndex() * paper.currentHeight()) * scale;
            double sheetW = paper.currentWidth() * scale;
            double sheetH = paper.currentHeight() * scale;

            // Borda da folha
            gc.setStroke(Color.web("#00A8FF"));
            gc.setLineWidth(1.5);
            gc.strokeRect(sheetX, sheetY, sheetW, sheetH);

            // Rótulo da folha
            gc.setFill(Color.web("#00A8FF"));
            gc.setFont(javafx.scene.text.Font.font("Consolas", 11));
            gc.fillText(tile.formattedLabel(), sheetX + 8, sheetY + 18);

            // Marcas de Corte nas 4 pontas
            if (showCropMarks) {
                gc.setStroke(Color.web("#FF3366"));
                gc.setLineWidth(2.0);
                double markLen = 8.0;

                // Canto Sup Esq
                gc.strokeLine(sheetX, sheetY, sheetX + markLen, sheetY);
                gc.strokeLine(sheetX, sheetY, sheetX, sheetY + markLen);
                // Canto Sup Dir
                gc.strokeLine(sheetX + sheetW, sheetY, sheetX + sheetW - markLen, sheetY);
                gc.strokeLine(sheetX + sheetW, sheetY, sheetX + sheetW, sheetY + markLen);
                // Canto Inf Esq
                gc.strokeLine(sheetX, sheetY + sheetH, sheetX + markLen, sheetY + sheetH);
                gc.strokeLine(sheetX, sheetY + sheetH, sheetX, sheetY + sheetH - markLen);
                // Canto Inf Dir
                gc.strokeLine(sheetX + sheetW, sheetY + sheetH, sheetX + sheetW - markLen, sheetY + sheetH);
                gc.strokeLine(sheetX + sheetW, sheetY + sheetH, sheetX + sheetW, sheetY + sheetH - markLen);
            }
        }
    }
}
