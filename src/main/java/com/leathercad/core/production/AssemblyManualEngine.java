package com.leathercad.core.production;

import com.leathercad.core.leather.CreaseElement;
import com.leathercad.core.leather.SkivingElement;
import com.leathercad.core.leather.StitchElement;
import com.leathercad.core.model.CADAssembler;
import com.leathercad.core.model.CADElement;
import com.leathercad.core.model.Document;
import com.leathercad.core.model.RectElement;

import java.util.*;

public class AssemblyManualEngine {

    public static AssemblyManual generateManual(Document doc) {
        List<CADElement> flatElements = CADAssembler.flatten(doc.getElements());

        int piecesCount = 0;
        int holesCount = 0;
        double stitchLenMm = 0.0;
        boolean hasSkiving = false;
        boolean hasCreases = false;
        boolean hasStitches = false;

        List<String> pieceNames = new ArrayList<>();

        for (CADElement elem : flatElements) {
            if (elem instanceof RectElement rect) {
                piecesCount++;
                pieceNames.add("Painel de Couro (" + String.format("%.0fx%.0f", rect.rect().width(), rect.rect().height()) + "mm)");
            } else if (elem instanceof StitchElement stitch) {
                hasStitches = true;
                holesCount += stitch.holePoints().size();
                stitchLenMm += stitch.baseLine().length();
            } else if (elem instanceof SkivingElement) {
                hasSkiving = true;
            } else if (elem instanceof CreaseElement) {
                hasCreases = true;
            }
        }

        if (piecesCount == 0) piecesCount = Math.max(1, flatElements.size());

        List<AssemblyStep> steps = new ArrayList<>();
        Set<String> tools = new LinkedHashSet<>();
        Set<String> supplies = new LinkedHashSet<>();

        // Etapa 1: Preparação e Corte de Peças
        tools.add("Estilete / Faca de Corte Olfa / Cutter");
        tools.add("Régua de Aço Inox em mm");
        tools.add("Base de Corte Veda-Corte A3/A2");
        supplies.add("Couro Selecionado (Veg Tan / Cromo / Pull-up)");

        steps.add(new AssemblyStep(
            1,
            "Corte & Preparação",
            "Corte dos Moldes em Couro",
            "Posicione os gabaritos impressos em escala 1:1 sobre o couro e realize o corte manual de todas as " + piecesCount + " peças usando a faca de corte e régua de aço.",
            List.of("Estilete de precisão", "Régua de Aço", "Base de Corte"),
            List.of("Placa de Couro"),
            pieceNames.isEmpty() ? List.of("Painéis Principais") : pieceNames
        ));

        // Etapa 2: Skiving / Rebaixamento de Bordas (se houver ou para montagem de abas)
        tools.add("Faca de Rebaixar Skiving / French Edger / Biselador");
        supplies.add("Cera de Abelha para Proteção da Lâmina");
        steps.add(new AssemblyStep(
            2,
            "Rebaixamento (Skiving)",
            "Desbaste de Espessura nas Bordas de Encaixe",
            hasSkiving
                ? "Realize o rebaixamento técnico nas áreas marcadas para reduzir a espessura nas bordas dobradas e evitar relevo nos porta-cartões."
                : "Realize o biselamento e chanfro suave nas bordas das abas de porta-cartões para garantir encaixe plano e sem volume excessivo.",
            List.of("Faca de Rebaixar / Skiver", "Biselador de Borda"),
            List.of("Cera protetora de lâmina"),
            List.of("Bordas de Encaixe", "Porta-Cartões")
        ));

        // Etapa 3: Vincos e Linhas Decorativas
        tools.add("Boleador / Ferramenta de Vinco Quente (Creaser 1.5mm)");
        tools.add("Lâmpada de Álcool / Aquecedor Elétrico");
        steps.add(new AssemblyStep(
            3,
            "Vincos & Marcação",
            "Execução de Vincos de Dobra e Acabamento",
            hasCreases
                ? "Aqueça o creaser a 120°C e passe firmemente sobre as linhas de vinco parametrizadas para guiar a dobra perfeita do couro."
                : "Passe o boleador/creaser aquecido a 1.5mm da borda para criar a linha decorativa de acabamento e compactar as fibras do couro.",
            List.of("Creaser 1.5mm", "Aquecedor de Ferramentas"),
            List.of("Álcool de aquecimento"),
            List.of("Linha de Vinco Central", "Bordas Decorativas")
        ));

        // Etapa 4: Colagem e Montagem de Abas
        tools.add("Pincel Aplicador de Cola");
        tools.add("Martelo de Nylon / Rolo de Pressão para Couro");
        supplies.add("Cola de Contato de Borracha (Sem Tolueno)");
        steps.add(new AssemblyStep(
            4,
            "Colagem & Alinhamento",
            "Montagem Temporária dos Porta-Cartões e Abas",
            "Aplique uma camada fina e uniforme de cola de contato nas margens de colagem. Aguarde 5 minutos até secar o tato e pressione firmemente os porta-cartões usando o martelo de nylon.",
            List.of("Pincel de Cola", "Martelo de Nylon", "Rolo de Pressão"),
            List.of("Cola de Contato de Borracha"),
            List.of("Abas de Cartão", "Moldura Principal")
        ));

        // Etapa 5: Furação de Garfo/Chisel
        tools.add("Garfo / Pricking Iron (Passo 3.85mm)");
        tools.add("Martelo / Macete de Polímero Reto (300g)");
        tools.add("Tábua de Impacto PU para Furação");
        steps.add(new AssemblyStep(
            5,
            "Furação de Costura",
            "Marcação e Puncionamento dos Furos de Garfo",
            "Alinhe o garfo de costura de 3.85mm a 3.85mm da borda colada. Realize a furação perimetral perpendicular dos " + (holesCount > 0 ? holesCount : "furos de garfo") + " mantendo o ângulo constante de 45°.",
            List.of("Garfo Francês 3.85mm", "Macete de Polímero", "Placa de Impacto PU"),
            List.of("Guia de Alinhamento"),
            List.of("Linha Perimetral de Costura")
        ));

        // Etapa 6: Costura Manual em Ponto Sela (Saddle Stitch)
        tools.add("Agulhas de Ponta Cega para Couro (Nº 2/0)");
        tools.add("Cavalete de Costura de Madeira (Stitching Pony)");
        supplies.add("Fio de Linha Poliéster Encerada 0.6mm (Cor Contrastante/Harmônica)");
        double threadNeededMeters = Math.max(1.5, (stitchLenMm / 1000.0) * 4.5);

        steps.add(new AssemblyStep(
            6,
            "Costura Manual (Saddle Stitch)",
            "Costura em Ponto Sela com Duas Agulhas",
            String.format("Corte %.1fm de fio encerado 0.6mm (4.5x o comprimento do trajeto). Passe duas agulhas nas extremidades e realize a costura ponto sela travada manual, finalizando com retrocesso de 2 furos e arremate cortado selado.", threadNeededMeters),
            List.of("2 Agulhas Cegas Nº 2/0", "Cavalete de Madeira / Stitching Pony", "Tesoura / Isqueiro de Arremate"),
            List.of("Fio Encerado Poliéster 0.6mm (" + String.format("%.1fm", threadNeededMeters) + ")"),
            List.of("Contorno Perimetral Costurado")
        ));

        // Etapa 7: Instalação de Ferragens (se aplicável)
        tools.add("Matriz de Fixação de Botões / Vazador Cilíndrico");
        supplies.add("Botão de Pressão / Fecho Magnético 14mm em Latão");
        steps.add(new AssemblyStep(
            7,
            "Ferragens & Fechos",
            "Instalação de Botões de Pressão e Componentes Metálicos",
            "Perfure o ponto de marcação com o vazador cilíndrico e posicione o fecho metálico. Remande o macho e fêmea com o balancim ou matriz manual até travar perfeitamente.",
            List.of("Vazador Cilíndrico", "Matriz de Fixação / Balancim"),
            List.of("Ferragem Metálica (Latão/Prata)"),
            List.of("Pontos de Fecho")
        ));

        // Etapa 8: Acabamento de Bordas (Brunimento e Tinta)
        tools.add("Lixa d'Água Grão 400/600/1000");
        tools.add("Brunidor de Madeira (Slicker de Jacarandá)");
        tools.add("Rolinho Aplicador de Tinta de Borda");
        supplies.add("Goma Tragacanto / Goma Tokonole Japonesa");
        supplies.add("Tinta de Borda para Couro (Fosca/Brilhante)");

        steps.add(new AssemblyStep(
            8,
            "Acabamento de Bordas",
            "Lixamento, Brunimento Tokonole e Pintura de Borda",
            "Lixe a borda colada com lixa 400 até nivelar. Aplique goma Tokonole e frictionar firmemente com o brunidor de madeira até obter o brilho espelhado *burnished*. Opcionalmente aplique 2 demãos de tinta de borda.",
            List.of("Lixas Grão 400/800", "Brunidor de Madeira", "Rolinho de Tinta"),
            List.of("Goma Tokonole Japonesa", "Tinta de Borda", "Cera de Carnaúba"),
            List.of("Bordas Externas Expostas")
        ));

        return new AssemblyManual(
            "Manual Técnico de Montagem — " + (doc.getElements().isEmpty() ? "Carteira de Couro" : "Projeto LeatherCAD"),
            piecesCount,
            holesCount,
            stitchLenMm / 1000.0,
            steps,
            new ArrayList<>(tools),
            new ArrayList<>(supplies)
        );
    }
}
