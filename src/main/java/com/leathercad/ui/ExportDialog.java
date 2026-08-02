package com.leathercad.ui;

import com.leathercad.core.export.DXFExporter;
import com.leathercad.core.export.PNGExporter;
import com.leathercad.core.export.SVGExporter;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.model.CADElement;
import com.leathercad.core.model.Document;
import com.leathercad.core.model.Layer;
import com.leathercad.core.print.PrintEngine;
import com.leathercad.core.print.PrintPDFExporter;
import com.leathercad.core.print.PrintPaperSize;
import com.leathercad.core.print.PrintTile;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExportDialog {

    public enum ExportFormat {
        PDF("📄 PDF Vetorial 1:1 (Mosaico/Tiling)", ".pdf", "Arquivos PDF (*.pdf)"),
        DXF("📐 DXF Industrial (Corte Laser/CNC)", ".dxf", "Arquivos AutoCAD DXF (*.dxf)"),
        SVG("🎨 SVG Vetorial", ".svg", "Arquivos SVG (*.svg)"),
        PNG("🖼️ Imagem PNG (Gabarito Visual)", ".png", "Arquivos PNG (*.png)");

        private final String label;
        private final String extension;
        private final String filterDescription;

        ExportFormat(String label, String extension, String filterDescription) {
            this.label = label;
            this.extension = extension;
            this.filterDescription = filterDescription;
        }

        public String getLabel() { return label; }
        public String getExtension() { return extension; }
        public String getFilterDescription() { return filterDescription; }
    }

    public static void showDialog(Stage owner, Document document) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("📦 Exportar Moldes 1:1 & Arquivos CAD");
        dialog.setResizable(false);

        VBox mainLayout = new VBox(15);
        mainLayout.setStyle("-fx-background-color: #141414;");
        mainLayout.setPadding(new Insets(20));

        // --- TITULO E INSTRUÇÕES ---
        Label titleLabel = new Label("Exportação de Moldes e Projetos");
        titleLabel.setStyle("-fx-text-fill: #00FF88; -fx-font-size: 16px; -fx-font-weight: bold;");

        // --- 1. SELEÇÃO DE FORMATO DE ARQUIVO ---
        VBox formatBox = new VBox(8);
        Label formatLabel = new Label("1. Escolha o Formato de Exportação:");
        formatLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        ToggleGroup formatGroup = new ToggleGroup();
        RadioButton pdfRadio = new RadioButton(ExportFormat.PDF.getLabel());
        RadioButton dxfRadio = new RadioButton(ExportFormat.DXF.getLabel());
        RadioButton svgRadio = new RadioButton(ExportFormat.SVG.getLabel());
        RadioButton pngRadio = new RadioButton(ExportFormat.PNG.getLabel());

        pdfRadio.setToggleGroup(formatGroup);
        dxfRadio.setToggleGroup(formatGroup);
        svgRadio.setToggleGroup(formatGroup);
        pngRadio.setToggleGroup(formatGroup);
        pdfRadio.setSelected(true);

        List.of(pdfRadio, dxfRadio, svgRadio, pngRadio).forEach(rb -> rb.setStyle("-fx-text-fill: white; -fx-font-size: 13px;"));
        formatBox.getChildren().addAll(formatLabel, pdfRadio, dxfRadio, svgRadio, pngRadio);

        // --- 2. CONFIGURAÇÕES DE PAPEL E PAGINAÇÃO (Visível apenas para PDF) ---
        VBox pdfOptionsBox = new VBox(10);
        pdfOptionsBox.setStyle("-fx-background-color: #1E1E1E; -fx-background-radius: 6px; -fx-border-color: #333; -fx-border-radius: 6px;");
        pdfOptionsBox.setPadding(new Insets(12));

        Label pdfTitle = new Label("Configurações do PDF 1:1:");
        pdfTitle.setStyle("-fx-text-fill: #00A8FF; -fx-font-weight: bold;");

        ComboBox<PrintPaperSize> paperCombo = new ComboBox<>();
        paperCombo.getItems().addAll(PrintPaperSize.A4, PrintPaperSize.A3, PrintPaperSize.LETTER);
        paperCombo.setValue(PrintPaperSize.A4);

        RadioButton portraitRadio = new RadioButton("Retrato (Portrait)");
        RadioButton landscapeRadio = new RadioButton("Paisagem (Landscape)");
        ToggleGroup orientGroup = new ToggleGroup();
        portraitRadio.setToggleGroup(orientGroup);
        landscapeRadio.setToggleGroup(orientGroup);
        portraitRadio.setSelected(true);
        portraitRadio.setStyle("-fx-text-fill: white;");
        landscapeRadio.setStyle("-fx-text-fill: white;");

        HBox paperHBox = new HBox(12, new Label("Papel:"), paperCombo, portraitRadio, landscapeRadio);
        paperHBox.setAlignment(Pos.CENTER_LEFT);
        paperHBox.getChildren().forEach(n -> { if (n instanceof Label l) l.setStyle("-fx-text-fill: white;"); });

        Spinner<Double> marginSpinner = new Spinner<>(0.0, 30.0, 10.0, 1.0);
        marginSpinner.setPrefWidth(75);
        Spinner<Double> overlapSpinner = new Spinner<>(0.0, 30.0, 10.0, 2.0);
        overlapSpinner.setPrefWidth(75);

        HBox marginHBox = new HBox(15,
            new Label("Margem (mm):"), marginSpinner,
            new Label("Sobreposição (mm):"), overlapSpinner
        );
        marginHBox.setAlignment(Pos.CENTER_LEFT);
        marginHBox.getChildren().forEach(n -> { if (n instanceof Label l) l.setStyle("-fx-text-fill: white;"); });

        pdfOptionsBox.getChildren().addAll(pdfTitle, paperHBox, marginHBox);

        // --- 3. SELEÇÃO DE CAMADAS (LAYERS) A EXPORTAR ---
        VBox layersBox = new VBox(8);
        Label layersLabel = new Label("2. Seleção de Camadas (Layers):");
        layersLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        VBox layersListVBox = new VBox(6);
        layersListVBox.setPadding(new Insets(5));
        Map<String, CheckBox> layerCheckMap = new HashMap<>();

        for (Layer layer : document.getLayers()) {
            CheckBox cb = new CheckBox(layer.getName() + (layer.isLocked() ? " 🔒 (Camada Travada)" : ""));
            cb.setSelected(layer.isVisible());
            cb.setStyle("-fx-text-fill: white;");
            layerCheckMap.put(layer.getId(), cb);
            layersListVBox.getChildren().add(cb);
        }

        Button selectAllBtn = new Button("Marcar Todas");
        Button deselectAllBtn = new Button("Desmarcar Todas");
        selectAllBtn.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-font-size: 11px;");
        deselectAllBtn.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-font-size: 11px;");

        selectAllBtn.setOnAction(e -> layerCheckMap.values().forEach(cb -> cb.setSelected(true)));
        deselectAllBtn.setOnAction(e -> layerCheckMap.values().forEach(cb -> cb.setSelected(false)));

        HBox layerBtnBox = new HBox(10, selectAllBtn, deselectAllBtn);
        layersBox.getChildren().addAll(layersLabel, layersListVBox, layerBtnBox);

        // --- 4. PAINEL DE INFORMAÇÕES E ESTATÍSTICAS ---
        TextArea statsArea = new TextArea();
        statsArea.setEditable(false);
        statsArea.setPrefRowCount(3);
        statsArea.setStyle("-fx-control-inner-background: #1A1A1A; -fx-text-fill: #00FF88; -fx-font-family: monospace;");

        Runnable updateStats = () -> {
            boolean isPdf = pdfRadio.isSelected();
            pdfOptionsBox.setDisable(!isPdf);

            Set<String> activeLayers = new HashSet<>();
            layerCheckMap.forEach((layerId, cb) -> {
                if (cb.isSelected()) activeLayers.add(layerId);
            });

            long elemCount = document.getElements().stream()
                .filter(e -> activeLayers.contains(e.layerId()))
                .count();

            Rect2D bbox = document.getBoundingBox();
            double widthMm = bbox.width();
            double heightMm = bbox.height();

            if (isPdf) {
                PrintPaperSize selectedPaper = paperCombo.getValue().withOrientation(landscapeRadio.isSelected());
                List<PrintTile> tiles = PrintEngine.calculateTiles(document, selectedPaper, marginSpinner.getValue(), overlapSpinner.getValue());

                statsArea.setText(String.format(
                    "📊 RESUMO DA EXPORTAÇÃO (PDF 1:1):\n" +
                    "- Dimensões do Molde: %.1f mm x %.1f mm | Elementos Ativos: %d\n" +
                    "- Total de Páginas Necessárias: %d folha(s) %s (%d colunas x %d linhas)",
                    widthMm, heightMm, elemCount,
                    tiles.size(), selectedPaper.name(),
                    tiles.isEmpty() ? 0 : tiles.getFirst().totalCols(),
                    tiles.isEmpty() ? 0 : tiles.getFirst().totalRows()
                ));
            } else {
                String formatStr = dxfRadio.isSelected() ? "DXF (Laser/CNC)" : (svgRadio.isSelected() ? "SVG Vetorial" : "PNG Imagem");
                statsArea.setText(String.format(
                    "📊 RESUMO DA EXPORTAÇÃO (%s):\n" +
                    "- Dimensões do Molde: %.1f mm x %.1f mm\n" +
                    "- Total de Elementos a Exportar: %d | Camadas Selecionadas: %d/%d",
                    formatStr, widthMm, heightMm, elemCount, activeLayers.size(), document.getLayers().size()
                ));
            }
        };

        // Listeners para atualizar estatísticas dinamicamente
        formatGroup.selectedToggleProperty().addListener((obs, o, n) -> updateStats.run());
        paperCombo.setOnAction(e -> updateStats.run());
        portraitRadio.setOnAction(e -> updateStats.run());
        landscapeRadio.setOnAction(e -> updateStats.run());
        marginSpinner.valueProperty().addListener((obs, o, n) -> updateStats.run());
        overlapSpinner.valueProperty().addListener((obs, o, n) -> updateStats.run());
        layerCheckMap.values().forEach(cb -> cb.setOnAction(e -> updateStats.run()));

        updateStats.run();

        // --- 5. BOTÕES DE AÇÃO ---
        Button cancelBtn = new Button("Cancelar");
        cancelBtn.setStyle("-fx-background-color: #333; -fx-text-fill: white;");
        cancelBtn.setOnAction(e -> dialog.close());

        Button exportBtn = new Button("💾 Gerar e Salvar Arquivo");
        exportBtn.setStyle("-fx-background-color: #00A8FF; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");

        exportBtn.setOnAction(e -> {
            Set<String> activeLayers = new HashSet<>();
            layerCheckMap.forEach((layerId, cb) -> {
                if (cb.isSelected()) activeLayers.add(layerId);
            });

            if (activeLayers.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING, "Selecione pelo menos uma camada para exportar.");
                alert.show();
                return;
            }

            ExportFormat selectedFormat = pdfRadio.isSelected() ? ExportFormat.PDF :
                                         (dxfRadio.isSelected() ? ExportFormat.DXF :
                                         (svgRadio.isSelected() ? ExportFormat.SVG : ExportFormat.PNG));

            FileChooser fc = new FileChooser();
            fc.setTitle("Salvar Molde Exportado");
            fc.setInitialFileName("leathercad_molde" + selectedFormat.getExtension());
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(selectedFormat.getFilterDescription(), "*" + selectedFormat.getExtension()));

            File targetFile = fc.showSaveDialog(dialog);
            if (targetFile != null) {
                try {
                    switch (selectedFormat) {
                        case PDF -> {
                            PrintPaperSize paper = paperCombo.getValue().withOrientation(landscapeRadio.isSelected());
                            PrintPDFExporter.exportMultiPagePDF(document, paper, marginSpinner.getValue(), overlapSpinner.getValue(), activeLayers, targetFile);
                        }
                        case DXF -> DXFExporter.exportToFile(document, activeLayers, targetFile);
                        case SVG -> SVGExporter.exportToFile(document, targetFile);
                        case PNG -> PNGExporter.exportToFile(document, targetFile);
                    }

                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "🎉 Arquivo exportado com sucesso em:\n" + targetFile.getAbsolutePath());
                    alert.showAndWait();
                    dialog.close();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Erro ao exportar arquivo: " + ex.getMessage());
                    alert.show();
                }
            }
        });

        HBox btnBox = new HBox(12, cancelBtn, exportBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        mainLayout.getChildren().addAll(titleLabel, formatBox, pdfOptionsBox, layersBox, statsArea, btnBox);

        Scene scene = new Scene(mainLayout, 560, 620);
        try {
            scene.getStylesheets().add(ExportDialog.class.getResource("/styles/dark-theme.css").toExternalForm());
        } catch (Exception ignored) {}

        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
