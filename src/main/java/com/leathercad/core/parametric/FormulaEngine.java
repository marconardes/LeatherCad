package com.leathercad.core.parametric;

import java.util.Map;

public class FormulaEngine {

    /**
     * Avalia uma expressão matemática substituindo nomes de variáveis pelos seus valores.
     * Exemplo: "card_width + (2 * margin) + (2 * leather_thickness)"
     */
    public static double evaluate(String expression, Map<String, Double> variables) {
        if (expression == null || expression.trim().isEmpty()) {
            return 0.0;
        }

        String cleanedExpr = expression.trim();

        // Tenta fazer parse numérico direto
        try {
            return Double.parseDouble(cleanedExpr);
        } catch (NumberFormatException ignored) {}

        // Se for um único nome de variável registrado
        if (variables.containsKey(cleanedExpr)) {
            return variables.get(cleanedExpr);
        }

        // Substituição de variáveis no texto da expressão
        String exprToParse = cleanedExpr;
        for (Map.Entry<String, Double> entry : variables.entrySet()) {
            // Substituição de palavra inteira usando regex
            exprToParse = exprToParse.replaceAll("\\b" + entry.getKey() + "\\b", String.valueOf(entry.getValue()));
        }
        exprToParse = exprToParse.replaceAll("\\bPI\\b", String.valueOf(Math.PI));

        return evalMathString(exprToParse);
    }

    private static double evalMathString(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) throw new RuntimeException("Caractere inesperado: " + (char)ch);
                return x;
            }

            // Gramática: expression = term | expression '+' term | expression '-' term
            double parseExpression() {
                double x = parseTerm();
                for (;;) {
                    if      (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }

            // term = factor | term '*' factor | term '/' factor
            double parseTerm() {
                double x = parseFactor();
                for (;;) {
                    if      (eat('*')) x *= parseFactor();
                    else if (eat('/')) x /= parseFactor();
                    else return x;
                }
            }

            // factor = '+' factor | '-' factor | '(' expression ')' | number | function
            double parseFactor() {
                if (eat('+')) return +parseFactor();
                if (eat('-')) return -parseFactor();

                double x;
                int startPos = this.pos;
                if (eat('(')) {
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if (ch >= 'a' && ch <= 'z') {
                    while (ch >= 'a' && ch <= 'z') nextChar();
                    String func = str.substring(startPos, this.pos);
                    if (eat('(')) {
                        x = parseExpression();
                        eat(')');
                    } else {
                        x = 0;
                    }
                    if (func.equals("sqrt")) x = Math.sqrt(x);
                    else if (func.equals("abs")) x = Math.abs(x);
                } else {
                    throw new RuntimeException("Símbolo inválido: " + (char)ch);
                }

                return x;
            }
        }.parse();
    }
}
