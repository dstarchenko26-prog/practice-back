package ua.lpnu.practBack.service;

import org.matheclipse.core.eval.ExprEvaluator;
import org.matheclipse.core.interfaces.IExpr;
import org.springframework.stereotype.Service;
import ua.lpnu.practBack.entity.Formula;
import ua.lpnu.practBack.entity.Formula.FormulaScript;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MathService {

    private static final int MAX_PASSES = 20;

    private static final String INTERNAL_CURRENT = "CURRENT";

    private static final Pattern VARIABLE_PATTERN =
            Pattern.compile("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b");

    private static final Set<String> SYMJA_NAMES = Set.of(
            "Sin", "Cos", "Tan", "Cot", "Sec", "Csc",
            "Sinh", "Cosh", "Tanh", "Exp", "Log", "Sqrt",
            "Abs", "Sign", "Floor", "Ceiling", "Round",
            "ArcSin", "ArcCos", "ArcTan", "ArcCot",
            "Min", "Max", "N", "Solve", "Rationalize",
            "E", "Pi", "True", "False"
    );

    public Map<String, Double> runAutoSolver(
            Formula formula,
            Map<String, Double> inputs
    ) {
        Map<String, Double> context = new HashMap<>();

        if (inputs != null) {
            context.putAll(inputs);
        }

        if (formula == null) {
            return context;
        }

        if (formula.getScripts() == null ||
                formula.getScripts().isEmpty()) {
            return context;
        }

        boolean progress;
        int pass = 0;

        do {
            progress = false;
            pass++;

            for (FormulaScript script : formula.getScripts()) {

                if (script == null) {
                    continue;
                }

                String expression = script.getExpression();
                String target = script.getTarget();

                if (expression == null ||
                        expression.isBlank()) {
                    continue;
                }

                try {
                    String equation =
                            buildEquation(expression, target);

                    Set<String> variables =
                            extractVariables(equation);

                    Set<String> missingVariables =
                            new LinkedHashSet<>();

                    for (String variable : variables) {
                        if (!context.containsKey(variable)) {
                            missingVariables.add(variable);
                        }
                    }

                    if (missingVariables.size() != 1) {
                        continue;
                    }

                    String missingVariable =
                            missingVariables.iterator().next();

                    Double result = solve(
                            equation,
                            context,
                            missingVariable
                    );

                    if (result == null ||
                            !Double.isFinite(result)) {
                        continue;
                    }

                    if (missingVariable.matches("^[RCLf].*") || missingVariable.equalsIgnoreCase("Tau")) {
                        result = Math.abs(result);
                    }

                    Double oldValue =
                            context.get(missingVariable);

                    if (oldValue == null ||
                            !approximatelyEqual(
                                    oldValue,
                                    result
                            )) {

                        context.put(
                                missingVariable,
                                result
                        );

                        progress = true;

                        System.out.println(
                                "AutoSolver: " +
                                        missingVariable +
                                        " = " +
                                        result +
                                        " from [" +
                                        equation +
                                        "]"
                        );
                    }

                } catch (Exception e) {

                    System.err.println(
                            "Помилка автосолвера: " +
                                    e.getMessage()
                    );
                }
            }

        } while (progress && pass < MAX_PASSES);

        return context;
    }

    private String buildEquation(
            String expression,
            String target
    ) {
        String equation = expression.trim();

        if (containsEqualityOperator(equation)) {
            return normalizeEqualityOperator(equation);
        }

        if (target == null ||
                target.isBlank()) {

            throw new IllegalArgumentException(
                    "FormulaScript target не може бути порожнім: " +
                            expression
            );
        }

        return target.trim() +
                " == " +
                equation;
    }

    private boolean containsEqualityOperator(
            String expression
    ) {
        return expression.contains("==") ||
                expression.matches(
                        ".*(?<![<>!])=(?!=).*"
                );
    }

    private String normalizeEqualityOperator(
            String equation
    ) {
        return equation.replaceAll(
                "(?<![<>=!])=(?!=)",
                "=="
        );
    }

    private Double solve(
            String equation,
            Map<String, Double> context,
            String targetVar
    ) {
        if (equation == null ||
                equation.isBlank() ||
                targetVar == null ||
                targetVar.isBlank()) {
            return null;
        }

        ExprEvaluator evaluator =
                new ExprEvaluator();

        try {
            String symjaEquation =
                    replaceUserVariablesForSymja(
                            equation
                    );

            String symjaTarget =
                    toSymjaVariable(targetVar);

            symjaEquation =
                    substituteKnownValues(
                            symjaEquation,
                            context,
                            targetVar
                    );

            System.out.println(
                    "Symja equation: " +
                            symjaEquation
            );

            String command =
                    "Solve(" +
                            "Rationalize(" +
                            symjaEquation +
                            ", 0), " +
                            symjaTarget +
                            ")";

            IExpr result =
                    evaluator.eval(command);

            if (result == null) {
                return null;
            }

            String response =
                    result.toString();

            System.out.println(
                    "Symja: " +
                            command +
                            " => " +
                            response
            );

            return extractBestRoot(
                    response,
                    symjaTarget
            );

        } catch (Exception e) {

            System.err.println(
                    "Помилка Symja для [" +
                            equation +
                            "], target [" +
                            targetVar +
                            "]: " +
                            e.getMessage()
            );

            return null;
        }
    }

    private String replaceUserVariablesForSymja(
            String equation
    ) {
        return replaceVariable(
                equation,
                "I",
                INTERNAL_CURRENT
        );
    }

    private String toSymjaVariable(
            String variable
    ) {
        if ("I".equals(variable)) {
            return INTERNAL_CURRENT;
        }

        return variable;
    }

    private String substituteKnownValues(
            String equation,
            Map<String, Double> context,
            String targetVar
    ) {
        String result = equation;

        List<String> variables =
                new ArrayList<>(
                        context.keySet()
                );

        variables.sort(
                Comparator.comparingInt(
                        String::length
                ).reversed()
        );

        for (String variable : variables) {

            /*
             * Шукану змінну не підставляємо.
             */
            if (variable.equals(targetVar)) {
                continue;
            }

            Double value =
                    context.get(variable);

            if (value == null ||
                    !Double.isFinite(value)) {
                continue;
            }

            String symjaVariable =
                    toSymjaVariable(variable);

            String number =
                    toSymjaNumber(value);

            result = replaceVariable(
                    result,
                    symjaVariable,
                    number
            );
        }

        return result;
    }

    private String replaceVariable(
            String text,
            String variable,
            String replacement
    ) {
        String regex =
                "(?<![a-zA-Z0-9_])" +
                        Pattern.quote(variable) +
                        "(?![a-zA-Z0-9_])";

        return text.replaceAll(
                regex,
                Matcher.quoteReplacement(
                        replacement
                )
        );
    }

    private String toSymjaNumber(
            Double value
    ) {
        if (value == null) {
            return "0";
        }

        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(
                    "Некоректне число: " + value
            );
        }

        return Double.toString(value)
                .replace("E", "*^")
                .replace("e", "*^");
    }

    private Set<String> extractVariables(
            String equation
    ) {
        Set<String> variables =
                new LinkedHashSet<>();

        if (equation == null ||
                equation.isBlank()) {
            return variables;
        }

        Matcher matcher =
                VARIABLE_PATTERN.matcher(
                        equation
                );

        while (matcher.find()) {

            String variable =
                    matcher.group();

            if (SYMJA_NAMES.contains(variable)) {
                continue;
            }

            variables.add(variable);
        }

        return variables;
    }

    private Double extractBestRoot(
            String symjaResponse,
            String symjaTarget
    ) {
        if (symjaResponse == null ||
                symjaResponse.isBlank()) {
            return null;
        }

        String response = symjaResponse.trim();

        if (response.equals("{}") ||
                response.equals("List()") ||
                response.equals("{{}}")) {
            return null;
        }

        Pattern pattern = Pattern.compile(
                "->\\s*([^}\\s]+)"
        );

        Matcher matcher = pattern.matcher(response);

        List<String> roots = new ArrayList<>();

        while (matcher.find()) {
            roots.add(matcher.group(1));
        }

        if (roots.isEmpty()) {
            return null;
        }

        ExprEvaluator evaluator = new ExprEvaluator();

        List<Double> numericRoots = new ArrayList<>();

        for (String root : roots) {
            try {

                IExpr numericResult = evaluator.eval("Re(N(" + root + "))");

                if (numericResult == null) {
                    continue;
                }

                String numStr = numericResult.toString()
                        .replace("*10^", "E")
                        .replace("*^", "E")
                        .replaceAll("[()]", ""); // Очищення від дужок

                double value = Double.parseDouble(numStr);

                if (Double.isFinite(value)) {
                    numericRoots.add(value);
                }

            } catch (Exception e) {
                System.err.println(
                        "Не вдалося перетворити корінь [" +
                                root +
                                "] у число: " +
                                e.getMessage()
                );
            }
        }

        if (numericRoots.isEmpty()) {
            return null;
        }


        for (Double root : numericRoots) {
            if (root >= 0.0) {
                return root;
            }
        }


        return numericRoots.get(0);
    }

    private boolean approximatelyEqual(
            double a,
            double b
    ) {
        double difference =
                Math.abs(a - b);

        if (difference < 1e-10) {
            return true;
        }

        double scale =
                Math.max(
                        Math.abs(a),
                        Math.abs(b)
                );

        return difference <=
                Math.max(
                        1e-10,
                        scale * 1e-10
                );
    }
}
