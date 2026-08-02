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
        CADTool dimTool = new DimensionTool(DimensionElement.DimensionType.LINEAR);
        CADTool dimH = new DimensionTool(DimensionElement.DimensionType.HORIZONTAL);
        CADTool dimV = new DimensionTool(DimensionElement.DimensionType.VERTICAL);
        CADTool dimR = new DimensionTool(DimensionElement.DimensionType.RADIUS);
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
        tools.put(dimTool.getName(), dimTool);
        tools.put(dimH.getName(), dimH);
        tools.put(dimV.getName(), dimV);
        tools.put(dimR.getName(), dimR);
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
        }
    }

    public void resetActiveTool() {
        if (activeTool != null) {
            activeTool.reset();
        }
        activeTool = tools.get("Seleção");
    }
}
