package com.leathercad.ui.panels;

import com.leathercad.core.parametric.Variable;
import com.leathercad.core.parametric.VariableTable;
import com.leathercad.ui.viewport.CanvasViewport;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.converter.DoubleStringConverter;

public class VariableTablePanel extends VBox {

    private final VariableTable variableTable;
    private final CanvasViewport viewport;
    private final TableView<Variable> tableView;
    private final ObservableList<Variable> masterData;

    public VariableTablePanel(VariableTable variableTable, CanvasViewport viewport) {
        this.variableTable = variableTable;
        this.viewport = viewport;
        this.masterData = FXCollections.observableArrayList(variableTable.getAllVariables());

        setStyle("-fx-background-color: #252526; -fx-padding: 10; -fx-min-width: 280; -fx-border-color: #333333; -fx-border-width: 0 0 0 1;");
        setSpacing(10);

        Label title = new Label("📐 Tabela de Variáveis (Fusion 360)");
        title.getStyleClass().add("title-label");

        tableView = new TableView<>();
        tableView.setEditable(true);
        tableView.setItems(masterData);

        // Coluna 1: Nome
        TableColumn<Variable, String> nameCol = new TableColumn<>("Variável");
        nameCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName()));
        nameCol.setCellFactory(TextFieldTableCell.forTableColumn());
        nameCol.setOnEditCommit(event -> {
            Variable var = event.getRowValue();
            var.setName(event.getNewValue());
            variableTable.recalculateAll();
            viewport.redraw();
        });

        // Coluna 2: Expressão / Valor
        TableColumn<Variable, String> exprCol = new TableColumn<>("Expressão / Valor");
        exprCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getExpression()));
        exprCol.setCellFactory(TextFieldTableCell.forTableColumn());
        exprCol.setOnEditCommit(event -> {
            Variable var = event.getRowValue();
            variableTable.updateVariableExpression(var.getName(), event.getNewValue());
            refreshTable();
            viewport.redraw();
        });

        // Coluna 3: Valor Avaliado
        TableColumn<Variable, Number> valCol = new TableColumn<>("Resultado (mm)");
        valCol.setCellValueFactory(cell -> new SimpleDoubleProperty(cell.getValue().getValue()));

        tableView.getColumns().addAll(nameCol, exprCol, valCol);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        // Controles de Adição de Variável
        TextField nameInput = new TextField();
        nameInput.setPromptText("Nome (ex: folga)");
        nameInput.setPrefWidth(90);

        TextField exprInput = new TextField();
        exprInput.setPromptText("Valor/Expressão");
        exprInput.setPrefWidth(100);

        Button addBtn = new Button("➕ Add");
        addBtn.setOnAction(e -> {
            if (!nameInput.getText().isBlank()) {
                Variable newVar = new Variable(nameInput.getText().trim(), exprInput.getText().trim(), 0.0, "mm", "Variável personalizada");
                variableTable.addVariable(newVar);
                nameInput.clear();
                exprInput.clear();
                refreshTable();
                viewport.redraw();
            }
        });

        HBox addBox = new HBox(5, nameInput, exprInput, addBtn);
        addBox.setPadding(new Insets(5, 0, 0, 0));

        getChildren().addAll(title, tableView, addBox);
    }

    public void refreshTable() {
        masterData.setAll(variableTable.getAllVariables());
        tableView.refresh();
    }
}
