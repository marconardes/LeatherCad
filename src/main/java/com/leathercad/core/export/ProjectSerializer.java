package com.leathercad.core.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.leathercad.core.geometry.*;
import com.leathercad.core.leather.CreaseElement;
import com.leathercad.core.leather.StitchConfig;
import com.leathercad.core.leather.StitchElement;
import com.leathercad.core.leather.StitchType;
import com.leathercad.core.model.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectSerializer {
    private static final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public static void saveToFile(Document document, File file) throws IOException {
        Map<String, Object> root = new HashMap<>();
        root.put("version", "1.0");

        // Camadas
        List<Map<String, Object>> layersList = new ArrayList<>();
        for (Layer layer : document.getLayers()) {
            Map<String, Object> lMap = new HashMap<>();
            lMap.put("id", layer.getId());
            lMap.put("name", layer.getName());
            lMap.put("colorHex", layer.getColorHex());
            lMap.put("visible", layer.isVisible());
            lMap.put("locked", layer.isLocked());
            lMap.put("zIndex", layer.getZIndex());
            layersList.add(lMap);
        }
        root.put("layers", layersList);
        root.put("activeLayerId", document.getActiveLayer().getId());

        // Elementos Geométricos
        List<Map<String, Object>> elementsList = new ArrayList<>();
        for (CADElement elem : document.getElements()) {
            Map<String, Object> eMap = new HashMap<>();
            eMap.put("id", elem.id());
            eMap.put("layerId", elem.layerId());

            if (elem instanceof RectElement r) {
                eMap.put("type", "RECT");
                eMap.put("x", r.rect().minPoint().x());
                eMap.put("y", r.rect().minPoint().y());
                eMap.put("w", r.rect().width());
                eMap.put("h", r.rect().height());
                eMap.put("r", r.rect().cornerRadius());
            } else if (elem instanceof CircleElement c) {
                eMap.put("type", "CIRCLE");
                eMap.put("cx", c.circle().center().x());
                eMap.put("cy", c.circle().center().y());
                eMap.put("radius", c.circle().radius());
            } else if (elem instanceof LineElement l) {
                eMap.put("type", "LINE");
                eMap.put("x1", l.line().start().x());
                eMap.put("y1", l.line().start().y());
                eMap.put("x2", l.line().end().x());
                eMap.put("y2", l.line().end().y());
            } else if (elem instanceof ArcElement a) {
                eMap.put("type", "ARC");
                eMap.put("cx", a.arc().center().x());
                eMap.put("cy", a.arc().center().y());
                eMap.put("radius", a.arc().radius());
                eMap.put("startAngle", a.arc().startAngleDegrees());
                eMap.put("sweepAngle", a.arc().sweepAngleDegrees());
            } else if (elem instanceof BezierElement b) {
                eMap.put("type", "BEZIER");
                eMap.put("sx", b.bezier().start().x());
                eMap.put("sy", b.bezier().start().y());
                eMap.put("c1x", b.bezier().control1().x());
                eMap.put("c1y", b.bezier().control1().y());
                eMap.put("c2x", b.bezier().control2().x());
                eMap.put("c2y", b.bezier().control2().y());
                eMap.put("ex", b.bezier().end().x());
                eMap.put("ey", b.bezier().end().y());
            } else if (elem instanceof PolylineElement p) {
                eMap.put("type", "POLYLINE");
                eMap.put("closed", p.polyline().isClosed());
                List<Map<String, Double>> pts = new ArrayList<>();
                for (Point2D pt : p.polyline().points()) {
                    Map<String, Double> ptMap = new HashMap<>();
                    ptMap.put("x", pt.x());
                    ptMap.put("y", pt.y());
                    pts.add(ptMap);
                }
                eMap.put("points", pts);
            } else if (elem instanceof StitchElement s) {
                eMap.put("type", "STITCH");
                eMap.put("x1", s.baseLine().start().x());
                eMap.put("y1", s.baseLine().start().y());
                eMap.put("x2", s.baseLine().end().x());
                eMap.put("y2", s.baseLine().end().y());
                eMap.put("pitch", s.config().pitchMm());
                eMap.put("margin", s.config().marginMm());
                eMap.put("holeDiameter", s.config().holeDiameterMm());
                eMap.put("stitchType", s.config().type().name());
                eMap.put("angle", s.config().angleDegrees());
            } else if (elem instanceof CreaseElement cr) {
                eMap.put("type", "CREASE");
                eMap.put("x1", cr.line().start().x());
                eMap.put("y1", cr.line().start().y());
                eMap.put("x2", cr.line().end().x());
                eMap.put("y2", cr.line().end().y());
                eMap.put("margin", cr.marginMm());
                eMap.put("depth", cr.depthMm());
            } else if (elem instanceof DimensionElement d) {
                eMap.put("type", "DIMENSION");
                eMap.put("x1", d.start().x());
                eMap.put("y1", d.start().y());
                eMap.put("x2", d.end().x());
                eMap.put("y2", d.end().y());
                eMap.put("dimType", d.type().name());
                eMap.put("offset", d.offsetMm());
                eMap.put("label", d.customLabel());
            }

            elementsList.add(eMap);
        }
        root.put("elements", elementsList);

        mapper.writeValue(file, root);
    }

    @SuppressWarnings("unchecked")
    public static Document loadFromFile(File file) throws IOException {
        Map<String, Object> root = mapper.readValue(file, Map.class);
        Document doc = new Document();
        doc.clearElements();

        // Carregar Camadas
        List<Map<String, Object>> layersList = (List<Map<String, Object>>) root.get("layers");
        if (layersList != null && !layersList.isEmpty()) {
            for (Map<String, Object> lMap : layersList) {
                String name = (String) lMap.get("name");
                String colorHex = (String) lMap.get("colorHex");
                boolean visible = (boolean) lMap.getOrDefault("visible", true);
                boolean locked = (boolean) lMap.getOrDefault("locked", false);
                int zIndex = ((Number) lMap.getOrDefault("zIndex", 0)).intValue();

                Layer layer = doc.findLayerById(name);
                if (layer == null) {
                    layer = new Layer(name, colorHex != null ? colorHex : "#FFFFFF", zIndex);
                    doc.addLayer(layer);
                }
                layer.setVisible(visible);
                layer.setLocked(locked);
            }
        }

        String activeLayerId = (String) root.get("activeLayerId");
        if (activeLayerId != null) {
            Layer active = doc.findLayerById(activeLayerId);
            if (active != null) doc.setActiveLayer(active);
        }

        // Carregar Elementos Geométricos
        List<Map<String, Object>> elementsList = (List<Map<String, Object>>) root.get("elements");
        if (elementsList != null) {
            for (Map<String, Object> eMap : elementsList) {
                String id = (String) eMap.get("id");
                String layerId = (String) eMap.get("layerId");
                String type = (String) eMap.get("type");
                if (type == null) continue;

                switch (type) {
                    case "RECT" -> {
                        double x = ((Number) eMap.get("x")).doubleValue();
                        double y = ((Number) eMap.get("y")).doubleValue();
                        double w = ((Number) eMap.get("w")).doubleValue();
                        double h = ((Number) eMap.get("h")).doubleValue();
                        double r = ((Number) eMap.getOrDefault("r", 0.0)).doubleValue();
                        doc.addElement(new RectElement(id, layerId, new Rect2D(x, y, w, h, r)));
                    }
                    case "CIRCLE" -> {
                        double cx = ((Number) eMap.get("cx")).doubleValue();
                        double cy = ((Number) eMap.get("cy")).doubleValue();
                        double radius = ((Number) eMap.get("radius")).doubleValue();
                        doc.addElement(new CircleElement(id, layerId, new Circle2D(new Point2D(cx, cy), radius)));
                    }
                    case "LINE" -> {
                        double x1 = ((Number) eMap.get("x1")).doubleValue();
                        double y1 = ((Number) eMap.get("y1")).doubleValue();
                        double x2 = ((Number) eMap.get("x2")).doubleValue();
                        double y2 = ((Number) eMap.get("y2")).doubleValue();
                        doc.addElement(new LineElement(id, layerId, new LineSegment(new Point2D(x1, y1), new Point2D(x2, y2))));
                    }
                    case "ARC" -> {
                        double cx = ((Number) eMap.get("cx")).doubleValue();
                        double cy = ((Number) eMap.get("cy")).doubleValue();
                        double radius = ((Number) eMap.get("radius")).doubleValue();
                        double startAngle = ((Number) eMap.get("startAngle")).doubleValue();
                        double sweepAngle = ((Number) eMap.get("sweepAngle")).doubleValue();
                        doc.addElement(new ArcElement(id, layerId, new Arc2D(new Point2D(cx, cy), radius, startAngle, sweepAngle)));
                    }
                    case "BEZIER" -> {
                        double sx = ((Number) eMap.get("sx")).doubleValue();
                        double sy = ((Number) eMap.get("sy")).doubleValue();
                        double c1x = ((Number) eMap.get("c1x")).doubleValue();
                        double c1y = ((Number) eMap.get("c1y")).doubleValue();
                        double c2x = ((Number) eMap.get("c2x")).doubleValue();
                        double c2y = ((Number) eMap.get("c2y")).doubleValue();
                        double ex = ((Number) eMap.get("ex")).doubleValue();
                        double ey = ((Number) eMap.get("ey")).doubleValue();
                        Bezier2D b2d = new Bezier2D(new Point2D(sx, sy), new Point2D(c1x, c1y), new Point2D(c2x, c2y), new Point2D(ex, ey));
                        doc.addElement(new BezierElement(id, layerId, b2d));
                    }
                    case "POLYLINE" -> {
                        boolean closed = (boolean) eMap.getOrDefault("closed", false);
                        List<Map<String, Number>> ptsMap = (List<Map<String, Number>>) eMap.get("points");
                        List<Point2D> pts = new ArrayList<>();
                        if (ptsMap != null) {
                            for (Map<String, Number> p : ptsMap) {
                                pts.add(new Point2D(p.get("x").doubleValue(), p.get("y").doubleValue()));
                            }
                        }
                        doc.addElement(new PolylineElement(id, layerId, new Polyline2D(pts, closed)));
                    }
                    case "STITCH" -> {
                        double x1 = ((Number) eMap.get("x1")).doubleValue();
                        double y1 = ((Number) eMap.get("y1")).doubleValue();
                        double x2 = ((Number) eMap.get("x2")).doubleValue();
                        double y2 = ((Number) eMap.get("y2")).doubleValue();
                        double pitch = ((Number) eMap.getOrDefault("pitch", 3.85)).doubleValue();
                        double margin = ((Number) eMap.getOrDefault("margin", 3.85)).doubleValue();
                        double holeDiameter = ((Number) eMap.getOrDefault("holeDiameter", 1.0)).doubleValue();
                        String stType = (String) eMap.getOrDefault("stitchType", "FRENCH");
                        double angle = ((Number) eMap.getOrDefault("angle", 45.0)).doubleValue();

                        StitchType typeEnum = StitchType.valueOf(stType);
                        StitchConfig cfg = new StitchConfig(margin, pitch, holeDiameter, angle, typeEnum);
                        doc.addElement(new StitchElement(layerId, new LineSegment(new Point2D(x1, y1), new Point2D(x2, y2)), cfg));
                    }
                    case "CREASE" -> {
                        double x1 = ((Number) eMap.get("x1")).doubleValue();
                        double y1 = ((Number) eMap.get("y1")).doubleValue();
                        double x2 = ((Number) eMap.get("x2")).doubleValue();
                        double y2 = ((Number) eMap.get("y2")).doubleValue();
                        double margin = ((Number) eMap.getOrDefault("margin", 1.5)).doubleValue();
                        double depth = ((Number) eMap.getOrDefault("depth", 0.5)).doubleValue();
                        doc.addElement(new CreaseElement(id, layerId, new LineSegment(new Point2D(x1, y1), new Point2D(x2, y2)), margin, depth));
                    }
                    case "DIMENSION" -> {
                        double x1 = ((Number) eMap.get("x1")).doubleValue();
                        double y1 = ((Number) eMap.get("y1")).doubleValue();
                        double x2 = ((Number) eMap.get("x2")).doubleValue();
                        double y2 = ((Number) eMap.get("y2")).doubleValue();
                        String dType = (String) eMap.getOrDefault("dimType", "HORIZONTAL");
                        double offset = ((Number) eMap.getOrDefault("offset", 5.0)).doubleValue();
                        String label = (String) eMap.get("label");
                        DimensionElement.DimensionType dimEnum = DimensionElement.DimensionType.valueOf(dType);
                        doc.addElement(new DimensionElement(id, layerId, new Point2D(x1, y1), new Point2D(x2, y2), dimEnum, offset, label));
                    }
                }
            }
        }

        return doc;
    }
}
