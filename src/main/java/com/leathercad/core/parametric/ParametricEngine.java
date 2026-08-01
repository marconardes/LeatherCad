package com.leathercad.core.parametric;

import com.leathercad.core.model.Document;

public class ParametricEngine {

    private final VariableTable variableTable;

    public ParametricEngine() {
        this.variableTable = new VariableTable();
    }

    public VariableTable getVariableTable() {
        return variableTable;
    }

    public void updateVariable(String name, double newValue, Document doc) {
        variableTable.updateVariableValue(name, newValue);
        reevaluateDocument(doc);
    }

    public void updateExpression(String name, String expression, Document doc) {
        variableTable.updateVariableExpression(name, expression);
        reevaluateDocument(doc);
    }

    public void reevaluateDocument(Document doc) {
        variableTable.recalculateAll();
        // Em modelos parametrizados, reavalia os elementos vinculados
    }
}
