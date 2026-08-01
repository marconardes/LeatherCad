package com.leathercad.core.model;

import java.util.ArrayList;
import java.util.List;

public class CADAssembler {

    /**
     * Une uma lista de elementos avulsos (contorno, recortes, costuras) em um bloco de montagem atômico (CADGroup).
     */
    public static CADGroup createAssemblyBlock(String layerId, String blockName, List<CADElement> elements) {
        return new CADGroup(layerId, blockName, elements);
    }

    /**
     * Desfaz os blocos de montagem expandindo recursivamente os elementos filhos para exportação plana ou simulação.
     */
    public static List<CADElement> flatten(List<CADElement> elements) {
        List<CADElement> flatList = new ArrayList<>();
        for (CADElement elem : elements) {
            if (elem instanceof CADGroup group) {
                flatList.addAll(flatten(group.children()));
            } else {
                flatList.add(elem);
            }
        }
        return flatList;
    }
}
