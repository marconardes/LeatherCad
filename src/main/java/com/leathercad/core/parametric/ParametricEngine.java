package com.leathercad.core.parametric;

import com.leathercad.core.model.Document;

public class ParametricEngine {

    private final VariableTable variableTable;
    private ParametricTemplate activeTemplate;

    public ParametricEngine() {
        this.variableTable = new VariableTable();
    }

    public VariableTable getVariableTable() {
        return variableTable;
    }

    public ParametricTemplate getActiveTemplate() {
        return activeTemplate;
    }

    public void setActiveTemplate(ParametricTemplate template) {
        this.activeTemplate = template;
        if (template != null) {
            VariableTable defaults = template.createDefaultVariables();
            for (Variable v : defaults.getAllVariables()) {
                variableTable.addVariable(v);
            }
        }
    }

    public void updateVariable(String name, double newValue, Document doc) {
        variableTable.updateVariableValue(name, newValue);
        reevaluateDocument(doc, true);
    }

    public void updateExpression(String name, String expression, Document doc) {
        variableTable.updateVariableExpression(name, expression);
        reevaluateDocument(doc, true);
    }

    public void reevaluateDocument(Document doc, boolean showDimensions) {
        variableTable.recalculateAll();
        if (activeTemplate != null) {
            activeTemplate.generate(doc, variableTable, showDimensions);
        }
    }
}
