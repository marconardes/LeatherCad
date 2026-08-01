package com.leathercad.ui;

import com.leathercad.core.export.AssemblyManualPDFExporter;
import com.leathercad.core.model.Document;
import com.leathercad.core.production.AssemblyManual;
import com.leathercad.core.production.AssemblyManualEngine;
import com.leathercad.core.production.AssemblyStep;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;

public class AssemblyManualDialog {

    public static void showDialog(Stage owner, Document document) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("📘 Manual de Montagem & Guia de Oficina — Passo a Passo");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #141414;");
        root.setPadding(new Insets(15));

        AssemblyManual manual = AssemblyManualEngine.generateManual(document);

        // Header
        Label headerLabel = new Label("📘 Manual de Montagem & Guia de Oficina");
        headerLabel.setStyle("-fx-text-fill: #00FF88; -fx-font-weight: bold; -fx-font-size: 16px;");

        Label subHeader = new Label(String.format("Projeto com %d peças de corte, %d furos de garfo e %.2fm de costura",
            manual.totalPiecesCount(), manual.totalStitchHolesCount(), manual.totalStitchLengthMeters()));
        subHeader.setStyle("-fx-text-fill: #CCCCCC; -fx-font-size: 12px;");

        VBox headerBox = new VBox(5, headerLabel, subHeader);
        headerBox.setPadding(new Insets(0, 0, 15, 0));

        // Centro: Tabela de Etapas
        ListView<VBox> stepsList = new ListView<>();
        stepsList.setStyle("-fx-control-inner-background: #1E1E1E;");

        for (AssemblyStep step : manual.steps()) {
            VBox card = new VBox(6);
            card.setPadding(new Insets(10));
            card.setStyle("-fx-background-color: #252526; -fx-border-color: #333333; -fx-background-radius: 4;");

            Label title = new Label(String.format("Etapa %d: [%s] — %s", step.stepNumber(), step.stageName().toUpperCase(), step.title()));
            title.setStyle("-fx-text-fill: #00A8FF; -fx-font-weight: bold; -fx-font-size: 13px;");

            Label desc = new Label(step.description());
            desc.setWrapText(true);
            desc.setStyle("-fx-text-fill: white;");

            Label tools = new Label("🛠️ Ferramentas: " + String.join(", ", step.requiredTools()));
            tools.setStyle("-fx-text-fill: #FFD700; -fx-font-size: 11px;");

            Label supplies = new Label("📦 Insumos: " + String.join(", ", step.requiredSupplies()));
            supplies.setStyle("-fx-text-fill: #00FF88; -fx-font-size: 11px;");

            card.getChildren().addAll(title, desc, tools, supplies);
            stepsList.getItems().add(card);
        }

        // Painel Lateral de Insumos e Ferramentas MESTRE
        VBox sideBox = new VBox(10);
        sideBox.setPrefWidth(260);
        sideBox.setPadding(new Insets(10));
        sideBox.setStyle("-fx-background-color: #1E1E1E; -fx-border-color: #333333;");

        Label toolsTitle = new Label("🛠️ Ferramentas da Bancada:");
        toolsTitle.setStyle("-fx-text-fill: #FFD700; -fx-font-weight: bold;");

        TextArea toolsArea = new TextArea(String.join("\n", manual.masterToolsList()));
        toolsArea.setEditable(false);
        toolsArea.setPrefRowCount(8);
        toolsArea.setWrapText(true);
        toolsArea.setStyle("-fx-control-inner-background: #252526; -fx-text-fill: white;");

        Label suppliesTitle = new Label("📦 Insumos & Materiais:");
        suppliesTitle.setStyle("-fx-text-fill: #00FF88; -fx-font-weight: bold;");

        TextArea suppliesArea = new TextArea(String.join("\n", manual.masterSuppliesList()));
        suppliesArea.setEditable(false);
        suppliesArea.setPrefRowCount(8);
        suppliesArea.setWrapText(true);
        suppliesArea.setStyle("-fx-control-inner-background: #252526; -fx-text-fill: white;");

        sideBox.getChildren().addAll(toolsTitle, toolsArea, new Separator(Orientation.HORIZONTAL), suppliesTitle, suppliesArea);

        // Rodapé com botões
        Button exportBtn = new Button("📄 Exportar Manual da Oficina (.TXT / PDF)");
        exportBtn.setStyle("-fx-background-color: #00A8FF; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");

        Button closeBtn = new Button("Fechar");
        closeBtn.setStyle("-fx-background-color: #333333; -fx-text-fill: white;");
        closeBtn.setOnAction(e -> dialog.close());

        exportBtn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Salvar Manual Técnico de Montagem");
            chooser.setInitialFileName("manual_montagem_oficina.txt");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Relatório de Oficina (*.txt)", "*.txt"));
            File file = chooser.showSaveDialog(dialog);
            if (file != null) {
                try {
                    AssemblyManualPDFExporter.exportManualToTextReport(manual, file);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Manual técnico de montagem exportado com sucesso para:\n" + file.getAbsolutePath());
                    alert.show();
                } catch (Exception ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Erro ao exportar manual: " + ex.getMessage());
                    alert.show();
                }
            }
        });

        HBox btnBox = new HBox(10, exportBtn, closeBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(10, 0, 0, 0));

        root.setTop(headerBox);
        root.setCenter(stepsList);
        root.setRight(sideBox);
        root.setBottom(btnBox);

        Scene scene = new Scene(root, 820, 560);
        scene.getStylesheets().add(AssemblyManualDialog.class.getResource("/styles/dark-theme.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
