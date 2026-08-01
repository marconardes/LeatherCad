package com.leathercad.core.parametric;

import java.util.*;

public class VariableTable {
    private final Map<String, Variable> variables = new LinkedHashMap<>();

    public VariableTable() {
        // Inicializa com variáveis padrão de marcenaria de couro
        addVariable(new Variable("card_width", 85.0, "mm", "Largura padrão ISO do cartão"));
        addVariable(new Variable("card_height", 55.0, "mm", "Altura padrão ISO do cartão"));
        addVariable(new Variable("margin", 3.85, "mm", "Margem de costura lateral"));
        addVariable(new Variable("leather_thickness", 1.4, "mm", "Espessura do couro"));
        addVariable(new Variable("fold_allowance", "PI * leather_thickness * 1.2", 0.0, "mm", "Folga técnica de dobra"));
        addVariable(new Variable("total_width", "card_width + (2 * margin) + (2 * leather_thickness)", 0.0, "mm", "Largura total parametrizada"));
        recalculateAll();
    }

    public void addVariable(Variable var) {
        variables.put(var.getName(), var);
        recalculateAll();
    }

    public void removeVariable(String name) {
        variables.remove(name);
        recalculateAll();
    }

    public Variable getVariable(String name) {
        return variables.get(name);
    }

    public Collection<Variable> getAllVariables() {
        return variables.values();
    }

    public double getValue(String name) {
        Variable var = variables.get(name);
        return var != null ? var.getValue() : 0.0;
    }

    public void updateVariableValue(String name, double newValue) {
        Variable var = variables.get(name);
        if (var != null) {
            var.setExpression(String.valueOf(newValue));
            var.setValue(newValue);
            recalculateAll();
        }
    }

    public void updateVariableExpression(String name, String expression) {
        Variable var = variables.get(name);
        if (var != null) {
            var.setExpression(expression);
            recalculateAll();
        }
    }

    public void recalculateAll() {
        Map<String, Double> evaluated = new HashMap<>();

        // Multi-pass re-evaluation para resolver dependências em cascata
        for (int pass = 0; pass < 5; pass++) {
            for (Variable var : variables.values()) {
                try {
                    double val = FormulaEngine.evaluate(var.getExpression(), evaluated);
                    var.setValue(val);
                    evaluated.put(var.getName(), val);
                } catch (Exception ignored) {
                    evaluated.put(var.getName(), var.getValue());
                }
            }
        }
    }

    public Map<String, Double> asValueMap() {
        Map<String, Double> map = new HashMap<>();
        for (Variable v : variables.values()) {
            map.put(v.getName(), v.getValue());
        }
        return map;
    }
}
