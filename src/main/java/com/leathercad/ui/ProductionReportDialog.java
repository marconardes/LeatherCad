package com.leathercad.ui;

import com.leathercad.core.model.Document;
import com.leathercad.core.production.*;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.PrintWriter;

public class ProductionReportDialog {

    public static void showDialog(Stage owner, Document document) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("🏭 Ficha Técnica & Relatório de Produção (BOM)");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #141414;");
        root.setPadding(new Insets(15));

        ProductionSummary summary = ProductionCalculator.calculateProduction(document);

        Label title = new Label("🏭 Ficha Técnica de Produção & Consumo de Insumos");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #00A8FF;");

        // Cartões de Resumo de Insumos
        VBox card1 = createSummaryCard("Área Total Couro", String.format("%.2f ft²", summary.totalLeatherSqFt()), "%.1f cm²".formatted(summary.totalLeatherAreaCm2()));
        VBox card2 = createSummaryCard("Consumo de Linha", String.format("%.2f m", summary.totalThreadMeters()), "Fio Encerado");
        VBox card3 = createSummaryCard("Total Ferragens", String.format("%d un", summary.totalHardwareCount()), "Botões / Snaps");
        VBox card4 = createSummaryCard("Tempo de Corte", String.format("%.1f min", summary.estimatedCuttingTimeMinutes()), "Laser / CNC");

        HBox summaryBox = new HBox(15, card1, card2, card3, card4);
        summaryBox.setAlignment(Pos.CENTER);
        summaryBox.setPadding(new Insets(10, 0, 15, 0));

        // Tabela BOMItem
        TableView<BOMItem> table = new TableView<>();
        table.setItems(FXCollections.observableArrayList(summary.items()));

        TableColumn<BOMItem, String> nameCol = new TableColumn<>("Peça");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("pieceName"));

        TableColumn<BOMItem, String> layerCol = new TableColumn<>("Camada");
        layerCol.setCellValueFactory(new PropertyValueFactory<>("layerName"));

        TableColumn<BOMItem, String> matCol = new TableColumn<>("Material");
        matCol.setCellValueFactory(new PropertyValueFactory<>("materialName"));

        TableColumn<BOMItem, String> dimCol = new TableColumn<>("Dimensões (mm)");
        dimCol.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().formattedDimensions()));

        TableColumn<BOMItem, Integer> qtyCol = new TableColumn<>("Qtd");
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));

        TableColumn<BOMItem, Double> areaCmCol = new TableColumn<>("Área (cm²)");
        areaCmCol.setCellValueFactory(new PropertyValueFactory<>("totalAreaCm2"));

        TableColumn<BOMItem, Double> areaFtCol = new TableColumn<>("Área (ft²)");
        areaFtCol.setCellValueFactory(new PropertyValueFactory<>("totalAreaSqFt"));

        table.getColumns().addAll(nameCol, layerCol, matCol, dimCol, qtyCol, areaCmCol, areaFtCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Button exportBtn = new Button("📄 Exportar Ficha Técnica (TXT / Relatório)");
        exportBtn.setStyle("-fx-background-color: #00A8FF; -fx-text-fill: white; -fx-font-weight: bold;");

        exportBtn.setOnAction(e -> {
            try {
                File file = new File("leathercad_ficha_tecnica.txt");
                try (PrintWriter out = new PrintWriter(file)) {
                    out.println(summary.formattedSummary());
                    out.println("\n--- DETALHAMENTO DE PEÇAS ---");
                    for (BOMItem item : summary.items()) {
                        out.printf("- %s [%s] | %s | Dim: %s | Qtd: %d | Área: %.1f cm² (%.3f ft²)\n",
                            item.pieceName(), item.layerName(), item.materialName(), item.formattedDimensions(), item.quantity(), item.totalAreaCm2(), item.totalAreaSqFt());
                    }
                }
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Ficha técnica de produção exportada com sucesso em: " + file.getAbsolutePath());
                alert.show();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        HBox btnBox = new HBox(exportBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(10, 0, 0, 0));

        VBox topContainer = new VBox(10, title, summaryBox);
        root.setTop(topContainer);
        root.setCenter(table);
        root.setBottom(btnBox);

        Scene scene = new Scene(root, 780, 560);
        scene.getStylesheets().add(ProductionReportDialog.class.getResource("/styles/dark-theme.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private static VBox createSummaryCard(String labelStr, String valueStr, String subStr) {
        Label label = new Label(labelStr);
        label.setStyle("-fx-text-fill: #A0A0A0; -fx-font-size: 11px;");

        Label value = new Label(valueStr);
        value.setStyle("-fx-text-fill: #00FF88; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label sub = new Label(subStr);
        sub.setStyle("-fx-text-fill: #555555; -fx-font-size: 10px;");

        VBox card = new VBox(2, label, value, sub);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #1E1E1E; -fx-background-radius: 6; -fx-border-color: #333333; -fx-border-radius: 6;");
        card.setMinWidth(160);
        card.setAlignment(Pos.CENTER);
        return card;
    }
}
