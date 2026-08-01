package com.leathercad.ui;

import com.leathercad.core.model.Document;
import com.leathercad.core.render.PhotorealisticRenderer;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;

public class RealisticRenderDialog {

    public static void showDialog(Stage owner, Document document) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("📸 Estúdio de Renderização Fotorrealista HD");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #141414;");
        root.setPadding(new Insets(15));

        Canvas renderCanvas = new Canvas(800, 520);
        PhotorealisticRenderer.renderPhotorealistic(document, renderCanvas);

        Label title = new Label("📸 Estúdio Fotográfico - Renderização de Couro Fotorrealista HD");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #00A8FF;");

        Button exportBtn = new Button("📸 Exportar Imagem Fotorrealista (PNG HD)");
        exportBtn.setStyle("-fx-background-color: #00A8FF; -fx-text-fill: white; -fx-font-weight: bold;");

        exportBtn.setOnAction(e -> {
            try {
                WritableImage image = renderCanvas.snapshot(null, null);
                File file = new File("leathercad_render_photorealistic.png");
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);

                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Render fotorrealista HD salvo com sucesso em: " + file.getAbsolutePath());
                alert.show();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        HBox btnBox = new HBox(exportBtn);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(10, 0, 0, 0));

        root.setTop(new VBox(title));
        root.setCenter(renderCanvas);
        root.setBottom(btnBox);

        Scene scene = new Scene(root, 840, 600);
        scene.getStylesheets().add(RealisticRenderDialog.class.getResource("/styles/dark-theme.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
