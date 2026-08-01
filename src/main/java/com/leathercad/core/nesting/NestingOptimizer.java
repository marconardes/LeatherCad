package com.leathercad.core.nesting;

import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.model.CADElement;
import com.leathercad.core.model.CADGroup;
import com.leathercad.core.model.Document;
import com.leathercad.core.model.PolylineElement;
import com.leathercad.core.model.RectElement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NestingOptimizer {

    /**
     * Executa o algoritmo de Nesting inteligente (2D Bin Packing) para otimizar o corte das peças de couro.
     */
    public static NestingResult optimizeLayout(Document doc, HideSheet hideSheet, boolean allowRotation) {
        List<NestingPiece> pieces = new ArrayList<>();
        List<CADElement> cutoutElements = new ArrayList<>();

        // 1. Identifica todas as peças de couro recortáveis independentes (Retângulos e Polilinhas fechadas)
        for (CADElement elem : doc.getElements()) {
            if (elem instanceof RectElement rectElem) {
                cutoutElements.add(rectElem);
                var rect = rectElem.rect();
                var layer = doc.findLayerById(elem.layerId());
                String matName = (layer != null && layer.getMaterial() != null) ? layer.getMaterial().name() : "Couro Padrão";

                NestingPiece piece = new NestingPiece(
                    elem.id(),
                    elem.layerId(),
                    matName,
                    rect.width(),
                    rect.height()
                );
                piece.setOrigMinPoint(rect.minPoint());
                pieces.add(piece);
            } else if (elem instanceof PolylineElement polyElem && polyElem.polyline().isClosed()) {
                cutoutElements.add(polyElem);
                var bbox = polyElem.boundingBox();
                var layer = doc.findLayerById(elem.layerId());
                String matName = (layer != null && layer.getMaterial() != null) ? layer.getMaterial().name() : "Couro Padrão";

                NestingPiece piece = new NestingPiece(
                    elem.id(),
                    elem.layerId(),
                    matName,
                    bbox.width(),
                    bbox.height()
                );
                piece.setOrigMinPoint(bbox.minPoint());
                pieces.add(piece);
            } else if (elem instanceof CADGroup groupElem) {
                cutoutElements.add(groupElem);
                var bbox = groupElem.boundingBox();
                var layer = doc.findLayerById(elem.layerId());
                String matName = (layer != null && layer.getMaterial() != null) ? layer.getMaterial().name() : "Couro Padrão";

                NestingPiece piece = new NestingPiece(
                    elem.id(),
                    elem.layerId(),
                    matName + " (" + groupElem.name() + ")",
                    bbox.width(),
                    bbox.height()
                );
                piece.setOrigMinPoint(bbox.minPoint());
                pieces.add(piece);
            }
        }

        // 2. Particionamento Hierárquico de Posse EXCLUSIVA por Distância ao Centro do Bounding Box
        for (CADElement other : doc.getElements()) {
            if (!cutoutElements.contains(other)) {
                CADElement bestParent = null;
                double minDistance = Double.MAX_VALUE;

                Point2D c = other.boundingBox().center();

                for (CADElement cutout : cutoutElements) {
                    var bbox = cutout.boundingBox();
                    Rect2D expandedRect = new Rect2D(
                        bbox.minPoint().x() - 5.0,
                        bbox.minPoint().y() - 5.0,
                        bbox.width() + 10.0,
                        bbox.height() + 10.0
                    );

                    if (expandedRect.contains(c)) {
                        double dist = c.distanceTo(bbox.center());
                        if (dist < minDistance) {
                            minDistance = dist;
                            bestParent = cutout;
                        }
                    }
                }

                if (bestParent != null) {
                    for (NestingPiece p : pieces) {
                        if (p.id().equals(bestParent.id())) {
                            p.childElementIds().add(other.id());
                            break;
                        }
                    }
                }
            }
        }

        // 3. Ordenar peças por Área Decrescente (Maltese/Shelf Packing Heuristic)
        pieces.sort(Comparator.comparingDouble((NestingPiece p) -> p.widthMm() * p.heightMm()).reversed());

        double spacing = hideSheet.spacingMm();
        double currentX = spacing;
        double currentY = spacing;
        double maxHeightInRow = 0.0;
        double totalUsedAreaCm2 = 0.0;

        for (NestingPiece piece : pieces) {
            double pw = piece.widthMm();
            double ph = piece.heightMm();
            double rotAngle = 0.0;

            // Testa se rotacionar 90° economiza espaço na linha atual ou na largura da chapa
            if (allowRotation) {
                boolean fitsNormal = (currentX + pw + spacing <= hideSheet.widthMm());
                boolean fitsRotated = (currentX + ph + spacing <= hideSheet.widthMm());

                if (!fitsNormal && fitsRotated) {
                    pw = piece.heightMm();
                    ph = piece.widthMm();
                    rotAngle = 90.0;
                } else if (fitsNormal && fitsRotated && (ph < pw)) {
                    // Escolhe a menor altura na linha para maximizar eficiência vertical
                    pw = piece.heightMm();
                    ph = piece.widthMm();
                    rotAngle = 90.0;
                }
            }

            // Se estourar a largura da chapa, passa para a próxima linha (shelf)
            if (currentX + pw + spacing > hideSheet.widthMm()) {
                currentX = spacing;
                currentY += maxHeightInRow + spacing;
                maxHeightInRow = 0.0;

                // Re-testa orientação na nova linha
                if (allowRotation && (ph > pw) && (currentX + ph + spacing <= hideSheet.widthMm())) {
                    pw = piece.heightMm();
                    ph = piece.widthMm();
                    rotAngle = 90.0;
                }
            }

            // Encaixa a peça se couber na altura total da chapa
            if (currentY + ph + spacing <= hideSheet.heightMm()) {
                piece.place(currentX, currentY, rotAngle);
                currentX += pw + spacing;
                maxHeightInRow = Math.max(maxHeightInRow, ph);
                totalUsedAreaCm2 += piece.areaCm2();
            }
        }

        double hideAreaCm2 = hideSheet.areaCm2();
        double wasteAreaCm2 = Math.max(0.0, hideAreaCm2 - totalUsedAreaCm2);
        double efficiencyPercent = hideAreaCm2 > 0 ? (totalUsedAreaCm2 / hideAreaCm2) * 100.0 : 0.0;

        return new NestingResult(
            pieces,
            hideSheet,
            hideAreaCm2,
            totalUsedAreaCm2,
            wasteAreaCm2,
            efficiencyPercent,
            1
        );
    }

    /**
     * Aplica o resultado do Nesting ao documento criando um novo conjunto de elementos limpos,
     * evitando translações duplas ou corrupção de IDs.
     */
    public static List<CADElement> applyNestingToDocument(Document doc, NestingResult result) {
        List<CADElement> newElements = new ArrayList<>();
        Set<String> processedChildIds = new HashSet<>();

        for (NestingPiece piece : result.placedPieces()) {
            if (!piece.isPlaced()) continue;

            CADElement elem = doc.findElementById(piece.id());
            if (elem != null) {
                double dx, dy;
                Point2D center;

                if (Math.abs(piece.rotationAngle() - 90.0) < 0.1) {
                    double centerX = piece.placedX() + piece.heightMm() / 2.0;
                    double centerY = piece.placedY() + piece.widthMm() / 2.0;
                    center = new Point2D(centerX, centerY);

                    double unrotatedMinX = centerX - piece.widthMm() / 2.0;
                    double unrotatedMinY = centerY - piece.heightMm() / 2.0;

                    dx = unrotatedMinX - piece.origMinPoint().x();
                    dy = unrotatedMinY - piece.origMinPoint().y();
                } else {
                    dx = piece.placedX() - piece.origMinPoint().x();
                    dy = piece.placedY() - piece.origMinPoint().y();
                    center = new Point2D(piece.placedX() + piece.widthMm() / 2.0, piece.placedY() + piece.heightMm() / 2.0);
                }

                CADElement movedElem = elem.translate(dx, dy);
                if (piece.rotationAngle() != 0.0) {
                    movedElem = movedElem.rotate(center, piece.rotationAngle());
                }
                newElements.add(movedElem);

                for (String childId : piece.childElementIds()) {
                    if (!processedChildIds.contains(childId)) {
                        CADElement child = doc.findElementById(childId);
                        if (child != null) {
                            processedChildIds.add(childId);
                            CADElement movedChild = child.translate(dx, dy);
                            if (piece.rotationAngle() != 0.0) {
                                movedChild = movedChild.rotate(center, piece.rotationAngle());
                            }
                            newElements.add(movedChild);
                        }
                    }
                }
            }
        }

        // Adiciona elementos não associados a nenhuma peça na sua posição original
        for (CADElement elem : doc.getElements()) {
            boolean isPiece = result.placedPieces().stream().anyMatch(p -> p.id().equals(elem.id()));
            if (!isPiece && !processedChildIds.contains(elem.id())) {
                newElements.add(elem);
            }
        }

        return newElements;
    }
}
