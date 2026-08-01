package com.leathercad.ui;

import com.leathercad.core.model.Document;
import com.leathercad.core.model.Layer;
import com.leathercad.ui.icons.CADIconFactory;
import com.leathercad.ui.icons.CADIconFactory.IconType;
import com.leathercad.ui.tools.ToolManager;
import com.leathercad.ui.viewport.CanvasViewport;

import javafx.application.Application;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;

public class MainApp extends Application {
    private Document document;
    private ToolManager toolManager;
    private CanvasViewport viewport;

    private com.leathercad.core.materials.MaterialLibrary materialLibrary = new com.leathercad.core.materials.MaterialLibrary();
    private Stage primaryStage;
    private Label statusLabel;
    private Label zoomLabel;

    private File currentProjectFile = null;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        document = new Document();
        toolManager = new ToolManager();
        viewport = new CanvasViewport(document, toolManager);

        BorderPane root = new BorderPane();

        // Top Menu & Toolbar
        MenuBar menuBar = createMenuBar(primaryStage);
        ToolBar toolBar = createToolBar();
        VBox topContainer = new VBox(menuBar, toolBar);
        root.setTop(topContainer);

        // Center Canvas Viewport
        Pane canvasContainer = new Pane(viewport);
        viewport.widthProperty().bind(canvasContainer.widthProperty());
        viewport.heightProperty().bind(canvasContainer.heightProperty());

        canvasContainer.widthProperty().addListener((obs, oldV, newV) -> viewport.redraw());
        canvasContainer.heightProperty().addListener((obs, oldV, newV) -> viewport.redraw());

        root.setCenter(canvasContainer);

        // Sidebar com Abas: Árvore do Modelo (FreeCAD), Camadas & Variáveis Globais (Fusion 360)
        com.leathercad.core.parametric.VariableTable variableTable = new com.leathercad.core.parametric.VariableTable();
        com.leathercad.ui.panels.VariableTablePanel variablePanel = new com.leathercad.ui.panels.VariableTablePanel(variableTable, viewport);
        VBox layerPanel = createLayerPanel();

        TabPane sidebarTabs = new TabPane();
        ModelTreeViewPanel modelTreePanel = new ModelTreeViewPanel(document, viewport);
        Tab treeTab = new Tab("🌳 Modelo (FreeCAD)", modelTreePanel);
        treeTab.setClosable(false);

        Tab layerTab = new Tab("Camadas", layerPanel);
        layerTab.setClosable(false);

        Tab varTab = new Tab("📐 Variáveis (Fusion 360)", variablePanel);
        varTab.setClosable(false);

        sidebarTabs.getTabs().addAll(treeTab, layerTab, varTab);
        sidebarTabs.setStyle("-fx-background-color: #252526;");
        root.setRight(sidebarTabs);

        // Status Bar inferior
        HBox statusBar = createStatusBar();
        root.setBottom(statusBar);

        // Listener de posição do cursor em mm
        viewport.setCursorPositionListener((point, zoom) -> {
            statusLabel.setText(String.format(" X: %.2f mm   Y: %.2f mm", point.x(), point.y()));
            zoomLabel.setText(String.format("Zoom: %.0f%% ", zoom * 100 / 3.0));
        });

        Scene scene = new Scene(root, 1280, 800);
        scene.getStylesheets().add(getClass().getResource("/styles/light-theme.css").toExternalForm());

        scene.setOnKeyPressed(event -> {
            if (event.isControlDown()) {
                if (event.isShiftDown() && event.getCode() == javafx.scene.input.KeyCode.S) {
                    saveProjectAs();
                    return;
                }
                switch (event.getCode()) {
                    case N -> { newProject(); return; }
                    case O -> { openProject(); return; }
                    case S -> { saveProject(); return; }
                    case T -> { toggleTheme(scene); return; }
                    case E -> { document.explodeSelected(); viewport.redraw(); return; }
                    case D -> {
                        document.copySelected(10.0, 10.0);
                        viewport.redraw();
                        return;
                    }
                }
            }
            switch (event.getCode()) {
                case DELETE, BACK_SPACE -> {
                    document.deleteSelected();
                    viewport.redraw();
                }
                case ESCAPE -> {
                    toolManager.resetActiveTool();
                    document.clearSelection();
                    viewport.redraw();
                }
            }
        });

        updateWindowTitle();
        primaryStage.setMaximized(true);
        primaryStage.setScene(scene);
        primaryStage.show();

