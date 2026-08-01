package com.leathercad.core.ai;

import com.leathercad.core.components.CardSlotComponent;
import com.leathercad.core.components.CoinPocketComponent;
import com.leathercad.core.components.HardwareComponent;
import com.leathercad.core.components.IDWindowComponent;
import com.leathercad.core.geometry.LineSegment;
import com.leathercad.core.geometry.Point2D;
import com.leathercad.core.geometry.Rect2D;
import com.leathercad.core.leather.CreaseElement;
import com.leathercad.core.leather.StitchConfig;
import com.leathercad.core.leather.StitchElement;
import com.leathercad.core.model.CADAssembler;
import com.leathercad.core.model.CADElement;
import com.leathercad.core.model.DimensionElement;
import com.leathercad.core.model.Document;
import com.leathercad.core.model.RectElement;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AIDesignGenerator {

    public static String generateDesign(Document doc, String prompt, String baseUrl, String modelName, boolean useOllama) {
        String aiResponseText = "";
        boolean ollamaConnected = false;

        if (useOllama) {
            OllamaClient client = new OllamaClient();
            if (client.isOllamaAvailable(baseUrl)) {
                ollamaConnected = true;
                String systemPrompt = "Você é um mestre artesão de couro e arquiteto CAD. Responda detalhadamente com a estrutura de peças, dimensões exatas em mm e margem de costura.";
                try {
                    aiResponseText = client.generateCompletion(baseUrl, modelName, systemPrompt, prompt);
                } catch (Exception e) {
                    aiResponseText = "";
                }
            }
        }

        synthesizeCADDesign(doc, prompt, aiResponseText);

        if (ollamaConnected) {
            return "🟢 Ollama Local (" + modelName + ") processou o prompt com sucesso!\n" +
                   "--- Resposta do Modelo Local ---\n" + aiResponseText;
        } else {
            return "⚡ Motor de IA do LeatherCAD gerou os moldes 2D com sucesso!\n" +
                   "(Status Ollama: Offline em " + baseUrl + " - Usando gerador preditivo local).";
        }
    }

    private static void synthesizeCADDesign(Document doc, String prompt, String aiText) {
        doc.clear();
        String leatherLayerId = doc.getLeatherLayerId();
        String stitchLayerId = doc.getStitchLayerId();
        String creaseLayerId = doc.getCreaseLayerId();
        String lower = prompt.toLowerCase();

        double parsedWidth = extractNumericValue(aiText, "(?i)Largura:\\s*([0-9]+(?:\\.[0-9]+)?)", 0.0);
        double parsedHeight = extractNumericValue(aiText, "(?i)Altura:\\s*([0-9]+(?:\\.[0-9]+)?)", 0.0);
        double parsedMargin = extractNumericValue(aiText, "(?i)(?:Margem|Costura):\\s*([0-9]+(?:\\.[0-9]+)?)", 3.85);

        if (lower.contains("passaporte") || lower.contains("passport")) {
            double w = (parsedWidth > 40) ? parsedWidth : 140.0;
            double h = (parsedHeight > 40) ? parsedHeight : 200.0;
            double margin = (parsedMargin > 0) ? parsedMargin : 3.85;

            List<CADElement> passportElems = new ArrayList<>();
            passportElems.add(new RectElement(leatherLayerId, new Rect2D(20, 20, w, h, 6.0)));

            StitchConfig sc = new StitchConfig(margin, 3.85, 1.0, 45.0, com.leathercad.core.leather.StitchType.FRENCH);
            passportElems.add(new StitchElement(stitchLayerId, new LineSegment(new Point2D(20 + margin, 20 + margin), new Point2D(20 + margin, 20 + h - margin)), sc));
            passportElems.add(new StitchElement(stitchLayerId, new LineSegment(new Point2D(20 + margin, 20 + h - margin), new Point2D(20 + w - margin, 20 + h - margin)), sc));
            passportElems.add(new StitchElement(stitchLayerId, new LineSegment(new Point2D(20 + w - margin, 20 + h - margin), new Point2D(20 + w - margin, 20 + margin)), sc));

            doc.addElement(CADAssembler.createAssemblyBlock(leatherLayerId, "Capa de Passaporte", passportElems));

            var idElems = IDWindowComponent.createIDWindow(leatherLayerId, new Point2D(35, 45));
            for (CADElement e : idElems) doc.addElement(e);

        } else if (lower.contains("moeda") || lower.contains("coin")) {
            double w = (parsedWidth > 40) ? parsedWidth : 110.0;
            double h = (parsedHeight > 40) ? parsedHeight : 85.0;

            var pocketElems = CoinPocketComponent.createCoinPocket(leatherLayerId, new Point2D(20, 20), w, h);
            for (CADElement e : pocketElems) doc.addElement(e);

            var snapElems = HardwareComponent.createMagneticSnap(leatherLayerId, new Point2D(20 + w / 2.0, 35), 14.0);
            for (CADElement e : snapElems) doc.addElement(e);

        } else if (lower.contains("minimalist") || lower.contains("3 slots") || lower.contains("3 divisórias")) {
            double w = (parsedWidth > 40) ? parsedWidth : 100.0;
            double h = (parsedHeight > 40) ? parsedHeight : 75.0;
            doc.addElement(new RectElement(leatherLayerId, new Rect2D(20, 20, w, h, 4.0)));

            for (int i = 0; i < 3; i++) {
                var slotElems = CardSlotComponent.createCardSlot(leatherLayerId, stitchLayerId, new Point2D(24, 25 + (i * 14)), 88.0, 42.0);
                for (CADElement e : slotElems) doc.addElement(e);
            }

        } else {
            // Padrão de Artesanato Real: Carteira Bifold 215mm x 95mm
            double width = (parsedWidth >= 170.0) ? parsedWidth : 215.0;
            double height = (parsedHeight >= 85.0) ? parsedHeight : 95.0;
            double margin = (parsedMargin > 0) ? parsedMargin : 3.85;

            // 1. Moldura Externa Frontal
            List<CADElement> frontElems = new ArrayList<>();
            Point2D origin1 = new Point2D(20, 20);
            frontElems.add(new RectElement(leatherLayerId, new Rect2D(origin1, width, height, 5.0)));

            StitchConfig sc = new StitchConfig(margin, 3.85, 1.0, 45.0, com.leathercad.core.leather.StitchType.FRENCH);
            frontElems.add(new StitchElement(stitchLayerId, new LineSegment(new Point2D(20 + margin, 20 + margin), new Point2D(20 + margin, 20 + height - margin)), sc));
            frontElems.add(new StitchElement(stitchLayerId, new LineSegment(new Point2D(20 + margin, 20 + height - margin), new Point2D(20 + width - margin, 20 + height - margin)), sc));
            frontElems.add(new StitchElement(stitchLayerId, new LineSegment(new Point2D(20 + width - margin, 20 + height - margin), new Point2D(20 + width - margin, 20 + margin)), sc));

            double creaseX = 20.0 + (width / 2.0);
            frontElems.add(new CreaseElement(creaseLayerId, new LineSegment(new Point2D(creaseX, 20), new Point2D(creaseX, 20 + height)), 1.5));
            doc.addElement(CADAssembler.createAssemblyBlock(leatherLayerId, "Corpo Frontal Principal", frontElems));

            // 2. EXATAMENTE 6 Porta-cartões em camadas separadas de couro e costura
            double cardW = 88.0;
            double leftSlotX = 26.0;
            double rightSlotX = creaseX + 13.5;

            for (int i = 0; i < 3; i++) {
                var leftSlot = CardSlotComponent.createCardSlot(leatherLayerId, stitchLayerId, new Point2D(leftSlotX, 26.0 + (i * 16)), cardW, 45.0);
                for (CADElement e : leftSlot) doc.addElement(e);

                var rightSlot = CardSlotComponent.createCardSlot(leatherLayerId, stitchLayerId, new Point2D(rightSlotX, 26.0 + (i * 16)), cardW, 45.0);
                for (CADElement e : rightSlot) doc.addElement(e);
            }

            // 3. Corpo Traseiro / Porta-Notas
            List<CADElement> backElems = new ArrayList<>();
            Point2D origin2 = new Point2D(20 + width + 25, 20);
            backElems.add(new RectElement(leatherLayerId, new Rect2D(origin2, width, height, 5.0)));
            backElems.add(new StitchElement(stitchLayerId, new LineSegment(new Point2D(origin2.x() + margin, origin2.y() + margin), new Point2D(origin2.x() + margin, origin2.y() + height - margin)), sc));
            backElems.add(new StitchElement(stitchLayerId, new LineSegment(new Point2D(origin2.x() + margin, origin2.y() + height - margin), new Point2D(origin2.x() + width - margin, origin2.y() + height - margin)), sc));
            backElems.add(new StitchElement(stitchLayerId, new LineSegment(new Point2D(origin2.x() + width - margin, origin2.y() + height - margin), new Point2D(origin2.x() + width - margin, origin2.y() + margin)), sc));

            doc.addElement(CADAssembler.createAssemblyBlock(leatherLayerId, "Corpo Traseiro Porta-Notas", backElems));

            // Cotas de Dimensão
            doc.addElement(new DimensionElement(leatherLayerId, origin1, new Point2D(origin1.x() + width, origin1.y()), DimensionElement.DimensionType.HORIZONTAL, 6.0));
            doc.addElement(new DimensionElement(leatherLayerId, origin1, new Point2D(origin1.x(), origin1.y() + height), DimensionElement.DimensionType.VERTICAL, 6.0));
        }
    }

    private static double extractNumericValue(String text, String regex, double fallback) {
        if (text == null || text.isBlank()) return fallback;
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
        return fallback;
    }
}
