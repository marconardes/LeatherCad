package com.leathercad.ui.icons;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

public class CADIconFactory {

    public enum IconType {
        NEW_FILE("M 6,2 h 8 l 5,5 v 13 h -13 Z M 14,2 v 5 h 5 M 12,10 v 6 M 9,13 h 6"),
        OPEN_FILE("M 3,7 h 5 l 2,3 h 11 v 10 h -18 Z"),
        SAVE_FILE("M 4,4 h 13 l 3,3 v 13 h -16 Z M 8,4 v 6 h 7 v -6 M 8,15 h 8 v 5 h -8 Z"),
        
        SELECT("M 3,3 L 10,21 L 13,14 L 20,11 Z M 16,12 L 16,20 M 12,16 L 20,16"),
        LINE("M 4,20 L 20,4 M 2,18 h 4 v 4 h -4 Z M 18,2 h 4 v 4 h -4 Z"),
        RECTANGLE("M 4,5 h 16 v 14 h -16 Z M 2,3 h 4 v 4 h -4 Z M 18,3 h 4 v 4 h -4 Z M 2,17 h 4 v 4 h -4 Z M 18,17 h 4 v 4 h -4 Z"),
        CIRCLE("M 12,4 A 8,8 0 1,1 11.99,4 Z M 12,8 L 12,16 M 8,12 L 16,12 M 19,11 h 2 v 2 h -2 Z"),
        POLYLINE("M 4,18 L 10,6 L 16,14 L 21,4 M 2,16 h 4 v 4 h -4 Z M 8,4 h 4 v 4 h -4 Z M 14,12 h 4 v 4 h -4 Z M 19,2 h 4 v 4 h -4 Z"),
        ARC("M 4,17 A 10,10 0 0,1 19,6 M 8,18 L 16,18 L 19,6 M 7,17 h 2 v 2 h -2 Z M 11,7 h 2 v 2 h -2 Z M 18,5 h 2 v 2 h -2 Z"),
        BEZIER("M 4,18 C 8,4 16,20 20,6 M 4,18 L 8,4 M 20,6 L 16,20 M 7,3 h 2 v 2 h -2 Z M 15,19 h 2 v 2 h -2 Z M 2,16 h 4 v 4 h -4 Z M 18,4 h 4 v 4 h -4 Z"),
        STITCH("M 3,12 L 21,12 M 5,9 L 8,15 M 11,9 L 14,15 M 17,9 L 20,15"),
        
        DIM_HORIZONTAL("M 4,4 L 4,16 M 20,4 L 20,16 M 4,12 L 20,12 M 7,9 L 4,12 L 7,15 M 17,9 L 20,12 L 17,15"),
        DIM_VERTICAL("M 4,4 L 16,4 M 4,20 L 16,20 M 12,4 L 12,20 M 9,7 L 12,4 L 15,7 M 9,17 L 12,20 L 15,17"),
        DIM_RADIUS("M 4,20 A 16,16 0 0,1 20,4 M 4,20 L 16,8 M 12,8 L 16,8 L 16,12"),
        
        MOVE("M 12,2 L 12,22 M 2,12 L 22,12 M 12,2 L 9,5 M 12,2 L 15,5 M 12,22 L 9,19 M 12,22 L 15,19 M 2,12 L 5,9 M 2,12 L 5,15 M 22,12 L 19,9 M 22,12 L 19,15"),
        COPY("M 4,8 h 10 v 12 h -10 Z M 9,4 h 10 v 12 h -10 Z"),
        ROTATE("M 12,4 A 8,8 0 1,1 4,12 M 4,6 L 4,12 L 10,12"),
        MIRROR("M 12,2 L 12,22 M 4,6 L 9,12 L 4,18 M 20,6 L 15,12 L 20,18"),
        DELETE("M 5,5 L 19,19 M 19,5 L 5,19"),
        EXPLODE("M 3,3 L 9,3 M 15,3 L 21,3 M 21,9 L 21,15 M 21,21 L 15,21 M 9,21 L 3,21 M 3,15 L 3,9"),
        CORNER("M 4,4 L 14,4 A 6,6 0 0,1 20,10 L 20,20 M 14,4 L 20,4 L 20,10"),
        CREASE("M 3,8 L 21,8 M 3,14 L 21,14");

        private final String pathData;

        IconType(String pathData) {
            this.pathData = pathData;
        }

        public String getPathData() {
            return pathData;
        }
    }

    public static Node createIcon(IconType iconType) {
        SVGPath path = new SVGPath();
        path.setContent(iconType.getPathData());
        path.getStyleClass().add("cad-icon-path");
        path.setStrokeLineCap(StrokeLineCap.ROUND);
        path.setStrokeLineJoin(StrokeLineJoin.ROUND);
        
        Group group = new Group(path);
        group.getStyleClass().add("cad-icon");
        return group;
    }

    public static Button createIconButton(IconType iconType, String tooltipText) {
        Node iconNode = createIcon(iconType);
        Button btn = new Button("", iconNode);
        btn.setTooltip(new Tooltip(tooltipText));
        btn.getStyleClass().add("icon-only-button");
        return btn;
    }

    public static ToggleButton createIconToggleButton(IconType iconType, String tooltipText, ToggleGroup group) {
        Node iconNode = createIcon(iconType);
        ToggleButton btn = new ToggleButton("", iconNode);
        btn.setTooltip(new Tooltip(tooltipText));
        btn.setToggleGroup(group);
        btn.getStyleClass().add("icon-only-button");
        return btn;
    }
}
