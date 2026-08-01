package com.leathercad.ui;

import com.leathercad.core.ai.AIDesignGenerator;
import com.leathercad.core.ai.AIPromptPreset;
import com.leathercad.core.ai.OllamaClient;
import com.leathercad.core.model.Document;
import com.leathercad.ui.viewport.CanvasViewport;

import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class AIAssistantDialog {

    public static void showDialog(Stage owner, Document document, CanvasViewport viewport) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("✨ Assistente de IA - Geração Automática de Moldes (Ollama Local)");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #141414;");
        root.setPadding(new Insets(15));

        // 1. Painel Superior: Status da Conexão e Seleção do Servidor/Modelo
        Label statusLabel = new Label("🔄 Verificando servidor Ollama...");
        statusLabel.setStyle("-fx-text-fill: #CCCCCC; -fx-font-weight: bold; -fx-font-size: 13px;");

        TextField urlField = new TextField("http://localhost:11434");
        urlField.setPrefWidth(180);

        ComboBox<String> modelCombo = new ComboBox<>();
        modelCombo.getItems().addAll("gemma4:e2b", "qwen2.5:1.5b", "gemma3:4b", "gemma3:270m");
        modelCombo.setValue("gemma4:e2b");

        Button checkBtn = new Button("🔄 Testar Conexão");
        checkBtn.setStyle("-fx-background-color: #333333; -fx-text-fill: white;");

        OllamaClient client = new OllamaClient();

        Runnable checkStatus = () -> {
            boolean available = client.isOllamaAvailable(urlField.getText());
            if (available) {
                statusLabel.setText("🟢 Ollama Local Ativo e Conectado (" + urlField.getText() + ")");
                statusLabel.setStyle("-fx-text-fill: #00FF88; -fx-font-weight: bold; -fx-font-size: 13px;");
            } else {
                statusLabel.setText("🟡 Ollama Offline (Usando Motor de IA de Regras Embutido)");
                statusLabel.setStyle("-fx-text-fill: #FFD700; -fx-font-weight: bold; -fx-font-size: 13px;");
            }
        };

        checkBtn.setOnAction(e -> checkStatus.run());
        checkStatus.run();

        HBox serverBox = new HBox(10, new Label("Servidor Ollama:"), urlField, new Label("Modelo:"), modelCombo, checkBtn);
        serverBox.setAlignment(Pos.CENTER_LEFT);
        ((Label) serverBox.getChildren().get(0)).setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        ((Label) serverBox.getChildren().get(2)).setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        // 2. Presets de Comandos
        ComboBox<AIPromptPreset> presetCombo = new ComboBox<>();
        presetCombo.getItems().addAll(
            AIPromptPreset.BIFOLD_6_CARDS,
            AIPromptPreset.MINIMALIST_CARDHOLDER,
            AIPromptPreset.PASSPORT_COVER,
            AIPromptPreset.COIN_WALLET
        );
        presetCombo.setValue(AIPromptPreset.BIFOLD_6_CARDS);

        TextArea promptArea = new TextArea(AIPromptPreset.BIFOLD_6_CARDS.prompt());
        promptArea.setPrefRowCount(3);
        promptArea.setWrapText(true);
        promptArea.setStyle("-fx-control-inner-background: #1E1E1E; -fx-text-fill: white;");

        presetCombo.setOnAction(e -> {
            if (presetCombo.getValue() != null) {
                promptArea.setText(presetCombo.getValue().prompt());
            }
        });

        HBox presetBox = new HBox(10, new Label("Presets Rápidos:"), presetCombo);
        presetBox.setAlignment(Pos.CENTER_LEFT);
        ((Label) presetBox.getChildren().get(0)).setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        TextArea outputLog = new TextArea();
        outputLog.setEditable(false);
        outputLog.setPrefRowCount(6);
        outputLog.setWrapText(true);
        outputLog.setStyle("-fx-control-inner-background: #1E1E1E; -fx-text-fill: #00A8FF; -fx-font-family: monospace;");

        Button generateBtn = new Button("✨ Executar IA / Processar Prompt");
        generateBtn.setStyle("-fx-background-color: #00A8FF; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");

        Button approveBtn = new Button("✅ Aprovar & Aplicar ao Canvas");
        approveBtn.setStyle("-fx-background-color: #2ED573; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");
        approveBtn.setDisable(true);

        generateBtn.setOnAction(e -> {
            String prompt = promptArea.getText().trim();
            if (!prompt.isEmpty()) {
                generateBtn.setDisable(true);
                generateBtn.setText("⏳ Processando IA... Aguarde");
                approveBtn.setDisable(true);
                outputLog.setText("⌛ Processando geração de moldes via IA (" + modelCombo.getValue() + ")... Aguarde.");

                Task<String> aiTask = new Task<>() {
                    @Override
                    protected String call() throws Exception {
                        return AIDesignGenerator.generateDesign(
                            document,
                            prompt,
                            urlField.getText(),
                            modelCombo.getValue(),
                            true
                        );
                    }
                };

                aiTask.setOnSucceeded(evt -> {
                    String resultLog = aiTask.getValue();
                    outputLog.setText(resultLog + "\n\n👉 Revise o log acima e clique em 'Aprovar & Aplicar ao Canvas' para carregar no desenho.");
                    generateBtn.setDisable(false);
                    generateBtn.setText("✨ Executar IA / Processar Prompt");
                    approveBtn.setDisable(false);
                });

                aiTask.setOnFailed(evt -> {
                    outputLog.setText("❌ Erro ao processar prompt na IA: " + (aiTask.getException() != null ? aiTask.getException().getMessage() : "Desconhecido"));
                    generateBtn.setDisable(false);
                    generateBtn.setText("✨ Executar IA / Processar Prompt");
                    approveBtn.setDisable(true);
                });

                Thread thread = new Thread(aiTask);
                thread.setDaemon(true);
                thread.start();
            }
        });

        approveBtn.setOnAction(e -> {
            viewport.redraw();
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Projeto de moldes 2D gerado pela IA foi aprovado e carregado no Canvas!");
            alert.show();
            dialog.close();
        });

        HBox btnBox = new HBox(10, generateBtn, approveBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(10, 0, 0, 0));

        VBox centerBox = new VBox(10, statusLabel, serverBox, presetBox, new Label("Instrução em Linguagem Natural:"), promptArea, new Label("Log de Resposta da IA (Revisão para Aprovação):"), outputLog);
        centerBox.getChildren().get(4).setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        centerBox.getChildren().get(6).setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        root.setCenter(centerBox);
        root.setBottom(btnBox);

        Scene scene = new Scene(root, 760, 560);
        scene.getStylesheets().add(AIAssistantDialog.class.getResource("/styles/dark-theme.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
