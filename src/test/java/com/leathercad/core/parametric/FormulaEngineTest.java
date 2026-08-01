package com.leathercad.core.parametric;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FormulaEngineTest {

    @Test
    public void testBasicArithmetic() {
        Map<String, Double> vars = new HashMap<>();
        double val = FormulaEngine.evaluate("10 + 20 * 2", vars);
        assertEquals(50.0, val, 1e-4);
    }

    @Test
    public void testVariablesAndPI() {
        Map<String, Double> vars = new HashMap<>();
        vars.put("card_width", 85.0);
        vars.put("margin", 3.85);

        double val = FormulaEngine.evaluate("card_width + (2 * margin)", vars);
        assertEquals(92.7, val, 1e-4);
    }

    @Test
    public void testVariableTableCascadingUpdates() {
        VariableTable table = new VariableTable();
        table.updateVariableValue("margin", 5.0);
        table.updateVariableValue("leather_thickness", 2.0);

        // total_width = card_width(85) + (2 * margin(5)) + (2 * leather_thickness(2)) = 85 + 10 + 4 = 99.0
        double totalWidth = table.getValue("total_width");
        assertEquals(99.0, totalWidth, 1e-4);
    }
}
