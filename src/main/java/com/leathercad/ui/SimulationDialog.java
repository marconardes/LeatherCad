package com.leathercad.ui;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.leather.CreaseElement;
import com.leathercad.core.leather.StitchElement;
import com.leathercad.core.model.*;
import com.leathercad.core.simulation.CollisionDetector;
import com.leathercad.core.simulation.FoldSimulator;
import com.leathercad.core.simulation.LayerStackAnalyzer;
import com.leathercad.core.simulation.VolumeCalculator;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

public class SimulationDialog {

    public static void showDialog(Stage owner, Document document) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.setTitle("🎬 Simulação de Montagem & Dobras 2.5D/3D Real");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1E1E1E;");
        root.setPadding(new Insets(10));

        // 1. Canvas 2.5D/3D Isométrico
        Canvas simCanvas = new Canvas(520, 380);
        GraphicsContext gc = simCanvas.getGraphicsContext2D();

        // 2. Controle Deslizante de Ângulo de Dobra (180° Aberto Plano a 0° Fechado Dobrado)
        Slider foldSlider = new Slider(0, 180, 180);
        foldSlider.setShowTickLabels(true);
        foldSlider.setShowTickMarks(true);
        foldSlider.setMajorTickUnit(45);
        foldSlider.setMinorTickCount(3);

        Label sliderLabel = new Label("Ângulo de Dobra: 180° (Carteira Aberta Plana)");
        sliderLabel.setStyle("-fx-text-fill: #00FF88; -fx-font-weight: bold;");

        List<CADElement> flatElements = CADAssembler.flatten(document.getElements());

        Runnable drawSimulation = () -> {
            double angle = foldSlider.getValue();
            sliderLabel.setText(String.format("Ângulo de Dobra: %.0f° (%s)", angle, angle <= 10 ? "Fechada 180°" : (angle < 160 ? "Dobrando em 3D" : "Aberta Plana")));

            gc.setFill(Color.web("#1E1E1E"));
            gc.fillRect(0, 0, simCanvas.getWidth(), simCanvas.getHeight());

            // Desenha grade de fundo de simulação
            gc.setStroke(Color.web("#2C2C2C"));
            gc.setLineWidth(1.0);
            for (int x = 0; x < simCanvas.getWidth(); x += 30) {
                gc.strokeLine(x, 0, x, simCanvas.getHeight());
            }
            for (int y = 0; y < simCanvas.getHeight(); y += 30) {
                gc.strokeLine(0, y, simCanvas.getWidth(), y);
            }

            double originX = simCanvas.getWidth() / 2.0;
            double originY = simCanvas.getHeight() / 2.0;

            Rect2D bbox = document.getBoundingBox();
            double docCenterX = bbox.minPoint().x() + bbox.width() / 2.0;
            double docCenterY = bbox.minPoint().y() + bbox.height() / 2.0;

            // Busca automática da linha de vinco central (foldLineX) e isolamento da peça dobrável
            double foldLineX = docCenterX;
            double foldPieceHalfWidth = 60.0;

            for (CADElement elem : flatElements) {
                if (elem instanceof CreaseElement crease) {
                    if (Math.abs(crease.line().start().x() - crease.line().end().x()) < 5.0) {
                        foldLineX = (crease.line().start().x() + crease.line().end().x()) / 2.0;
                        for (CADElement e2 : flatElements) {
                            if (e2 instanceof RectElement r && r.rect().contains(crease.line().start())) {
                                foldPieceHalfWidth = r.rect().width() / 2.0;
                                break;
                            }
                        }
                        break;
                    }
                }
            }

            double maxFoldableX = foldLineX + foldPieceHalfWidth + 5.0;
            double maxDim = Math.max(20.0, Math.max(bbox.width(), bbox.height()));
            double scale = Math.min(260.0 / maxDim, 2.2);

            final double detectedFoldLineX = foldLineX;
            final double detectedMaxFoldableX = maxFoldableX;

            java.util.function.Function<Point2D, Point2D> projectPoint = (Point2D worldPt) -> {
                return FoldSimulator.projectFold3D(
                    worldPt,
                    detectedFoldLineX,
                    detectedMaxFoldableX,
                    angle,
                    docCenterX,
                    docCenterY,
                    scale,
                    originX,
                    originY
                );
            };

            // Renderiza primitivas no espaço 3D dobrável
            for (CADElement elem : flatElements) {
                if (elem instanceof RectElement rectElem) {
                    var rect = rectElem.rect();
                    Point2D p1 = projectPoint.apply(rect.minPoint());
                    Point2D p2 = projectPoint.apply(new Point2D(rect.minPoint().x() + rect.width(), rect.minPoint().y()));
                    Point2D p3 = projectPoint.apply(new Point2D(rect.minPoint().x() + rect.width(), rect.minPoint().y() + rect.height()));
                    Point2D p4 = projectPoint.apply(new Point2D(rect.minPoint().x(), rect.minPoint().y() + rect.height()));

                    gc.setFill(Color.web("#00A8FF", 0.20));
                    gc.fillPolygon(new double[]{p1.x(), p2.x(), p3.x(), p4.x()}, new double[]{p1.y(), p2.y(), p3.y(), p4.y()}, 4);

                    gc.setStroke(Color.web("#00A8FF"));
                    gc.setLineWidth(1.8);
                    gc.strokePolygon(new double[]{p1.x(), p2.x(), p3.x(), p4.x()}, new double[]{p1.y(), p2.y(), p3.y(), p4.y()}, 4);

                } else if (elem instanceof PolylineElement polyElem) {
                    var pts = polyElem.polyline().points();
                    if (pts.size() >= 2) {
                        double[] xPts = new double[pts.size()];
                        double[] yPts = new double[pts.size()];
                        for (int i = 0; i < pts.size(); i++) {
                            Point2D proj = projectPoint.apply(pts.get(i));
                            xPts[i] = proj.x();
                            yPts[i] = proj.y();
                        }
                        if (polyElem.polyline().isClosed()) {
                            gc.setFill(Color.web("#00A8FF", 0.20));
                            gc.fillPolygon(xPts, yPts, pts.size());
                            gc.setStroke(Color.web("#00A8FF"));
                            gc.setLineWidth(1.8);
                            gc.strokePolygon(xPts, yPts, pts.size());
                        } else {
                            gc.setStroke(Color.web("#00A8FF"));
                            gc.setLineWidth(1.8);
                            gc.strokePolyline(xPts, yPts, pts.size());
                        }
                    }
                } else if (elem instanceof StitchElement stitchElem) {
                    Point2D s = projectPoint.apply(stitchElem.baseLine().start());
                    Point2D e = projectPoint.apply(stitchElem.baseLine().end());

                    gc.setStroke(Color.web("#FFD700"));
                    gc.setLineWidth(1.2);
                    gc.setLineDashes(4.0);
                    gc.strokeLine(s.x(), s.y(), e.x(), e.y());
                    gc.setLineDashes(null);

                    gc.setFill(Color.web("#FFD700"));
                    for (Point2D hole : stitchElem.holePoints()) {
                        Point2D hp = projectPoint.apply(hole);
                        gc.fillOval(hp.x() - 1.5, hp.y() - 1.5, 3.0, 3.0);
                    }
                } else if (elem instanceof CreaseElement creaseElem) {
                    Point2D s = projectPoint.apply(creaseElem.line().start());
                    Point2D e = projectPoint.apply(creaseElem.line().end());
                    gc.setStroke(Color.web("#FF8C00"));
                    gc.setLineWidth(2.0);
                    gc.strokeLine(s.x(), s.y(), e.x(), e.y());
                }
            }
        };

