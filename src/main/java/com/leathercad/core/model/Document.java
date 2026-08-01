package com.leathercad.core.model;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Polyline2D;
import com.leathercad.core.geometry.Rect2D;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Document {
    private final List<Layer> layers = new CopyOnWriteArrayList<>();
    private final List<CADElement> elements = new CopyOnWriteArrayList<>();
    private Layer activeLayer;
    private final Set<String> selectedElementIds = new HashSet<>();
    private Runnable onLayersChangedListener;

    public Document() {
        Layer defaultLayer = new Layer("Contorno Couro", "#00A8FF", 0);
        Layer stitchLayer = new Layer("Costura", "#FFD700", 1);
        Layer creaseLayer = new Layer("Vinco / Boleador", "#FF8C00", 2);

        layers.add(defaultLayer);
        layers.add(stitchLayer);
        layers.add(creaseLayer);
        activeLayer = defaultLayer;
    }

    public List<Layer> getLayers() { return Collections.unmodifiableList(layers); }
    public List<CADElement> getElements() { return Collections.unmodifiableList(elements); }

    public void clearElements() {
        elements.clear();
        selectedElementIds.clear();
        selectedSubElements.clear();
        notifyDocumentChanged();
    }

    public Layer getActiveLayer() { return activeLayer; }
    public void setActiveLayer(Layer layer) { this.activeLayer = layer; }

    public String getLeatherLayerId() {
        return layers.stream()
            .filter(l -> l.getName().equalsIgnoreCase("Contorno Couro") || l.getName().toLowerCase().contains("couro") || l.getName().toLowerCase().contains("contorno"))
            .map(Layer::getId)
            .findFirst()
            .orElseGet(() -> activeLayer != null ? activeLayer.getId() : (layers.isEmpty() ? "" : layers.get(0).getId()));
    }

    public String getStitchLayerId() {
        return layers.stream()
            .filter(l -> l.getName().equalsIgnoreCase("Costura") || l.getName().toLowerCase().contains("costura"))
            .map(Layer::getId)
            .findFirst()
            .orElseGet(() -> activeLayer != null ? activeLayer.getId() : (layers.isEmpty() ? "" : layers.get(0).getId()));
    }

    public String getCreaseLayerId() {
        return layers.stream()
            .filter(l -> l.getName().equalsIgnoreCase("Vinco / Boleador") || l.getName().toLowerCase().contains("vinco") || l.getName().toLowerCase().contains("boleador"))
            .map(Layer::getId)
            .findFirst()
            .orElseGet(() -> activeLayer != null ? activeLayer.getId() : (layers.isEmpty() ? "" : layers.get(0).getId()));
    }

    private final List<Runnable> onDocumentChangedListeners = new ArrayList<>();

    public void addOnDocumentChangedListener(Runnable listener) {
        this.onDocumentChangedListeners.add(listener);
    }

    public void notifyDocumentChanged() {
        for (Runnable r : onDocumentChangedListeners) {
            try { r.run(); } catch (Exception ignored) {}
        }
    }

    public void setOnLayersChangedListener(Runnable listener) {
        this.onLayersChangedListener = listener;
    }

    public void notifyLayersChanged() {
        if (onLayersChangedListener != null) {
            onLayersChangedListener.run();
        }
        notifyDocumentChanged();
    }

    public void addLayer(Layer layer) {
        layers.add(layer);
        notifyLayersChanged();
    }

    private final Map<String, String> elementParentMap = new HashMap<>();
    private final Map<String, List<String>> elementChildrenMap = new HashMap<>();

    public String getParentId(String elementId) {
        return elementParentMap.get(elementId);
    }

    public List<String> getChildrenIds(String parentId) {
        return elementChildrenMap.getOrDefault(parentId, Collections.emptyList());
    }

    public void linkChildToParent(String childId, String parentId) {
        elementParentMap.put(childId, parentId);
        elementChildrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(childId);
    }

    public void rebuildSpatialHierarchy() {
        elementParentMap.clear();
        elementChildrenMap.clear();

        for (CADElement child : elements) {
            if (child instanceof DimensionElement) continue;

            Point2D center = child.boundingBox().center();
            double areaChild = child.boundingBox().width() * child.boundingBox().height();

            CADElement bestParent = null;
            double smallestParentArea = Double.MAX_VALUE;

            for (CADElement parent : elements) {
                if (parent == child || parent instanceof DimensionElement) continue;

                double areaParent = parent.boundingBox().width() * parent.boundingBox().height();
                if (areaParent <= areaChild) continue;

                boolean isInside = false;
                if (parent instanceof RectElement rectElem) {
                    isInside = rectElem.rect().contains(center);
                } else if (parent instanceof CircleElement circleElem) {
                    isInside = circleElem.circle().center().distanceTo(center) <= circleElem.circle().radius();
                } else if (parent instanceof PolylineElement polyElem) {
                    isInside = polyElem.polyline().isClosed() && polyElem.polyline().contains(center);
                } else if (parent.containsPoint(center, 0.0)) {
                    isInside = true;
                }

                if (isInside && areaParent < smallestParentArea) {
                    smallestParentArea = areaParent;
                    bestParent = parent;
                }
            }

            if (bestParent != null) {
                linkChildToParent(child.id(), bestParent.id());
            }
        }
    }

    public void addElement(CADElement element) {
        elements.add(element);
        rebuildSpatialHierarchy();
        notifyDocumentChanged();
    }

    public void removeElement(CADElement element) {
        elements.remove(element);
        selectedElementIds.remove(element.id());
        rebuildSpatialHierarchy();
        notifyDocumentChanged();
    }

    public Set<String> getSelectedElementIds() {
        return Collections.unmodifiableSet(selectedElementIds);
    }

    public void selectElement(String id, boolean multiSelect) {
        CADElement elem = findElementById(id);
        if (elem != null) {
            Layer layer = findLayerById(elem.layerId());
            if (layer != null && (!layer.isVisible() || layer.isLocked())) {
                return;
            }
        }
        if (!multiSelect) {
            selectedElementIds.clear();
        }
        selectedElementIds.add(id);
    }

    private final Set<SubElementRef> selectedSubElements = new HashSet<>();

    public Set<SubElementRef> getSelectedSubElements() {
        return Collections.unmodifiableSet(selectedSubElements);
    }

    public void selectSubElement(SubElementRef ref, boolean multiSelect) {
        if (!multiSelect) {
            selectedSubElements.clear();
        }
        selectedSubElements.add(ref);
        selectElement(ref.elementId(), multiSelect);
    }

    public void toggleSubElementSelection(SubElementRef ref) {
        if (selectedSubElements.contains(ref)) {
            selectedSubElements.remove(ref);
        } else {
            selectSubElement(ref, true);
        }
    }

    public void toggleElementSelection(String id) {
        if (selectedElementIds.contains(id)) {
            selectedElementIds.remove(id);
        } else {
            selectElement(id, true);
        }
    }

    public void clearSelection() {
        selectedElementIds.clear();
        selectedSubElements.clear();
    }

    public Optional<CADElement> findElementAt(Point2D p, double toleranceMm) {
        List<CADElement> matches = new ArrayList<>();
        for (CADElement elem : elements) {
            Layer layer = findLayerById(elem.layerId());
            if (layer != null && layer.isVisible() && !layer.isLocked()) {
                if (elem.containsPoint(p, toleranceMm)) {
                    matches.add(elem);
                }
            }
        }

        if (matches.isEmpty()) return Optional.empty();
        if (matches.size() == 1) return Optional.of(matches.get(0));

        // Priorizar elementos 1D e elementos de menor área (ex: porta-cartões dentro do corpo da carteira)
        matches.sort((a, b) -> {
            boolean is1D_A = (a instanceof LineElement || a instanceof com.leathercad.core.leather.StitchElement || a instanceof com.leathercad.core.leather.CreaseElement || a instanceof DimensionElement);
            boolean is1D_B = (b instanceof LineElement || b instanceof com.leathercad.core.leather.StitchElement || b instanceof com.leathercad.core.leather.CreaseElement || b instanceof DimensionElement);

            if (is1D_A && !is1D_B) return -1;
            if (!is1D_A && is1D_B) return 1;

            double areaA = a.boundingBox().width() * a.boundingBox().height();
            double areaB = b.boundingBox().width() * b.boundingBox().height();
            return Double.compare(areaA, areaB);
        });

        return Optional.of(matches.get(0));
    }

    public Set<String> getExpandedSelectionWithChildElements() {
        Set<String> expanded = new HashSet<>(selectedElementIds);
        for (String id : selectedElementIds) {
            collectAllChildren(id, expanded);
        }
        return expanded;
    }

    private void collectAllChildren(String parentId, Set<String> result) {
        List<String> children = elementChildrenMap.get(parentId);
        if (children != null) {
            for (String childId : children) {
                result.add(childId);
                collectAllChildren(childId, result);
            }
        }
    }

    public void moveSelected(double dx, double dy) {
        Set<String> targetIds = getExpandedSelectionWithChildElements();
        List<CADElement> toReplace = new ArrayList<>();
        for (CADElement elem : elements) {
            if (targetIds.contains(elem.id())) {
                Layer layer = findLayerById(elem.layerId());
                if (layer == null || (!layer.isLocked() && layer.isVisible())) {
                    toReplace.add(elem);
                }
            }
        }
        for (CADElement oldElem : toReplace) {
            int index = elements.indexOf(oldElem);
            CADElement newElem = oldElem.translate(dx, dy);
            elements.set(index, newElem);
            if (selectedElementIds.contains(oldElem.id())) {
                selectedElementIds.remove(oldElem.id());
                selectedElementIds.add(newElem.id());
            }
        }
    }

    public void copySelected(double dx, double dy) {
        Set<String> targetIds = getExpandedSelectionWithChildElements();
        Set<String> newSelection = new HashSet<>();
        List<CADElement> copies = new ArrayList<>();
        for (CADElement elem : elements) {
            if (targetIds.contains(elem.id())) {
                Layer layer = findLayerById(elem.layerId());
                if (layer == null || (!layer.isLocked() && layer.isVisible())) {
                    CADElement copy = elem.copyWithNewId().translate(dx, dy);
                    copies.add(copy);
                    newSelection.add(copy.id());
                }
            }
        }
        elements.addAll(copies);
        selectedElementIds.clear();
        selectedElementIds.addAll(newSelection);
    }

    public void rotateSelected(double angleDegrees) {
        if (selectedElementIds.isEmpty()) return;
        Set<String> targetIds = getExpandedSelectionWithChildElements();
        Point2D center = getSelectionCenter();
        List<CADElement> toReplace = new ArrayList<>();
        for (CADElement elem : elements) {
            if (targetIds.contains(elem.id())) {
                Layer layer = findLayerById(elem.layerId());
                if (layer == null || (!layer.isLocked() && layer.isVisible())) {
                    toReplace.add(elem);
                }
            }
        }
        for (CADElement oldElem : toReplace) {
            int index = elements.indexOf(oldElem);
            CADElement newElem = oldElem.rotate(center, angleDegrees);
            elements.set(index, newElem);
            if (selectedElementIds.contains(oldElem.id())) {
                selectedElementIds.remove(oldElem.id());
                selectedElementIds.add(newElem.id());
            }
        }
    }

    public void mirrorSelectedHorizontal() {
        if (selectedElementIds.isEmpty()) return;
        Point2D center = getSelectionCenter();
        mirrorSelected(new Point2D(center.x(), center.y() - 100), new Point2D(center.x(), center.y() + 100));
    }

    public void mirrorSelected(Point2D axisStart, Point2D axisEnd) {
        if (selectedElementIds.isEmpty()) return;
        Set<String> targetIds = getExpandedSelectionWithChildElements();
        List<CADElement> copies = new ArrayList<>();
        for (CADElement elem : elements) {
            if (targetIds.contains(elem.id())) {
                Layer layer = findLayerById(elem.layerId());
                if (layer == null || (!layer.isLocked() && layer.isVisible())) {
                    copies.add(elem.copyWithNewId().mirror(axisStart, axisEnd));
                }
            }
        }
        elements.addAll(copies);
    }

    public List<String> explodeSelected() {
        if (selectedElementIds.isEmpty()) return Collections.emptyList();

        List<CADElement> toRemove = new ArrayList<>();
        List<CADElement> toAdd = new ArrayList<>();
        List<String> newSelectedIds = new ArrayList<>();

        for (CADElement elem : elements) {
            if (selectedElementIds.contains(elem.id())) {
                Layer layer = findLayerById(elem.layerId());
                if (layer != null && (layer.isLocked() || !layer.isVisible())) continue;

                if (elem instanceof RectElement rectElem) {
                    toRemove.add(rectElem);
                    List<LineElement> lines = rectElem.explodeToLines();
                    toAdd.addAll(lines);
                    for (LineElement l : lines) newSelectedIds.add(l.id());
                } else if (elem instanceof PolylineElement polyElem) {
                    toRemove.add(polyElem);
                    List<LineElement> lines = polyElem.explodeToLines();
                    toAdd.addAll(lines);
                    for (LineElement l : lines) newSelectedIds.add(l.id());
                }
            }
        }

        elements.removeAll(toRemove);
        elements.addAll(toAdd);
        selectedElementIds.clear();
        selectedElementIds.addAll(newSelectedIds);
        return newSelectedIds;
    }

    public void updateBezierControlPoint(String id, int handleIndex, Point2D newPoint) {
        CADElement elem = findElementById(id);
        if (elem instanceof BezierElement bezierElem) {
            Layer layer = findLayerById(bezierElem.layerId());
            if (layer != null && (layer.isLocked() || !layer.isVisible())) return;

            var oldB = bezierElem.bezier();
            Point2D s = (handleIndex == 1) ? newPoint : oldB.start();
            Point2D c1 = (handleIndex == 2) ? newPoint : oldB.control1();
            Point2D c2 = (handleIndex == 3) ? newPoint : oldB.control2();
            Point2D e = (handleIndex == 4) ? newPoint : oldB.end();

            int index = elements.indexOf(bezierElem);
            BezierElement updated = new BezierElement(bezierElem.id(), bezierElem.layerId(), new com.leathercad.core.geometry.Bezier2D(s, c1, c2, e));
            elements.set(index, updated);
        }
    }

    public void updatePolylineVertex(String id, int vertexIndex, Point2D newPoint) {
        CADElement elem = findElementById(id);
        if (elem instanceof PolylineElement polyElem) {
            Layer layer = findLayerById(polyElem.layerId());
            if (layer != null && (layer.isLocked() || !layer.isVisible())) return;

            var pts = new ArrayList<>(polyElem.polyline().points());
            if (vertexIndex >= 0 && vertexIndex < pts.size()) {
                pts.set(vertexIndex, newPoint);
                int index = elements.indexOf(polyElem);
                PolylineElement updated = new PolylineElement(polyElem.id(), polyElem.layerId(), new com.leathercad.core.geometry.Polyline2D(pts, polyElem.polyline().isClosed()));
                elements.set(index, updated);
            }
        }
    }

    public void scaleSelected(double factor) {
        if (selectedElementIds.isEmpty()) return;
        Set<String> targetIds = getExpandedSelectionWithChildElements();
        Point2D center = getSelectionCenter();
        List<CADElement> toReplace = new ArrayList<>();
        for (CADElement elem : elements) {
            if (targetIds.contains(elem.id())) {
                Layer layer = findLayerById(elem.layerId());
                if (layer == null || (!layer.isLocked() && layer.isVisible())) {
                    toReplace.add(elem);
                }
            }
        }
        for (CADElement oldElem : toReplace) {
            int index = elements.indexOf(oldElem);
            CADElement newElem = oldElem.scale(center, factor);
            elements.set(index, newElem);
        }
    }

    public void deleteSelected() {
        List<CADElement> toRemove = new ArrayList<>();
        Set<Point2D> deletedPoints = new HashSet<>();

        for (CADElement elem : elements) {
            if (selectedElementIds.contains(elem.id())) {
                Layer layer = findLayerById(elem.layerId());
                if (layer == null || (!layer.isLocked() && layer.isVisible())) {
                    toRemove.add(elem);
                    if (elem instanceof RectElement r) {
                        Point2D min = r.rect().minPoint();
                        deletedPoints.add(min);
                        deletedPoints.add(new Point2D(min.x() + r.rect().width(), min.y()));
                        deletedPoints.add(new Point2D(min.x() + r.rect().width(), min.y() + r.rect().height()));
                        deletedPoints.add(new Point2D(min.x(), min.y() + r.rect().height()));
                    } else if (elem instanceof LineElement l) {
                        deletedPoints.add(l.line().start());
                        deletedPoints.add(l.line().end());
                    }
                }
            }
        }

        // Remover também cotas (DimensionElement) cujos pontos pertençam aos elementos deletados ou que foram selecionadas
        for (CADElement elem : elements) {
            if (elem instanceof DimensionElement dim) {
                if (selectedElementIds.contains(dim.id()) ||
                    deletedPoints.contains(dim.start()) || deletedPoints.contains(dim.end())) {
                    toRemove.add(dim);
                }
            }
        }

        elements.removeAll(toRemove);
        selectedElementIds.clear();
        selectedSubElements.clear();
        rebuildSpatialHierarchy();
        notifyDocumentChanged();
    }

    public void setCornerRadiusSelected(double radiusMm) {
        if (!selectedSubElements.isEmpty()) {
            Map<String, Set<SubElementRef>> subMap = new HashMap<>();
            for (SubElementRef s : selectedSubElements) {
                subMap.computeIfAbsent(s.elementId(), k -> new HashSet<>()).add(s);
            }

            for (Map.Entry<String, Set<SubElementRef>> entry : subMap.entrySet()) {
                String elemId = entry.getKey();
                Set<SubElementRef> subs = entry.getValue();
                CADElement elem = findElementById(elemId);
                if (elem instanceof RectElement rectElem) {
                    var r = rectElem.rect();
                    Set<Integer> edgeIndices = new HashSet<>();
                    Set<Integer> vertexIndices = new HashSet<>();
                    for (SubElementRef s : subs) {
                        if (s.type() == SubElementRef.SubElementType.EDGE) edgeIndices.add(s.index());
                        if (s.type() == SubElementRef.SubElementType.VERTEX) vertexIndices.add(s.index());
                    }

                    boolean selTL = vertexIndices.contains(0) || (edgeIndices.contains(0) && edgeIndices.contains(3));
                    boolean selTR = vertexIndices.contains(1) || (edgeIndices.contains(0) && edgeIndices.contains(1));
                    boolean selBR = vertexIndices.contains(2) || (edgeIndices.contains(1) && edgeIndices.contains(2));
                    boolean selBL = vertexIndices.contains(3) || (edgeIndices.contains(2) && edgeIndices.contains(3));

                    double rTL = selTL ? radiusMm : 0.0;
                    double rTR = selTR ? radiusMm : 0.0;
                    double rBR = selBR ? radiusMm : 0.0;
                    double rBL = selBL ? radiusMm : 0.0;

                    int index = elements.indexOf(rectElem);
                    Rect2D newRect = new Rect2D(r.minPoint(), r.width(), r.height(), rTL, rTR, rBR, rBL);
                    elements.set(index, new RectElement(rectElem.id(), rectElem.layerId(), newRect));
                } else if (elem instanceof PolylineElement polyElem) {
                    Set<Integer> edgeIndices = new HashSet<>();
                    Set<Integer> vertexIndices = new HashSet<>();
                    for (SubElementRef s : subs) {
                        if (s.type() == SubElementRef.SubElementType.EDGE) edgeIndices.add(s.index());
                        if (s.type() == SubElementRef.SubElementType.VERTEX) vertexIndices.add(s.index());
                    }
                    var pts = polyElem.polyline().points();
                    int numPts = pts.size();
                    boolean isClosed = polyElem.polyline().isClosed();
                    for (int i = 0; i < numPts; i++) {
                        int prevEdge = (i > 0) ? (i - 1) : (isClosed ? numPts - 1 : -1);
                        int nextEdge = i;
                        boolean vertexSelected = vertexIndices.contains(i);
                        boolean bothEdgesSelected = (prevEdge >= 0 && edgeIndices.contains(prevEdge) && edgeIndices.contains(nextEdge));
                        if (vertexSelected || bothEdgesSelected) {
                            applyFilletAtVertex(elem, i, radiusMm);
                        }
                    }
                }
            }
            selectedSubElements.clear();
            notifyDocumentChanged();
            return;
        }

        setCornerRadiusSelected(radiusMm, radiusMm, radiusMm, radiusMm);
    }

    public void setCornerRadiusSelected(double rTL, double rTR, double rBR, double rBL) {
        if (!selectedSubElements.isEmpty()) {
            Map<String, Set<SubElementRef>> subMap = new HashMap<>();
            for (SubElementRef s : selectedSubElements) {
                subMap.computeIfAbsent(s.elementId(), k -> new HashSet<>()).add(s);
            }

            for (Map.Entry<String, Set<SubElementRef>> entry : subMap.entrySet()) {
                String elemId = entry.getKey();
                Set<SubElementRef> subs = entry.getValue();
                CADElement elem = findElementById(elemId);
                if (elem instanceof RectElement rectElem) {
                    var r = rectElem.rect();
                    Set<Integer> edgeIndices = new HashSet<>();
                    Set<Integer> vertexIndices = new HashSet<>();
                    for (SubElementRef s : subs) {
                        if (s.type() == SubElementRef.SubElementType.EDGE) edgeIndices.add(s.index());
                        if (s.type() == SubElementRef.SubElementType.VERTEX) vertexIndices.add(s.index());
                    }

                    boolean selTL = vertexIndices.contains(0) || (edgeIndices.contains(0) && edgeIndices.contains(3));
                    boolean selTR = vertexIndices.contains(1) || (edgeIndices.contains(0) && edgeIndices.contains(1));
                    boolean selBR = vertexIndices.contains(2) || (edgeIndices.contains(1) && edgeIndices.contains(2));
                    boolean selBL = vertexIndices.contains(3) || (edgeIndices.contains(2) && edgeIndices.contains(3));

                    double finalTL = selTL ? rTL : 0.0;
                    double finalTR = selTR ? rTR : 0.0;
                    double finalBR = selBR ? rBR : 0.0;
                    double finalBL = selBL ? rBL : 0.0;

                    int index = elements.indexOf(rectElem);
                    Rect2D newRect = new Rect2D(r.minPoint(), r.width(), r.height(), finalTL, finalTR, finalBR, finalBL);
                    elements.set(index, new RectElement(rectElem.id(), rectElem.layerId(), newRect));
                }
            }
            selectedSubElements.clear();
            notifyDocumentChanged();
            return;
        }

        List<LineElement> selectedLines = new ArrayList<>();
        for (String id : selectedElementIds) {
            CADElement elem = findElementById(id);
            if (elem instanceof RectElement rectElem) {
                Layer layer = findLayerById(rectElem.layerId());
                if (layer != null && (layer.isLocked() || !layer.isVisible())) continue;
                int index = elements.indexOf(rectElem);
                Rect2D newRect = new Rect2D(rectElem.rect().minPoint(), rectElem.rect().width(), rectElem.rect().height(), rTL, rTR, rBR, rBL);
                RectElement newElem = new RectElement(rectElem.id(), rectElem.layerId(), newRect);
                elements.set(index, newElem);
            } else if (elem instanceof LineElement lineElem) {
                Layer layer = findLayerById(lineElem.layerId());
                if (layer == null || (!layer.isLocked() && layer.isVisible())) {
                    selectedLines.add(lineElem);
                }
            }
        }

        if (selectedLines.size() == 2) {
            applyFilletBetweenLines(selectedLines.get(0), selectedLines.get(1), rTL);
        }
        notifyDocumentChanged();
    }

    public boolean applyFilletAtVertex(CADElement elem, int vertexIndex, double radiusMm) {
        if (elem == null || radiusMm <= 0) return false;
        Layer layer = findLayerById(elem.layerId());
        if (layer != null && (layer.isLocked() || !layer.isVisible())) return false;

        if (elem instanceof RectElement rectElem) {
            Rect2D r = rectElem.rect();
            double rTL = (vertexIndex == 0) ? radiusMm : r.rTopLeft();
            double rTR = (vertexIndex == 1) ? radiusMm : r.rTopRight();
            double rBR = (vertexIndex == 2) ? radiusMm : r.rBottomRight();
            double rBL = (vertexIndex == 3) ? radiusMm : r.rBottomLeft();

            Rect2D newRect = new Rect2D(r.minPoint(), r.width(), r.height(), rTL, rTR, rBR, rBL);
            RectElement newElem = new RectElement(rectElem.id(), rectElem.layerId(), newRect);
            int idx = elements.indexOf(rectElem);
            if (idx >= 0) {
                elements.set(idx, newElem);
                return true;
            }
        } else if (elem instanceof PolylineElement polyElem) {
            var pts = polyElem.polyline().points();
            int numPts = pts.size();
            if (numPts < 2) return false;

            boolean isClosed = polyElem.polyline().isClosed();
            if (!isClosed && (vertexIndex == 0 || vertexIndex == numPts - 1)) {
                return false;
            }

            Point2D v = pts.get(vertexIndex);
            Point2D pA = (vertexIndex > 0) ? pts.get(vertexIndex - 1) : pts.get(numPts - 1);
            Point2D pB = (vertexIndex < numPts - 1) ? pts.get(vertexIndex + 1) : pts.get(0);

            var filletOpt = com.leathercad.core.geometry.Arc2D.calculateFillet(pA, v, pB, radiusMm);
            if (filletOpt.isEmpty()) return false;

            var fRes = filletOpt.get();
            ArcElement newArc = new ArcElement(polyElem.layerId(), fRes.arc());
            elements.add(newArc);

            List<Point2D> newPts = new ArrayList<>();
            for (int i = 0; i < numPts; i++) {
                if (i == vertexIndex) {
                    newPts.add(fRes.tA());
                    newPts.add(fRes.tB());
                } else {
                    newPts.add(pts.get(i));
                }
            }
            Polyline2D newPolyline = new Polyline2D(newPts, isClosed);
            PolylineElement newPolyElem = new PolylineElement(polyElem.id(), polyElem.layerId(), newPolyline);
            int idx = elements.indexOf(polyElem);
            if (idx >= 0) {
                elements.set(idx, newPolyElem);
                return true;
            }
        } else if (elem instanceof LineElement lineElem) {
            Point2D start = lineElem.line().start();
            Point2D end = lineElem.line().end();
            Point2D targetV = (vertexIndex == 0) ? start : end;

            LineElement adjacent = null;
            for (CADElement other : elements) {
                if (other instanceof LineElement otherLine && !other.id().equals(lineElem.id())) {
                    if (otherLine.line().start().distanceTo(targetV) < 1e-2 || otherLine.line().end().distanceTo(targetV) < 1e-2) {
                        adjacent = otherLine;
                        break;
                    }
                }
            }
            if (adjacent != null) {
                return applyFilletBetweenLines(lineElem, adjacent, radiusMm);
            }
        }
        return false;
    }

    public boolean applyFilletBetweenLines(LineElement lineElemA, LineElement lineElemB, double radiusMm) {
        var segA = lineElemA.line();
        var segB = lineElemB.line();

        Point2D a1 = segA.start();
        Point2D a2 = segA.end();
        Point2D b1 = segB.start();
        Point2D b2 = segB.end();

        Point2D v = null;
        Point2D pA = null;
        Point2D pB = null;

        double d11 = a1.distanceTo(b1), d12 = a1.distanceTo(b2);
        double d21 = a2.distanceTo(b1), d22 = a2.distanceTo(b2);
        double minD = Math.min(Math.min(d11, d12), Math.min(d21, d22));

        if (minD < 1e-2) {
            if (minD == d11) { v = a1; pA = a2; pB = b2; }
            else if (minD == d12) { v = a1; pA = a2; pB = b1; }
            else if (minD == d21) { v = a2; pA = a1; pB = b2; }
            else { v = a2; pA = a1; pB = b1; }
        } else {
            double denom = (a1.x() - a2.x()) * (b1.y() - b2.y()) - (a1.y() - a2.y()) * (b1.x() - b2.x());
            if (Math.abs(denom) < 1e-6) return false;

            double tNumerator = (a1.x() - b1.x()) * (b1.y() - b2.y()) - (a1.y() - b1.y()) * (b1.x() - b2.x());
            double t = tNumerator / denom;
            v = new Point2D(a1.x() + t * (a2.x() - a1.x()), a1.y() + t * (a2.y() - a1.y()));

            pA = (a1.distanceTo(v) > a2.distanceTo(v)) ? a1 : a2;
            pB = (b1.distanceTo(v) > b2.distanceTo(v)) ? b1 : b2;
        }

        var filletOpt = com.leathercad.core.geometry.Arc2D.calculateFillet(pA, v, pB, radiusMm);
        if (filletOpt.isEmpty()) return false;

        var fRes = filletOpt.get();

        LineElement newA = new LineElement(lineElemA.id(), lineElemA.layerId(), new com.leathercad.core.geometry.LineSegment(pA, fRes.tA()));
        LineElement newB = new LineElement(lineElemB.id(), lineElemB.layerId(), new com.leathercad.core.geometry.LineSegment(fRes.tB(), pB));
        ArcElement newArc = new ArcElement(lineElemA.layerId(), fRes.arc());

        int idxA = elements.indexOf(lineElemA);
        if (idxA >= 0) elements.set(idxA, newA);

        int idxB = elements.indexOf(lineElemB);
        if (idxB >= 0) elements.set(idxB, newB);

        elements.add(newArc);
        return true;
    }


    private Point2D getSelectionCenter() {
        double sumX = 0, sumY = 0;
        int count = 0;
        for (CADElement elem : elements) {
            if (selectedElementIds.contains(elem.id())) {
                Point2D c = elem.boundingBox().center();
                sumX += c.x();
                sumY += c.y();
                count++;
            }
        }
        return count == 0 ? Point2D.ZERO : new Point2D(sumX / count, sumY / count);
    }

    public Layer findLayerById(String layerId) {
        if (layerId == null) return null;
        return layers.stream().filter(l -> l.getId().equals(layerId)).findFirst().orElse(null);
    }

    public CADElement findElementById(String id) {
        if (id == null) return null;
        return elements.stream().filter(e -> e.id().equals(id)).findFirst().orElse(null);
    }

    public Rect2D getBoundingBox() {
        if (elements.isEmpty()) return new Rect2D(0, 0, 100, 100);
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (CADElement elem : elements) {
            Rect2D b = elem.boundingBox();
            minX = Math.min(minX, b.minPoint().x());
            minY = Math.min(minY, b.minPoint().y());
            maxX = Math.max(maxX, b.minPoint().x() + b.width());
            maxY = Math.max(maxY, b.minPoint().y() + b.height());
        }
        return new Rect2D(new Point2D(minX, minY), Math.max(1.0, maxX - minX), Math.max(1.0, maxY - minY));
    }

    public void updateLineEndpoint(String id, int handleIndex, Point2D newPoint) {
        CADElement elem = findElementById(id);
        if (elem instanceof LineElement lineElem) {
            Layer layer = findLayerById(lineElem.layerId());
            if (layer != null && (layer.isLocked() || !layer.isVisible())) return;

            var line = lineElem.line();
            Point2D s = (handleIndex == 1) ? newPoint : line.start();
            Point2D e = (handleIndex == 2) ? newPoint : line.end();

            int index = elements.indexOf(lineElem);
            LineElement updated = new LineElement(lineElem.id(), lineElem.layerId(), new com.leathercad.core.geometry.LineSegment(s, e));
            elements.set(index, updated);
        }
    }

    public void updateRectCorner(String id, int cornerIndex, Point2D newPoint) {
        CADElement elem = findElementById(id);
        if (elem instanceof RectElement rectElem) {
            Layer layer = findLayerById(rectElem.layerId());
            if (layer != null && (layer.isLocked() || !layer.isVisible())) return;

            var r = rectElem.rect();
            double x1 = r.minPoint().x();
            double y1 = r.minPoint().y();
            double x2 = x1 + r.width();
            double y2 = y1 + r.height();

            switch (cornerIndex) {
                case 0 -> { x1 = newPoint.x(); y1 = newPoint.y(); }
                case 1 -> { x2 = newPoint.x(); y1 = newPoint.y(); }
                case 2 -> { x2 = newPoint.x(); y2 = newPoint.y(); }
                case 3 -> { x1 = newPoint.x(); y2 = newPoint.y(); }
            }

            double minX = Math.min(x1, x2);
            double minY = Math.min(y1, y2);
            double w = Math.max(0.1, Math.abs(x2 - x1));
            double h = Math.max(0.1, Math.abs(y2 - y1));

            Rect2D newRect = new Rect2D(new Point2D(minX, minY), w, h, r.cornerRadius());
            int index = elements.indexOf(rectElem);
            elements.set(index, new RectElement(rectElem.id(), rectElem.layerId(), newRect));
        }
    }

    public void updateCircleGrip(String id, int handleIndex, Point2D newPoint) {
        CADElement elem = findElementById(id);
        if (elem instanceof CircleElement circleElem) {
            Layer layer = findLayerById(circleElem.layerId());
            if (layer != null && (layer.isLocked() || !layer.isVisible())) return;

            var c = circleElem.circle();
            com.leathercad.core.geometry.Circle2D newCircle;
            if (handleIndex == 0) {
                newCircle = new com.leathercad.core.geometry.Circle2D(newPoint, c.radius());
            } else {
                double newR = Math.max(0.1, c.center().distanceTo(newPoint));
                newCircle = new com.leathercad.core.geometry.Circle2D(c.center(), newR);
            }

            int index = elements.indexOf(circleElem);
            elements.set(index, new CircleElement(circleElem.id(), circleElem.layerId(), newCircle));
        }
    }

    public void updateArcGrip(String id, int handleIndex, Point2D newPoint) {
        CADElement elem = findElementById(id);
        if (elem instanceof ArcElement arcElem) {
            Layer layer = findLayerById(arcElem.layerId());
            if (layer != null && (layer.isLocked() || !layer.isVisible())) return;

            var a = arcElem.arc();
            com.leathercad.core.geometry.Arc2D newArc;
            if (handleIndex == 0) {
                newArc = new com.leathercad.core.geometry.Arc2D(newPoint, a.radius(), a.startAngleDegrees(), a.sweepAngleDegrees());
            } else if (handleIndex == 1) {
                double newR = Math.max(0.1, a.center().distanceTo(newPoint));
                double newStart = Math.toDegrees(Math.atan2(newPoint.y() - a.center().y(), newPoint.x() - a.center().x()));
                if (newStart < 0) newStart += 360;
                newArc = new com.leathercad.core.geometry.Arc2D(a.center(), newR, newStart, a.sweepAngleDegrees());
            } else {
                double newR = Math.max(0.1, a.center().distanceTo(newPoint));
                double endAngle = Math.toDegrees(Math.atan2(newPoint.y() - a.center().y(), newPoint.x() - a.center().x()));
                if (endAngle < 0) endAngle += 360;
                double start = a.startAngleDegrees() % 360;
                if (start < 0) start += 360;
                double sweep = endAngle - start;
                while (sweep <= 0) sweep += 360;
                newArc = new com.leathercad.core.geometry.Arc2D(a.center(), newR, a.startAngleDegrees(), sweep);
            }

            int index = elements.indexOf(arcElem);
            elements.set(index, new ArcElement(arcElem.id(), arcElem.layerId(), newArc));
        }
    }

    public void clear() {
        elements.clear();
        selectedElementIds.clear();
    }
}
