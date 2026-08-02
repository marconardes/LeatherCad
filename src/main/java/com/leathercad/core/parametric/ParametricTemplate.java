package com.leathercad.core.parametric;

import com.leathercad.core.model.Document;

public interface ParametricTemplate {
    String getId();
    String getName();
    String getDescription();
    VariableTable createDefaultVariables();
    void generate(Document doc, VariableTable vars, boolean showDimensions);
}
