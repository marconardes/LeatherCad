package com.leathercad.ui.tools;

import com.leathercad.core.model.DimensionElement;

import java.util.HashMap;
import java.util.Map;

public class ToolManager {
    private final Map<String, CADTool> tools = new HashMap<>();
    private CADTool activeTool;

    public ToolManager() {
        CADTool selectTool = new SelectTool();
        CADTool lineTool = new LineTool();
        CADTool rectTool = new RectTool();
        CADTool circleTool = new CircleTool();
        CADTool arcTool = new ArcTool();
        CADTool bezierTool = new BezierTool();
        CADTool polylineTool = new PolylineTool();
        DimensionTool dimTool = new DimensionTool(com.leathercad.core.model.DimensionElement.DimensionType.LINEAR);
        CADTool stitchTool = new StitchTool();
        CADTool filletTool = new FilletTool();
        CADTool offsetTool = new OffsetTool();

        tools.put(selectTool.getName(), selectTool);
        tools.put(lineTool.getName(), lineTool);
        tools.put(rectTool.getName(), rectTool);
        tools.put(circleTool.getName(), circleTool);
        tools.put(arcTool.getName(), arcTool);
        tools.put(bezierTool.getName(), bezierTool);
        tools.put(polylineTool.getName(), polylineTool);

        tools.put("Cotagem e Ficha Técnica", dimTool);
        tools.put("Cotagem", dimTool);
        tools.put("Cota", dimTool);
        tools.put("Cota Linear (D)", dimTool);
        tools.put("Cota Linear", dimTool);
        tools.put("Cota Horizontal", dimTool);
        tools.put("Cota Vertical", dimTool);
        tools.put("Cota Raio", dimTool);
        tools.put("Nota Técnica (T)", dimTool);

        tools.put(stitchTool.getName(), stitchTool);
        tools.put(filletTool.getName(), filletTool);
        tools.put(offsetTool.getName(), offsetTool);

        activeTool = selectTool;
    }

    public CADTool getActiveTool() {
        return activeTool;
    }

    public void setActiveTool(String name) {
        if (tools.containsKey(name)) {
            activeTool = tools.get(name);
            if (activeTool instanceof DimensionTool dimTool) {
                if (name.contains("Raio")) {
                    dimTool.setCurrentType(com.leathercad.core.model.DimensionElement.DimensionType.RADIUS);
                } else if (name.contains("Horizontal")) {
                    dimTool.setCurrentType(com.leathercad.core.model.DimensionElement.DimensionType.HORIZONTAL);
                } else if (name.contains("Vertical")) {
                    dimTool.setCurrentType(com.leathercad.core.model.DimensionElement.DimensionType.VERTICAL);
                } else if (name.contains("Nota")) {
                    dimTool.setCurrentType(com.leathercad.core.model.DimensionElement.DimensionType.CALLOUT_NOTE);
                } else {
                    dimTool.setCurrentType(com.leathercad.core.model.DimensionElement.DimensionType.LINEAR);
                }
            }
        }
    }

    public void resetActiveTool() {
        if (activeTool != null) {
            activeTool.reset();
        }
        activeTool = tools.get("Seleção");
    }
}
