package com.leathercad.ui;

import com.leathercad.core.model.Document;
import com.leathercad.core.parametric.BifoldWalletTemplate;
import com.leathercad.core.parametric.FoldType;
import com.leathercad.core.parametric.ProjectVariables;
import com.leathercad.ui.viewport.CanvasViewport;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import javafx.stage.Modality;
import javafx.stage.Stage;

public class ParametricDialog {

    public static void showDialog(Stage owner, Document document, CanvasViewport viewport) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("Gerador Inteligente de Carteiras Paramétricas");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));

        // 1. Modelo de Projeto
        ComboBox<String> modelCombo = new ComboBox<>();
        modelCombo.getItems().addAll("Carteira Bifold (2 Abas)", "Porta-Cartões Minimalista");
        modelCombo.setValue("Carteira Bifold (2 Abas)");

        // 2. Espessura do Couro
        Spinner<Double> thicknessSpinner = new Spinner<>(0.6, 3.5, 1.4, 0.1);
        thicknessSpinner.setEditable(true);

        // 3. Número de Cartões
        Spinner<Integer> cardsSpinner = new Spinner<>(2, 12, 6, 2);

        // 4. Largura Fechada
        Spinner<Double> widthSpinner = new Spinner<>(80.0, 150.0, 115.0, 5.0);
        widthSpinner.setEditable(true);

        // 5. Altura (com recálculo reativo em função dos cartões)
        Spinner<Double> heightSpinner = new Spinner<>(60.0, 160.0, 85.0, 5.0);
        heightSpinner.setEditable(true);

        Runnable updateHeight = () -> {
            boolean isCardHolder = modelCombo.getValue().contains("Porta-Cartões");
            int numCards = cardsSpinner.getValue();
            int slots = isCardHolder ? numCards : (int) Math.ceil(numCards / 2.0);
            double minH = 5.0 + ((slots - 1) * 14.0) + 45.0 + 15.0;
            if (heightSpinner.getValue() < minH) {
                heightSpinner.getValueFactory().setValue(minH);
            }
        };

        cardsSpinner.valueProperty().addListener((obs, oldV, newV) -> updateHeight.run());
        modelCombo.valueProperty().addListener((obs, oldV, newV) -> updateHeight.run());

        grid.add(new Label("Modelo de Artigo:"), 0, 0);
        grid.add(modelCombo, 1, 0);

        grid.add(new Label("Espessura do Couro (mm):"), 0, 1);
        grid.add(thicknessSpinner, 1, 1);

        grid.add(new Label("Quantidade de Cartões:"), 0, 2);
        grid.add(cardsSpinner, 1, 2);

        grid.add(new Label("Largura Fechada (mm):"), 0, 3);
        grid.add(widthSpinner, 1, 3);

        grid.add(new Label("Altura (mm):"), 0, 4);
        grid.add(heightSpinner, 1, 4);

        Button generateBtn = new Button("🚀 Gerar Molde Técnico");
        generateBtn.setStyle("-fx-background-color: #00A8FF; -fx-text-fill: white; -fx-font-weight: bold;");

        generateBtn.setOnAction(e -> {
            boolean isCardHolder = modelCombo.getValue().contains("Porta-Cartões");
            ProjectVariables vars = new ProjectVariables(
                widthSpinner.getValue(),
                heightSpinner.getValue(),
                thicknessSpinner.getValue(),
                cardsSpinner.getValue(),
                isCardHolder ? FoldType.SINGLE_FOLD : FoldType.BIFOLD,
                3.85,
                3.85
            );

            if (isCardHolder) {
                com.leathercad.core.parametric.CardHolderTemplate.generateCardHolder(document, vars);
            } else {
                BifoldWalletTemplate.generateBifoldWallet(document, vars);
            }
            viewport.redraw();
            dialog.close();
        });

        HBox btnBox = new HBox(generateBtn);
        btnBox.setPadding(new Insets(10, 0, 0, 0));
        grid.add(btnBox, 1, 5);

        Scene scene = new Scene(grid, 420, 280);
        scene.getStylesheets().add(ParametricDialog.class.getResource("/styles/dark-theme.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