        foldSlider.valueProperty().addListener((obs, oldV, newV) -> drawSimulation.run());

        VBox topControls = new VBox(5, sliderLabel, foldSlider);
        topControls.setPadding(new Insets(0, 0, 10, 0));

        // 3. Painel de Ficha Técnica & Diagnósticos à Direita
        VBox reportBox = new VBox(10);
        reportBox.setPadding(new Insets(10));
        reportBox.setStyle("-fx-background-color: #252526; -fx-border-color: #333333; -fx-min-width: 260;");

        Label reportTitle = new Label("📊 Ficha de Montagem");
        reportTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");

        double totalArea = VolumeCalculator.calculateTotalAreaMm2(document);
        double totalVolume = VolumeCalculator.calculateTotalVolumeMm3(document);
        double totalWeight = VolumeCalculator.calculateTotalWeightGrams(document);
        double maxThickness = LayerStackAnalyzer.calculateMaxAccumulatedThickness(document);

        Label areaLabel = new Label(String.format("Área Plana: %.1f cm²", totalArea / 100.0));
        Label volLabel = new Label(String.format("Volume Total: %.1f cm³", totalVolume / 1000.0));
        Label weightLabel = new Label(String.format("Peso Estimado: %.1f g", totalWeight));
        Label thickLabel = new Label(String.format("Espessura Máxima: %.1f mm", maxThickness));

        areaLabel.setStyle("-fx-text-fill: #CCCCCC;");
        volLabel.setStyle("-fx-text-fill: #CCCCCC;");
        weightLabel.setStyle("-fx-text-fill: #CCCCCC;");
        thickLabel.setStyle("-fx-text-fill: #CCCCCC;");

        ListView<String> warningsList = new ListView<>();
        warningsList.setPrefHeight(130);
        warningsList.setStyle("-fx-control-inner-background: #1E1E1E; -fx-text-fill: #FF6B6B;");
        List<String> warnings = CollisionDetector.detectInterferences(document);
        warningsList.getItems().addAll(warnings);

        Label diagLabel = new Label("Diagnósticos & Interferências:");
        diagLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");

        reportBox.getChildren().addAll(
            reportTitle,
            areaLabel, volLabel, weightLabel, thickLabel,
            new Separator(Orientation.HORIZONTAL),
            diagLabel,
            warningsList
        );

        drawSimulation.run();

        root.setTop(topControls);
        root.setCenter(simCanvas);
        root.setRight(reportBox);

        Scene scene = new Scene(root, 820, 500);
        scene.getStylesheets().add(SimulationDialog.class.getResource("/styles/dark-theme.css").toExternalForm());
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
