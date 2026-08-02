package com.leathercad.core.parametric;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ParametricRegistry {
    private static final ParametricRegistry INSTANCE = new ParametricRegistry();
    private final Map<String, ParametricTemplate> templates = new LinkedHashMap<>();

    private ParametricRegistry() {
        registerTemplate(new BifoldWalletTemplate());
        registerTemplate(new CardHolderTemplate());
    }

    public static ParametricRegistry getInstance() {
        return INSTANCE;
    }

    public void registerTemplate(ParametricTemplate template) {
        templates.put(template.getId(), template);
    }

    public ParametricTemplate getTemplate(String id) {
        return templates.get(id);
    }

    public List<ParametricTemplate> getAllTemplates() {
        return new ArrayList<>(templates.values());
    }
}
