package ua.lpnu.practBack.service;

import org.matheclipse.core.eval.ExprEvaluator;
import org.matheclipse.core.interfaces.IExpr;
import org.springframework.stereotype.Service;
import ua.lpnu.practBack.entity.Formula;
import ua.lpnu.practBack.entity.Formula.FormulaScript;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MathService {

    public Map<String, Double> runAutoSolver(Formula formula, Map<String, Double> inputs) {
        Map<String, Double> context = new HashMap<>(inputs);

        if (formula.getScripts() == null || formula.getScripts().isEmpty()) {
            return context;
        }

        boolean progress;
        int maxPasses = 10; // Захист від нескінченного циклу

        do {
            progress = false;
            for (FormulaScript script : formula.getScripts()) {
                String rawEquation = script.getExpression();
                String target = script.getTarget();

                // Якщо немає "=", створюємо рівняння (наприклад, "R == U/I")
                if (!rawEquation.contains("=")) {
                    rawEquation = target + " == " + rawEquation;
                } else {
                    rawEquation = rawEquation.replace("=", "==");
                }

                List<String> rawVars = extractVariables(rawEquation);

                String missingVar = null;
                int missingCount = 0;

                for (String v : rawVars) {
                    if (!context.containsKey(v)) {
                        missingVar = v;
                        missingCount++;
                    }
                }

                // Якщо не вистачає лише однієї змінної — можемо її знайти!
                if (missingCount == 1) {
                    try {
                        Double res = solve(rawEquation, context, missingVar);
                        if (res != null) {
                            context.put(missingVar, res);
                            progress = true;
                        }
                    } catch (Exception e) {
                        System.err.println("Помилка автосолвера для змінної " + missingVar + ": " + e.getMessage());
                    }
                }
            }
            maxPasses--;
        } while (progress && maxPasses > 0);

        return context;
    }

    private Double solve(String equation, Map<String, Double> context, String targetVar) {
        // Створюємо новий екземпляр для потокобезпечності
        ExprEvaluator evaluator = new ExprEvaluator();

        try {
            String commandEq = equation;

            // Підставляємо відомі значення у рівняння
            for (Map.Entry<String, Double> entry : context.entrySet()) {
                String varName = entry.getKey();
                String val = String.valueOf(entry.getValue()).replace("E", "*^");
                // Використовуємо регулярний вираз для точної заміни слова
                commandEq = commandEq.replaceAll("\\b" + varName + "\\b", val);
            }

            // Формуємо команду для Symja
            String command = "N(Solve(Rationalize(" + commandEq + "), " + targetVar + "), 50)";

            IExpr result = evaluator.eval(command);
            return extractPositiveRoot(result.toString());

        } catch (Exception e) {
            System.err.println("Помилка Symja: " + e.getMessage());
            return null;
        }
    }

    private List<String> extractVariables(String equation) {
        List<String> vars = new ArrayList<>();
        // Шукаємо слова, що складаються з літер та цифр (наприклад, U, I, R1)
        Pattern pattern = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");
        Matcher matcher = pattern.matcher(equation);

        // Ігноруємо вбудовані функції Symja
        List<String> ignoreList = List.of("Sin", "Cos", "Tan", "Log", "Sqrt", "E", "Pi");

        while (matcher.find()) {
            String var = matcher.group();
            if (!ignoreList.contains(var)) {
                vars.add(var);
            }
        }
        return vars;
    }

    private Double extractPositiveRoot(String symjaResponse) {
        if (symjaResponse == null || symjaResponse.equals("{}") || symjaResponse.equals("List()")) {
            return null;
        }

        String cleanResponse = symjaResponse.replace("*^", "E");
        Pattern pattern = Pattern.compile("->\\s*(-?\\d+(\\.\\d*)?([eE][+-]?\\d+)?)");
        Matcher matcher = pattern.matcher(cleanResponse);

        Double bestResult = null;

        while (matcher.find()) {
            try {
                double val = Double.parseDouble(matcher.group(1));
                if (val >= 0) return val; // Пріоритет додатним кореням
                if (bestResult == null) bestResult = val;
            } catch (Exception ignored) { }
        }
        return bestResult;
    }
}