        viewport.redraw();
    }

    private MenuBar createMenuBar(Stage stage) {
        MenuBar menuBar = new MenuBar();

        Menu fileMenu = new Menu("Arquivo");

        MenuItem newItem = new MenuItem("📄 Novo Projeto (Ctrl+N)");
        newItem.setOnAction(e -> newProject());

        MenuItem openItem = new MenuItem("📂 Abrir Projeto (.lcad)... (Ctrl+O)");
        openItem.setOnAction(e -> openProject());

        MenuItem saveItem = new MenuItem("💾 Salvar Projeto (Ctrl+S)");
        saveItem.setOnAction(e -> saveProject());

        MenuItem saveAsItem = new MenuItem("💾 Salvar Como... (Ctrl+Shift+S)");
        saveAsItem.setOnAction(e -> saveProjectAs());

        MenuItem exportSvg = new MenuItem("Exportar SVG...");
        exportSvg.setOnAction(e -> exportFile("svg"));

        MenuItem exportDxf = new MenuItem("Exportar DXF (AutoCAD R12)...");
        exportDxf.setOnAction(e -> exportFile("dxf"));

        MenuItem exportPdf = new MenuItem("Exportar PDF...");
        exportPdf.setOnAction(e -> exportFile("pdf"));

        MenuItem exportPng = new MenuItem("Exportar PNG...");
        exportPng.setOnAction(e -> exportFile("png"));

        MenuItem printStudioItem = new MenuItem("🖨️ Imprimir Moldes 1:1 & Paginação...");
        printStudioItem.setOnAction(e -> PrintStudioDialog.showDialog(stage, document));

        MenuItem exitItem = new MenuItem("Sair");
        exitItem.setOnAction(e -> stage.close());

        fileMenu.getItems().addAll(
            newItem, openItem, saveItem, saveAsItem, new SeparatorMenuItem(),
            exportSvg, exportDxf, exportPdf, exportPng, new SeparatorMenuItem(),
            printStudioItem, new SeparatorMenuItem(), exitItem
        );

        Menu editMenu = new Menu("Editar");
        MenuItem moveItem = new MenuItem("Mover Selecionados");
        moveItem.setOnAction(e -> { document.moveSelected(10.0, 10.0); viewport.redraw(); });

        MenuItem copyItem = new MenuItem("Duplicar / Copiar");
        copyItem.setOnAction(e -> { document.copySelected(10.0, 10.0); viewport.redraw(); });

        MenuItem rotateItem = new MenuItem("Rotacionar 90°");
        rotateItem.setOnAction(e -> { document.rotateSelected(90.0); viewport.redraw(); });

        MenuItem mirrorItem = new MenuItem("Clonagem Espelhada Simétrica");
        mirrorItem.setOnAction(e -> { document.mirrorSelectedHorizontal(); viewport.redraw(); });

        MenuItem deleteItem = new MenuItem("Excluir Seleção");
        deleteItem.setOnAction(e -> { document.deleteSelected(); viewport.redraw(); });

        MenuItem explodeItem = new MenuItem("Explodir em Linhas Individuais (Ctrl+E)");
        explodeItem.setOnAction(e -> { document.explodeSelected(); viewport.redraw(); });

        editMenu.getItems().addAll(moveItem, copyItem, rotateItem, mirrorItem, deleteItem, new SeparatorMenuItem(), explodeItem);

        Menu leatherMenu = new Menu("Couro");
        MenuItem filletToolItem = new MenuItem("📐 Arredondamento de Cantos (Fillet)...");
        filletToolItem.setOnAction(e -> CornerRadiusDialog.showDialog(primaryStage, document, viewport, toolManager, 5.0));

        MenuItem r3Item = new MenuItem("Arredondar Seleção (R3)");
        r3Item.setOnAction(e -> { document.setCornerRadiusSelected(3.0, 3.0, 3.0, 3.0); viewport.redraw(); });

        MenuItem r5Item = new MenuItem("Arredondar Seleção (R5)");
        r5Item.setOnAction(e -> { document.setCornerRadiusSelected(5.0, 5.0, 5.0, 5.0); viewport.redraw(); });

        MenuItem r8Item = new MenuItem("Arredondar Seleção (R8)");
        r8Item.setOnAction(e -> { document.setCornerRadiusSelected(8.0, 8.0, 8.0, 8.0); viewport.redraw(); });

        MenuItem r10Item = new MenuItem("Arredondar Seleção (R10)");
        r10Item.setOnAction(e -> { document.setCornerRadiusSelected(10.0, 10.0, 10.0, 10.0); viewport.redraw(); });

        MenuItem creaseItem = new MenuItem("Adicionar Vinco Borda (1.5mm)");
        creaseItem.setOnAction(e -> {
            for (com.leathercad.core.model.CADElement elem : document.getElements()) {
                if (document.getSelectedElementIds().contains(elem.id()) && elem instanceof com.leathercad.core.model.LineElement lineElem) {
                    com.leathercad.core.leather.CreaseElement crease = new com.leathercad.core.leather.CreaseElement(
                        document.getActiveLayer().getId(),
                        lineElem.line().offset(1.5),
                        1.5
                    );
                    document.addElement(crease);
                }
            }
            viewport.redraw();
        });

        leatherMenu.getItems().addAll(filletToolItem, new SeparatorMenuItem(), r3Item, r5Item, r8Item, r10Item, new SeparatorMenuItem(), creaseItem);

        Menu compMenu = new Menu("Componentes");
        MenuItem cardSlotItem = new MenuItem("💳 Porta-Cartão (95x55mm)");
        cardSlotItem.setOnAction(e -> {
            var elems = com.leathercad.core.components.CardSlotComponent.createCardSlot(document.getActiveLayer().getId(), new com.leathercad.core.geometry.Point2D(50, 50), 95.0, 55.0);
            for (var elem : elems) document.addElement(elem);
            viewport.redraw();
        });

        MenuItem idWinItem = new MenuItem("🪪 Janela Transparente ID");
        idWinItem.setOnAction(e -> {
            var elems = com.leathercad.core.components.IDWindowComponent.createIDWindow(document.getActiveLayer().getId(), new com.leathercad.core.geometry.Point2D(60, 60));
            for (var elem : elems) document.addElement(elem);
            viewport.redraw();
        });

        MenuItem coinPocketItem = new MenuItem("👛 Porta-Moedas com Lapela");
        coinPocketItem.setOnAction(e -> {
            var elems = com.leathercad.core.components.CoinPocketComponent.createCoinPocket(document.getActiveLayer().getId(), new com.leathercad.core.geometry.Point2D(70, 70), 100.0, 80.0);
            for (var elem : elems) document.addElement(elem);
            viewport.redraw();
        });

        MenuItem hiddenPocketItem = new MenuItem("🔒 Compartimento Oculto");
        hiddenPocketItem.setOnAction(e -> {
            var elems = com.leathercad.core.components.CoinPocketComponent.createHiddenPocket(document.getActiveLayer().getId(), new com.leathercad.core.geometry.Point2D(80, 80), 95.0, 60.0);
            for (var elem : elems) document.addElement(elem);
            viewport.redraw();
        });

        MenuItem magSnapItem = new MenuItem("🧲 Fecho Magnético (14mm)");
        magSnapItem.setOnAction(e -> {
            var elems = com.leathercad.core.components.HardwareComponent.createMagneticSnap(document.getActiveLayer().getId(), new com.leathercad.core.geometry.Point2D(100, 100), 14.0);
            for (var elem : elems) document.addElement(elem);
            viewport.redraw();
        });

        MenuItem pressStudItem = new MenuItem("🔘 Botão de Pressão (12mm)");
        pressStudItem.setOnAction(e -> {
            var elems = com.leathercad.core.components.HardwareComponent.createPressStud(document.getActiveLayer().getId(), new com.leathercad.core.geometry.Point2D(120, 120), 12.0);
            for (var elem : elems) document.addElement(elem);
            viewport.redraw();
        });

        MenuItem zipperItem = new MenuItem("🔩 Zíper Parametrizado (150mm)");
        zipperItem.setOnAction(e -> {
            var elems = com.leathercad.core.components.HardwareComponent.createZipper(document.getActiveLayer().getId(), new com.leathercad.core.geometry.Point2D(40, 150), 150.0, 30.0);
            for (var elem : elems) document.addElement(elem);
            viewport.redraw();
        });

        MenuItem elasticItem = new MenuItem("🎗️ Tira de Elástico (100mm)");
        elasticItem.setOnAction(e -> {
            var elems = com.leathercad.core.components.HardwareComponent.createElasticBand(document.getActiveLayer().getId(), new com.leathercad.core.geometry.Point2D(50, 180), 20.0, 100.0);
            for (var elem : elems) document.addElement(elem);
            viewport.redraw();
        });

        compMenu.getItems().addAll(cardSlotItem, idWinItem, coinPocketItem, hiddenPocketItem, new SeparatorMenuItem(), magSnapItem, pressStudItem, zipperItem, elasticItem);

        Menu matMenu = new Menu("Materiais");
        MenuItem matItem = new MenuItem("🐂 Biblioteca de Couros...");
        matItem.setOnAction(e -> MaterialManagerDialog.showDialog(primaryStage, document, materialLibrary, viewport));
        matMenu.getItems().add(matItem);

        Menu simMenu = new Menu("Simulação");
        MenuItem simItem = new MenuItem("🎬 Simular Montagem e Dobras...");
        simItem.setOnAction(e -> SimulationDialog.showDialog(primaryStage, document));
        simMenu.getItems().add(simItem);

        Menu renderMenu = new Menu("Renderização");
        MenuItem renderItem = new MenuItem("📸 Estúdio de Render Fotorrealista...");
        renderItem.setOnAction(e -> RealisticRenderDialog.showDialog(primaryStage, document));
        renderMenu.getItems().add(renderItem);

        Menu cutMenu = new Menu("Corte");
        MenuItem nestItem = new MenuItem("🧩 Corte Inteligente (Nesting)...");
        nestItem.setOnAction(e -> NestingDialog.showDialog(primaryStage, document));
        cutMenu.getItems().add(nestItem);

        Menu prodMenu = new Menu("Produção");
        MenuItem prodItem = new MenuItem("🏭 Ficha Técnica & Lista de Peças (BOM)...");
        prodItem.setOnAction(e -> ProductionReportDialog.showDialog(primaryStage, document));
        MenuItem manualItem = new MenuItem("📘 Manual de Montagem & Guia de Oficina...");
        manualItem.setOnAction(e -> AssemblyManualDialog.showDialog(primaryStage, document));
        prodMenu.getItems().addAll(prodItem, manualItem);

        Menu projectMenu = new Menu("Projeto");
        MenuItem bifoldItem = new MenuItem("📐 Gerar Carteira Paramétrica...");
        bifoldItem.setOnAction(e -> ParametricDialog.showDialog(primaryStage, document, viewport));
        MenuItem aiItem = new MenuItem("✨ Gerar Moldes por IA (Ollama Local)...");
        aiItem.setOnAction(e -> AIAssistantDialog.showDialog(primaryStage, document, viewport));
        projectMenu.getItems().addAll(bifoldItem, aiItem);

        Menu viewMenu = new Menu("Exibir");
        MenuItem themeItem = new MenuItem("☀️ Alternar Tema Claro / Escuro (Ctrl+T)");
        themeItem.setOnAction(e -> toggleTheme(primaryStage.getScene()));
        viewMenu.getItems().add(themeItem);

        menuBar.getMenus().addAll(fileMenu, editMenu, viewMenu, leatherMenu, compMenu, matMenu, simMenu, renderMenu, cutMenu, prodMenu, projectMenu);
        return menuBar;
    }

    private void exportFile(String format) {
        try {
            File f = new File("leathercad_export." + format);
            switch (format) {
                case "svg" -> com.leathercad.core.export.SVGExporter.exportToFile(document, f);
                case "dxf" -> com.leathercad.core.export.DXFExporter.exportToFile(document, f);
                case "pdf" -> com.leathercad.core.export.PDFExporter.exportToFile(document, f);
                case "png" -> com.leathercad.core.export.PNGExporter.exportViewportToPNG(viewport, f);
            }
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Exportado com sucesso para: " + f.getAbsolutePath());
            alert.show();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void newProject() {
        document.clearElements();
        currentProjectFile = null;
        updateWindowTitle();
        viewport.redraw();
    }

    private void openProject() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Abrir Projeto LeatherCAD");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Projeto LeatherCAD (*.lcad)", "*.lcad"));
        File file = chooser.showOpenDialog(primaryStage);
        if (file != null) {
            try {
                Document loaded = com.leathercad.core.export.ProjectSerializer.loadFromFile(file);
                document.clearElements();
                for (var elem : loaded.getElements()) {
                    document.addElement(elem);
                }
                if (loaded.getActiveLayer() != null) {
                    document.setActiveLayer(loaded.getActiveLayer());
                }
                currentProjectFile = file;
                updateWindowTitle();
                viewport.redraw();
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Projeto carregado com sucesso!");
                alert.show();
            } catch (Exception ex) {
                ex.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR, "Erro ao abrir o arquivo: " + ex.getMessage());
                alert.show();
            }
        }
    }

    private void saveProject() {
        if (currentProjectFile == null) {
            saveProjectAs();
        } else {
            try {
                com.leathercad.core.export.ProjectSerializer.saveToFile(document, currentProjectFile);
                updateWindowTitle();
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Projeto salvo com sucesso!");
                alert.show();
            } catch (Exception ex) {
                ex.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR, "Erro ao salvar o arquivo: " + ex.getMessage());
                alert.show();
            }
        }
    }

    private void saveProjectAs() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Salvar Projeto LeatherCAD Como...");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Projeto LeatherCAD (*.lcad)", "*.lcad"));
        if (currentProjectFile != null) {
            chooser.setInitialFileName(currentProjectFile.getName());
        } else {
            chooser.setInitialFileName("projeto_couro.lcad");
        }
        File file = chooser.showSaveDialog(primaryStage);
        if (file != null) {
            try {
                com.leathercad.core.export.ProjectSerializer.saveToFile(document, file);
                currentProjectFile = file;
                updateWindowTitle();
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Projeto salvo com sucesso em: " + file.getAbsolutePath());
                alert.show();
            } catch (Exception ex) {
                ex.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR, "Erro ao salvar o arquivo: " + ex.getMessage());
                alert.show();
            }
        }
    }

    private void updateWindowTitle() {
        String fileName = (currentProjectFile != null) ? currentProjectFile.getName() : "Sem Título";
        primaryStage.setTitle("LeatherCAD - [" + fileName + "] - CAD para Artefatos de Couro v0.1");
    }

    private boolean isLightMode = true;

    private void toggleTheme(Scene scene) {
        if (scene == null) return;
        isLightMode = !isLightMode;
        scene.getStylesheets().clear();
        String cssPath = isLightMode ? "/styles/light-theme.css" : "/styles/dark-theme.css";
        var url = getClass().getResource(cssPath);
        if (url != null) {
            scene.getStylesheets().add(url.toExternalForm());
        }
        viewport.setDarkMode(!isLightMode);
    }

    private Button createIconButton(org.kordamp.ikonli.Ikon icon, String tooltipText) {
        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.setIconSize(16);
        Button btn = new Button("", fontIcon);
        btn.setTooltip(new Tooltip(tooltipText));
        btn.getStyleClass().add("icon-only-button");
        return btn;
    }

    private ToggleButton createIconToggleButton(org.kordamp.ikonli.Ikon icon, String tooltipText, ToggleGroup group) {
        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.setIconSize(16);
        ToggleButton btn = new ToggleButton("", fontIcon);
        btn.setTooltip(new Tooltip(tooltipText));
        btn.setToggleGroup(group);
        btn.getStyleClass().add("icon-only-button");
        return btn;
    }

    private ToolBar createToolBar() {
        Button newBtn = CADIconFactory.createIconButton(IconType.NEW_FILE, "Novo Projeto (Ctrl+N)");
        newBtn.setOnAction(e -> newProject());

        Button openBtn = CADIconFactory.createIconButton(IconType.OPEN_FILE, "Abrir Projeto (.lcad)... (Ctrl+O)");
        openBtn.setOnAction(e -> openProject());

        Button saveBtn = CADIconFactory.createIconButton(IconType.SAVE_FILE, "Salvar Projeto (Ctrl+S)");
        saveBtn.setOnAction(e -> saveProject());

        ToggleGroup toolGroup = new ToggleGroup();

        ToggleButton selectBtn = CADIconFactory.createIconToggleButton(IconType.SELECT, "Ferramenta de Seleção & Edição de Nós (S)", toolGroup);
        selectBtn.setSelected(true);
        selectBtn.setOnAction(e -> toolManager.setActiveTool("Seleção"));

        ToggleButton lineBtn = CADIconFactory.createIconToggleButton(IconType.LINE, "Desenhar Segmento de Reta (L)", toolGroup);
        lineBtn.setOnAction(e -> toolManager.setActiveTool("Linha"));

        ToggleButton rectBtn = CADIconFactory.createIconToggleButton(IconType.RECTANGLE, "Desenhar Retângulo de Couro (R)", toolGroup);
        rectBtn.setOnAction(e -> toolManager.setActiveTool("Retângulo"));

        ToggleButton circleBtn = CADIconFactory.createIconToggleButton(IconType.CIRCLE, "Desenhar Círculo / Furo (C)", toolGroup);
        circleBtn.setOnAction(e -> toolManager.setActiveTool("Círculo"));

        ToggleButton polylineBtn = CADIconFactory.createIconToggleButton(IconType.POLYLINE, "Desenhar Polilinha / Molde Irregular (P)", toolGroup);
        polylineBtn.setOnAction(e -> toolManager.setActiveTool("Polilinha"));

        ToggleButton arcBtn = CADIconFactory.createIconToggleButton(IconType.ARC, "Desenhar Arco Circular (A)", toolGroup);
        arcBtn.setOnAction(e -> toolManager.setActiveTool("Arco"));

        ToggleButton bezierBtn = CADIconFactory.createIconToggleButton(IconType.BEZIER, "Desenhar Curva Bézier (B)", toolGroup);
        bezierBtn.setOnAction(e -> toolManager.setActiveTool("Curva Bézier"));

        ToggleButton stitchBtn = CADIconFactory.createIconToggleButton(IconType.STITCH, "Costura Parametrizada de Couro (Chisel)", toolGroup);
        stitchBtn.setOnAction(e -> toolManager.setActiveTool("Costura Parametrizada"));

        ToggleButton dimHBtn = CADIconFactory.createIconToggleButton(IconType.DIM_HORIZONTAL, "Cota Horizontal", toolGroup);
        dimHBtn.setOnAction(e -> toolManager.setActiveTool("Cota Horizontal"));

        ToggleButton dimVBtn = CADIconFactory.createIconToggleButton(IconType.DIM_VERTICAL, "Cota Vertical", toolGroup);
        dimVBtn.setOnAction(e -> toolManager.setActiveTool("Cota Vertical"));

        ToggleButton dimRBtn = CADIconFactory.createIconToggleButton(IconType.DIM_RADIUS, "Cota Raio", toolGroup);
        dimRBtn.setOnAction(e -> toolManager.setActiveTool("Cota Raio"));

        Button moveBtn = CADIconFactory.createIconButton(IconType.MOVE, "Mover Elementos Selecionados");
        moveBtn.setOnAction(e -> { document.moveSelected(10.0, 10.0); viewport.redraw(); });

        Button copyBtn = CADIconFactory.createIconButton(IconType.COPY, "Copiar / Duplicar Elementos");
        copyBtn.setOnAction(e -> { document.copySelected(10.0, 10.0); viewport.redraw(); });

        Button rotBtn = CADIconFactory.createIconButton(IconType.ROTATE, "Rotacionar 90°");
        rotBtn.setOnAction(e -> { document.rotateSelected(90.0); viewport.redraw(); });

        Button mirBtn = CADIconFactory.createIconButton(IconType.MIRROR, "Espelhar Clone Simétrico");
        mirBtn.setOnAction(e -> { document.mirrorSelectedHorizontal(); viewport.redraw(); });

        Button delBtn = CADIconFactory.createIconButton(IconType.DELETE, "Excluir Elementos Selecionados (DEL)");
        delBtn.setOnAction(e -> { document.deleteSelected(); viewport.redraw(); });

        Button explodeBtn = CADIconFactory.createIconButton(IconType.EXPLODE, "Explodir Retângulo em 4 Linhas Individuais (Ctrl+E)");
        explodeBtn.setOnAction(e -> { document.explodeSelected(); viewport.redraw(); });

        Button r5Btn = CADIconFactory.createIconButton(IconType.CORNER, "Arredondar Cantos / Fillet de Couro");
        r5Btn.setOnAction(e -> CornerRadiusDialog.showDialog(primaryStage, document, viewport, toolManager, 5.0));

        Button r10Btn = CADIconFactory.createIconButton(IconType.CORNER, "Arredondar Cantos de Couro (Raio 10mm)");
        r10Btn.setOnAction(e -> {
            boolean hasRect = document.getSelectedElementIds().stream().anyMatch(id -> document.findElementById(id) instanceof com.leathercad.core.model.RectElement);
            if (hasRect) {
                CornerRadiusDialog.showDialog(primaryStage, document, viewport, 10.0);
            } else {
                document.setCornerRadiusSelected(10.0);
                viewport.redraw();
            }
        });

        Button creaseBtn = CADIconFactory.createIconButton(IconType.CREASE, "Adicionar Vinco de Borda 1.5mm");
        creaseBtn.setOnAction(e -> {
            for (com.leathercad.core.model.CADElement elem : document.getElements()) {
                if (document.getSelectedElementIds().contains(elem.id()) && elem instanceof com.leathercad.core.model.LineElement lineElem) {
                    com.leathercad.core.leather.CreaseElement c = new com.leathercad.core.leather.CreaseElement(
                        document.getActiveLayer().getId(),
                        lineElem.line().offset(1.5),
                        1.5
                    );
                    document.addElement(c);
                }
            }
            viewport.redraw();
        });


        MenuButton compMenuBtn = new MenuButton("🧩 Componentes");
        MenuItem cardSlot = new MenuItem("💳 Porta-Cartão");
        cardSlot.setOnAction(e -> {
            var elems = com.leathercad.core.components.CardSlotComponent.createCardSlot(document.getActiveLayer().getId(), new com.leathercad.core.geometry.Point2D(50, 50), 95.0, 55.0);
            for (var elem : elems) document.addElement(elem);
            viewport.redraw();
        });
        MenuItem idWin = new MenuItem("🪪 Janela Transparente ID");
        idWin.setOnAction(e -> {
            var elems = com.leathercad.core.components.IDWindowComponent.createIDWindow(document.getActiveLayer().getId(), new com.leathercad.core.geometry.Point2D(60, 60));
            for (var elem : elems) document.addElement(elem);
            viewport.redraw();
        });
        MenuItem coinPocket = new MenuItem("👛 Porta-Moedas com Lapela");
        coinPocket.setOnAction(e -> {
            var elems = com.leathercad.core.components.CoinPocketComponent.createCoinPocket(document.getActiveLayer().getId(), new com.leathercad.core.geometry.Point2D(70, 70), 100.0, 80.0);
            for (var elem : elems) document.addElement(elem);
            viewport.redraw();
        });
        MenuItem hiddenPocket = new MenuItem("🔒 Compartimento Oculto");
        hiddenPocket.setOnAction(e -> {
            var elems = com.leathercad.core.components.CoinPocketComponent.createHiddenPocket(document.getActiveLayer().getId(), new com.leathercad.core.geometry.Point2D(80, 80), 95.0, 60.0);
            for (var elem : elems) document.addElement(elem);
            viewport.redraw();
        });
        MenuItem magSnap = new MenuItem("🧲 Fecho Magnético (14mm)");
        magSnap.setOnAction(e -> {
            var elems = com.leathercad.core.components.HardwareComponent.createMagneticSnap(document.getActiveLayer().getId(), new com.leathercad.core.geometry.Point2D(100, 100), 14.0);
            for (var elem : elems) document.addElement(elem);
            viewport.redraw();
        });
        MenuItem pressStud = new MenuItem("🔘 Botão de Pressão (12mm)");
        pressStud.setOnAction(e -> {
            var elems = com.leathercad.core.components.HardwareComponent.createPressStud(document.getActiveLayer().getId(), new com.leathercad.core.geometry.Point2D(120, 120), 12.0);
            for (var elem : elems) document.addElement(elem);
            viewport.redraw();
        });
        MenuItem zipper = new MenuItem("🔩 Zíper Parametrizado (150mm)");
        zipper.setOnAction(e -> {
            var elems = com.leathercad.core.components.HardwareComponent.createZipper(document.getActiveLayer().getId(), new com.leathercad.core.geometry.Point2D(40, 150), 150.0, 30.0);
            for (var elem : elems) document.addElement(elem);
            viewport.redraw();
        });
        MenuItem elastic = new MenuItem("🎗️ Tira de Elástico (100mm)");
        elastic.setOnAction(e -> {
            var elems = com.leathercad.core.components.HardwareComponent.createElasticBand(document.getActiveLayer().getId(), new com.leathercad.core.geometry.Point2D(50, 180), 20.0, 100.0);
            for (var elem : elems) document.addElement(elem);
            viewport.redraw();
        });
        compMenuBtn.getItems().addAll(cardSlot, idWin, coinPocket, hiddenPocket, new SeparatorMenuItem(), magSnap, pressStud, zipper, elastic);

        MenuButton advMenuBtn = new MenuButton("⚙️ Ferramentas Avançadas");
        MenuItem paramItem = new MenuItem("📐 Projeto Paramétrico 1-Click...");
        paramItem.setOnAction(e -> ParametricDialog.showDialog(primaryStage, document, viewport));
        MenuItem simBtnItem = new MenuItem("🎬 Simulação de Montagem & Dobras...");
        simBtnItem.setOnAction(e -> SimulationDialog.showDialog(primaryStage, document));
        MenuItem matBtnItem = new MenuItem("🐂 Biblioteca de Couros & Materiais...");
        matBtnItem.setOnAction(e -> MaterialManagerDialog.showDialog(primaryStage, document, materialLibrary, viewport));
        MenuItem renderStudioItem = new MenuItem("📸 Estúdio de Render Fotorrealista...");
        renderStudioItem.setOnAction(e -> RealisticRenderDialog.showDialog(primaryStage, document));
        MenuItem nestingItem = new MenuItem("🧩 Corte Inteligente (Nesting)...");
        nestingItem.setOnAction(e -> NestingDialog.showDialog(primaryStage, document));
        MenuItem prodBtnItem = new MenuItem("🏭 Ficha Técnica & Lista de Peças (BOM)...");
        prodBtnItem.setOnAction(e -> ProductionReportDialog.showDialog(primaryStage, document));
        MenuItem printBtnItem = new MenuItem("🖨️ Impressão & Paginação 1:1...");
        printBtnItem.setOnAction(e -> PrintStudioDialog.showDialog(primaryStage, document));
        MenuItem aiBtnItem = new MenuItem("✨ Gerador de Moldes por IA (Ollama)...");
        aiBtnItem.setOnAction(e -> AIAssistantDialog.showDialog(primaryStage, document, viewport));
        advMenuBtn.getItems().addAll(paramItem, simBtnItem, matBtnItem, renderStudioItem, nestingItem, prodBtnItem, printBtnItem, aiBtnItem);

        Button clearBtn = createIconButton(Feather.X_CIRCLE, "Desmarcar Seleção (ESC)");
        clearBtn.setOnAction(e -> {
            document.clearSelection();
            viewport.redraw();
        });

        Button themeBtn = createIconButton(Feather.SUN, "Alternar Tema Claro / Escuro (Ctrl+T)");
        themeBtn.setOnAction(e -> toggleTheme(primaryStage.getScene()));

        return new ToolBar(
            newBtn, openBtn, saveBtn,
            new Separator(Orientation.VERTICAL),
            selectBtn, lineBtn, rectBtn, circleBtn, polylineBtn, arcBtn, bezierBtn, stitchBtn,
            new Separator(Orientation.VERTICAL),
            dimHBtn, dimVBtn, dimRBtn,
            new Separator(Orientation.VERTICAL),
            moveBtn, copyBtn, rotBtn, mirBtn, delBtn, explodeBtn,
            new Separator(Orientation.VERTICAL),
            r5Btn, r10Btn, creaseBtn,
            new Separator(Orientation.VERTICAL),
            compMenuBtn, advMenuBtn,
            new Separator(Orientation.VERTICAL),
            clearBtn, themeBtn
        );
    }

    private VBox layerBoxContainer;

    private VBox createLayerPanel() {
        VBox panel = new VBox(10);
        panel.setStyle("-fx-background-color: #252526; -fx-padding: 10; -fx-min-width: 230; -fx-border-color: #333333; -fx-border-width: 0 0 0 1;");

        Label title = new Label("Camadas (Layers)");
        title.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px;");

        layerBoxContainer = new VBox(6);
        refreshLayerPanel();

        document.setOnLayersChangedListener(this::refreshLayerPanel);

        Button addLayerBtn = new Button("➕ Nova Camada");
        addLayerBtn.setStyle("-fx-background-color: #333333; -fx-text-fill: white; -fx-font-size: 11px;");
        addLayerBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog("Nova Camada");
            dialog.setTitle("Nova Camada");
            dialog.setHeaderText("Digite o nome da nova camada:");
            dialog.showAndWait().ifPresent(name -> {
                if (!name.isBlank()) {
                    Layer newLayer = new Layer(name, "#00FF88", document.getLayers().size());
                    document.addLayer(newLayer);
                    document.setActiveLayer(newLayer);
                    refreshLayerPanel();
                }
            });
        });

        panel.getChildren().addAll(title, layerBoxContainer, addLayerBtn);
        return panel;
    }

    private void refreshLayerPanel() {
        if (layerBoxContainer == null) return;
        layerBoxContainer.getChildren().clear();

        ToggleGroup activeGroup = new ToggleGroup();

        for (Layer l : document.getLayers()) {
            HBox row = new HBox(6);
            row.setAlignment(Pos.CENTER_LEFT);

            RadioButton activeRadio = new RadioButton();
            activeRadio.setToggleGroup(activeGroup);
            activeRadio.setSelected(l.equals(document.getActiveLayer()));
            activeRadio.setOnAction(e -> document.setActiveLayer(l));

            Button visBtn = new Button(l.isVisible() ? "👁" : "🙈");
            visBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-padding: 2 4 2 4;");
            visBtn.setOnAction(e -> {
                l.setVisible(!l.isVisible());
                visBtn.setText(l.isVisible() ? "👁" : "🙈");
                if (!l.isVisible()) {
                    document.clearSelection();
                }
                viewport.redraw();
            });

            Button lockBtn = new Button(l.isLocked() ? "🔒" : "🔓");
            lockBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-padding: 2 4 2 4;");
            lockBtn.setOnAction(e -> {
                l.setLocked(!l.isLocked());
                lockBtn.setText(l.isLocked() ? "🔒" : "🔓");
                if (l.isLocked()) {
                    document.clearSelection();
                }
                viewport.redraw();
            });

            ColorPicker colorPicker = new ColorPicker(Color.web(l.getColorHex()));
            colorPicker.setStyle("-fx-color-label-visible: false; -fx-pref-width: 32; -fx-pref-height: 22;");
            colorPicker.setOnAction(e -> {
                String hex = String.format("#%02X%02X%02X",
                    (int) (colorPicker.getValue().getRed() * 255),
                    (int) (colorPicker.getValue().getGreen() * 255),
                    (int) (colorPicker.getValue().getBlue() * 255));
                l.setColorHex(hex);
                viewport.redraw();
            });

            Label nameLabel = new Label(l.getName());
            nameLabel.setStyle("-fx-text-fill: " + (l.equals(document.getActiveLayer()) ? "#00FF88" : "white") + "; -fx-font-size: 11px;");

            row.getChildren().addAll(activeRadio, visBtn, lockBtn, colorPicker, nameLabel);
            layerBoxContainer.getChildren().add(row);
        }
    }

    private HBox createStatusBar() {
        HBox bar = new HBox();
        bar.getStyleClass().add("status-bar");

        statusLabel = new Label(" X: 0.00 mm   Y: 0.00 mm");
        zoomLabel = new Label("Zoom: 100% ");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        bar.getChildren().addAll(statusLabel, spacer, zoomLabel);
        return bar;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
